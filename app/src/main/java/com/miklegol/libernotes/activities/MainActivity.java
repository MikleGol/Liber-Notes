package com.miklegol.libernotes.activities;

import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.miklegol.libernotes.R;
import com.miklegol.libernotes.adapters.NotesAdapter;
import com.miklegol.libernotes.database.NotesDatabase;
import com.miklegol.libernotes.entities.Note;
import com.miklegol.libernotes.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;

    private AlertDialog dialogAddUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window w = getWindow();
        ActionBar actionBar = getSupportActionBar();
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        //actionBar.hide();

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);

        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE);
            }
        });

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(noteList.size() != 0){
                    notesAdapter.searchNotes(editable.toString());
                }
            }
        });

        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE);
            }
        });

        findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
                } else{
                    selectImage();
                }
            }
        });

        findViewById(R.id.imageAddWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddUrlDialog();
            }
        });
    }

    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted){

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>>{

            @Override
            protected List<Note> doInBackground(Void... voids){
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes){
                super.onPostExecute(notes);
                if(requestCode == REQUEST_CODE_SHOW_NOTES){
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else if(requestCode == REQUEST_CODE_ADD_NOTE){
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                } else if(requestCode == REQUEST_CODE_UPDATE_NOTE){
                    noteList.remove(noteClickedPosition);

                    if(isNoteDeleted){
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                        Log.d("123123","Deleted - true");
                    } else{
                        Log.d("123123","Deleted - false");
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            } else{
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPath(Uri contentUri){
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if(cursor == null){
            filePath = contentUri.getPath();
        } else{
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK){
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
            if(data != null){
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        } else if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data != null){
                Uri selectImageUri = data.getData();
                if(selectImageUri != null){
                    try {
                        String selectImagePath = getPath(selectImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    } catch (Exception ex){
                        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void showAddUrlDialog(){
        if(dialogAddUrl == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url, (ViewGroup) findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);

            dialogAddUrl = builder.create();
            if(dialogAddUrl.getWindow() != null){
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputUrl = view.findViewById(R.id.inputUrl);
            inputUrl.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(inputUrl.getText().toString().trim().isEmpty()){
                        Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if(!Patterns.WEB_URL.matcher(inputUrl.getText().toString()).matches()){
                        Toast.makeText(MainActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                    } else{
                        dialogAddUrl.dismiss();
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "URL");
                        intent.putExtra("URL", inputUrl.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddUrl.dismiss();
                }
            });
        }
        dialogAddUrl.show();
    }
}