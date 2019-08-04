package com.memtrig.admin.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.memtrig.admin.R;
import com.memtrig.admin.serializer.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ShowTodayFragment extends Fragment {

    private View mBaseView;
    private DatabaseReference mDatabase;
    private FirebaseAuth auth;
    private ListView listView;
    private Adapter adapter;
    private ArrayList<Task> arrayList;
    private String todaysDate;
    private SearchView searchView;
    private String searchFor = "";
    private Timer timer=new Timer();
    private final long DELAY = 200; // milliseconds
    private ArrayList<Task> searchTaks;

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Today's Tasks");
        mBaseView = inflater.inflate(R.layout.today_tasks, container, false);
        listView = mBaseView.findViewById(R.id.list);
        arrayList = new ArrayList<>();
        setHasOptionsMenu(true);
        return mBaseView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchViewMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchViewMenuItem.getActionView();
        ImageView v = (ImageView) searchView.findViewById(androidx.appcompat.R.id.search_button);
        v.setImageResource(android.R.drawable.ic_menu_search); //Changing the image

        if (!searchFor.isEmpty()) {
            searchView.setIconified(false);
            searchView.setQuery(searchFor, false);
        }
        searchView.setQueryHint("Search Task");
        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.i("TAG", "focus " + hasFocus + searchView.getQuery());
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Do your search
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                if (newText.isEmpty()) {
                    Log.i("TAG", "empty");
                    timer.cancel();
                    searchTaks = new ArrayList<>();
                    adapter = new Adapter(getContext(), arrayList);
                    listView.setAdapter(adapter);
                    return false;
                } else {
                    Log.i("TAG", "!empty");
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            search(newText.toLowerCase());
                                        }
                                    });
                                }
                            },
                            DELAY
                    );
                    return true;
                }
            }
        });
    }

    private void search(String query) {
        searchTaks = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            Task task = arrayList.get(i);
            String title = task.getTitle().toLowerCase();
            if (title.contains(query) || title.contentEquals(query)
                    || title.equals(query) || title.equalsIgnoreCase(query)) {
                searchTaks.add(task);
            }
        }
        if (searchTaks.size() > 0) {
            adapter = new Adapter(getContext(), searchTaks);
            listView.setAdapter(adapter);
        } else {
            adapter = new Adapter(getContext(), arrayList);
            listView.setAdapter(adapter);
            Toast.makeText(getActivity(), "Nothing Matched", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mDatabase.removeEventListener(childEventListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        arrayList = new ArrayList<>();
        adapter = new Adapter(getContext(), arrayList);
        listView.setAdapter(adapter);
        Calendar calendar = Calendar.getInstance();
        String myFormat = "dd/MM/yy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        todaysDate = sdf.format(calendar.getTime());
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(auth.getCurrentUser().getUid()).child("tasks")
                .orderByChild("time").addChildEventListener(childEventListener);
    }

    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Log.i("TAG", " data " + dataSnapshot);
            Task task = dataSnapshot.getValue(Task.class);
            task.setKey(dataSnapshot.getKey());
            if (task.getDate().equals(todaysDate)) {
                arrayList.add(task);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
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

    private class Adapter extends ArrayAdapter<Task> {

        private ArrayList<Task> arrayList;
        private ViewHolder viewHolder;

        public Adapter(Context context, ArrayList<Task> tasks) {
            super(context, R.layout.item_raw);
            arrayList = tasks;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.item_raw, parent, false);
                viewHolder.imageView = convertView.findViewById(R.id.repeat);
                viewHolder.title = convertView.findViewById(R.id.title);
                viewHolder.dateTime = convertView.findViewById(R.id.date_time);
                viewHolder.checkMark = convertView.findViewById(R.id.check_mark);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Task task = getItem(position);
            if (task.isMonday() || task.isTuesday() || task.isWednesday() || task.isThrusday() ||
            task.isFriday() || task.isSaturday() || task.isSunday()) {
                viewHolder.imageView.setImageResource(R.drawable.ic_repeat);
            } else {
                viewHolder.imageView.setImageResource(0);
            }
            if (task.isTaskDone()) {
                viewHolder.checkMark.setImageResource(R.drawable.ic_checkmarked);
            } else {
                viewHolder.checkMark.setImageResource(R.drawable.ic_checkmark_normal);
            }
            viewHolder.title.setText(task.getTitle());
            viewHolder.dateTime.setText(task.getTime());
            return convertView;
        }

        @Override
        public Task getItem(int position) {
            return arrayList.get(position);
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }
    }

    private class ViewHolder {
        ImageView imageView;
        TextView title;
        AppCompatTextView dateTime;
        ImageView checkMark;

    }
}
