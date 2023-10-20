package com.miklegol.libernotes.listeners;

import com.miklegol.libernotes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
