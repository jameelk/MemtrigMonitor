package com.memtrig.admin.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.memtrig.admin.R;
import com.memtrig.admin.serializer.Note;

import java.util.ArrayList;

public class NoteFragment extends Fragment {

    private View mBaseView;
    private DatabaseReference mDatabase;
    private FirebaseAuth auth;
    private ListView listView;
    private ArrayList<Note> arrayList;
    public static boolean showingNote = false;
    private Adapter adapter;
    private Receiver receiver;
    private AlertDialog alertDialog = null;
    private Note mNote;
    private int item = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Notes");
        setHasOptionsMenu(true);
        mBaseView = inflater.inflate(R.layout.notes, container, false);
        listView = mBaseView.findViewById(R.id.list);
        return mBaseView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mNote = arrayList.get(i);
                showDialog(mNote.getTitle(), mNote.getDescription(), true);

            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mNote = arrayList.get(i);
                item = i;
                new AlertDialog.Builder(getActivity())
                        .setTitle("Delete Note")
                        .setMessage(String.format("Are you sure you want to delete %s?", mNote.getTitle()))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteTask();
                            }
                        })

                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                item = -1;
                            }
                        })
                        .setIcon(android.R.drawable.ic_menu_delete)
                        .show();
                return true;
            }
        });
    }

    private void deleteTask() {
        mDatabase.child("users").child(auth.getCurrentUser().getUid()).child("notes")
                .child(mNote.getKey()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(getView(), "Deleted!!",
                                Snackbar.LENGTH_SHORT).show();
                        arrayList.remove(item);
                        item = -1;
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        showingNote = true;
        arrayList = new ArrayList<>();
        adapter = new Adapter(getContext(), arrayList);
        listView.setAdapter(adapter);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(auth.getCurrentUser().getUid()).child("notes")
                .addChildEventListener(childEventListener);
        receiver = new Receiver();
        IntentFilter intentFilter = new IntentFilter("show_dialog");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        showingNote = false;
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    public void showDialog(String titleText, String des, final boolean update) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        View view = getLayoutInflater().inflate(R.layout.note_dialog, null);
        dialogBuilder.setView(view);
        final EditText title = (EditText) view.findViewById(R.id.title);
        final EditText description = view.findViewById(R.id.description);
        Button cancel = view.findViewById(R.id.cancel_action);
        Button save = view.findViewById(R.id.save);
        if (!titleText.equals("") && update) {
            title.setText(titleText);
        }
        if (!des.equals("") && update) {
            description.setText(des);
        }
        if (update) {
            save.setText("Update");
        } else {
            save.setText("Save");
        }
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String titleText = title.getText().toString();
                String descriptionText = description.getText().toString();
                if (titleText == null || titleText.trim().isEmpty()) {
                    Toast.makeText(getActivity(), "please enter note title",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (descriptionText == null || descriptionText.trim().isEmpty()) {
                    Toast.makeText(getActivity(), "please enter note description",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (titleText != null && !titleText.trim().isEmpty() && descriptionText != null &&
                        !descriptionText.trim().isEmpty()) {
                    Note note = new Note();
                    note.setTitle(titleText);
                    note.setDescription(descriptionText);
                    if (!update) {
                        String key = mDatabase.push().getKey();
                        mDatabase.child("users").child(auth.getCurrentUser().getUid()).child("notes")
                                .child(key).setValue(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(getView(), "Success", Snackbar.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            }
                        });
                    } else {
                        mDatabase.child("users").child(auth.getCurrentUser().getUid()).child("notes")
                                .child(mNote.getKey()).setValue(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(getView(), "Success", Snackbar.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            }
                        });
                    }
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Log.i("TAG", " data " + dataSnapshot);
            Note task = dataSnapshot.getValue(Note.class);
            task.setKey(dataSnapshot.getKey());
            arrayList.add(task);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private class Adapter extends ArrayAdapter<Note> {

        private ArrayList<Note> arrayList;
        private ViewHolder viewHolder;

        public Adapter(Context context, ArrayList<Note> notes) {
            super(context, R.layout.item_raw);
            arrayList = notes;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.note_raw, parent, false);
                viewHolder.title = convertView.findViewById(R.id.title);
                viewHolder.description = convertView.findViewById(R.id.description);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Note note = getItem(position);
            viewHolder.title.setText(note.getTitle());
            viewHolder.description.setText(note.getDescription());
            return convertView;
        }

        @Override
        public Note getItem(int position) {
            return arrayList.get(position);
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }
    }

    private class ViewHolder {
        TextView title;
        AppCompatTextView description;

    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            showDialog("", "", false);
        }
    }
}
