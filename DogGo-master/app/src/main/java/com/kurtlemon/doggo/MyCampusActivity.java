package com.kurtlemon.doggo;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MyCampusActivity extends AppCompatActivity {

    // Shared preferences information
    private final String SHARED_PREFERENCES_FILENAME = "storedData";
    private final String KEY_TEXT = "myCampus";

    // ID variables
    private String campusID;

    // XML display fields
    private RadioGroup campusRadioGroup;
    private EditText searchEditText;

    // Campus information
    private ArrayList<CampusInfo> campusInfoArrayList;

    // Firebase database fields
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_campus);

        campusRadioGroup = findViewById(R.id.campusRadioGroup);

        campusInfoArrayList = new ArrayList<>();

        // Setup and initialization of important features
        setUpDatabase();
        setUpSearch();
        setUpConfirmButton();
        setUpSharedPreferences();

    }

    /**
     * Sets up the database info for the campuses.
     */
    private void setUpDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance();

        /* CODE TO ADD NEW CAMPUSES. REMOVE THIS BEFORE PUBLISHING.
        CampusInfo gonzaga = new CampusInfo("Gonzaga University", "Washington", "Spokane", 1, 47.6672, 117.4024);
        databaseReference = firebaseDatabase.getReference().child("campusInfo").child(gonzaga.getID());
        databaseReference.setValue(gonzaga);
        CampusInfo uw = new CampusInfo("University of Washington", "Washington", "Seattle", 1, 47.6553, 122.3035);
        databaseReference = firebaseDatabase.getReference().child("campusInfo").child(uw.getID());
        databaseReference.setValue(uw);
        */

        databaseReference = firebaseDatabase.getReference().child("campusInfo");

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                CampusInfo campusInfo = dataSnapshot.getValue(CampusInfo.class);
                campusInfoArrayList.add(campusInfo);
                setUpRadioButtons();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.addChildEventListener(childEventListener);
    }

    /**
     * Sets up the campus search functionality.
     */
    private void setUpSearch(){
        searchEditText = findViewById(R.id.campusSearchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                campusRadioGroup.removeAllViews();
                for(CampusInfo campusInfo : campusInfoArrayList){
                    if(campusInfo.search(editable.toString().toLowerCase())){
                        RadioButton campusRadioButton =
                                new RadioButton(MyCampusActivity.this);
                        String campusTitle = campusInfo.getName() + " (" + campusInfo.getCity() +
                                ", " + campusInfo.getState() + ")";
                        campusRadioButton.setText(campusTitle);
                        campusRadioGroup.addView(campusRadioButton);

                        // Keeps the already selected button enabled
                        if(campusInfo.getID().equals(campusID)){
                            campusRadioButton.setChecked(true);
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets up the RadioGroup displays.
     */
    private void setUpRadioButtons(){
        campusRadioGroup.removeAllViews();
        for(CampusInfo campusInfo : campusInfoArrayList){
            RadioButton campusRadioButton =
                    new RadioButton(MyCampusActivity.this);
            String campusTitle = campusInfo.getName() + " (" + campusInfo.getCity() + ", " +
                    campusInfo.getState() + ")";
            campusRadioButton.setText(campusTitle);
            campusRadioGroup.addView(campusRadioButton);

            // Keeps the already selected button enabled
            if(campusInfo.getID().equals(campusID)){
                campusRadioButton.setChecked(true);
            }
        }
    }

    /**
     * Sets up the functionality of the confirm button.
     */
    private void setUpConfirmButton(){
        Button confirmCampus = findViewById(R.id.confirmCampus);
        confirmCampus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Finds the selected RadioButton using the given RadioGroup
                int cID = campusRadioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = findViewById(cID);

                // Ensuring that something is selected in the RadioGroup
                if(radioButton != null) {
                    Toast.makeText(getApplicationContext(),
                            radioButton.getText() + " set as your default campus",
                            Toast.LENGTH_SHORT).show();
                    campusID = "";
                    for(CampusInfo campusInfo : campusInfoArrayList){
                        String campusTitle = campusInfo.getName() + " (" + campusInfo.getCity() +
                                ", " + campusInfo.getState() + ")";
                        if(campusTitle.equals(radioButton.getText().toString())){
                            campusID = campusInfo.getID();
                        }
                    }

                    // Saving the campus ID as a Shared Preference
                    if(!campusID.equals("")){
                        SharedPreferences sharedPreferences =
                                getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KEY_TEXT, campusID);
                        editor.commit();
                        finish();
                    }
                }
            }
        });
    }

    /**
     * Sets up the shared preferences.
     */
    private void setUpSharedPreferences(){
        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        campusID = sharedPreferences.getString(KEY_TEXT, "");
    }
}
