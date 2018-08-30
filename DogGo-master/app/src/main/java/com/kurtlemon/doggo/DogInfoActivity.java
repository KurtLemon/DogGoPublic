package com.kurtlemon.doggo;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DogInfoActivity extends AppCompatActivity {

    // Firebase user and Auth fields
    private FirebaseUser currentUser;
    private FirebaseAuth firebaseAuth;

    // Firebase storage fields
    private FirebaseStorage storage;
    private StorageReference gsReference;

    // Firebase database fields
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private DatabaseReference reportDatabaseReference;
    private ChildEventListener childEventListener;

    // ID information
    private String userID;
    private String currentUserID;

    // List of dogs that belong to the user
    private ArrayList<DogInfo> infoDogsArrayList;
    private ArrayList<String> activeDogIDs;

    // Fields needed to display the dog list
    private ListView infoDogsListView;
    private ArrayAdapter<DogInfo> infoDogsArrayAdapter;

    // Other views
    private TextView noDogsTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_info);

        // TextView for if there are no dogs to display
        noDogsTextView = findViewById(R.id.noInfoDogsTextView);
        noDogsTextView.setVisibility(View.VISIBLE);

        // Firebase storage setup
        storage = FirebaseStorage.getInstance();

        // List of user's dogs
        infoDogsArrayList = new ArrayList<>();

        // Custom array adaptor setup
        infoDogsArrayAdapter = new ArrayAdapter<DogInfo>(this,
                R.layout.dog_info_listview_row, R.id.dogInfoNameTextView, infoDogsArrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Sub-views for each list item
                TextView dogNameTextView = view.findViewById(R.id.dogInfoNameTextView);
                TextView dogDescriptionTextView =
                        view.findViewById(R.id.dogInfoDescriptionTextView);
                final ImageView dogPhotoImageView = view.findViewById(R.id.dogInfoImageView);

                dogNameTextView.setText(infoDogsArrayList.get(position).getName());
                dogDescriptionTextView.setText(infoDogsArrayList.get(position).getDescription());

                // Setup for the storage
                String storageURL = "gs://doggo-38323.appspot.com/doggo/" + userID + "/"
                        + infoDogsArrayList.get(position).getDogID() + ".png";
                gsReference = storage.getReferenceFromUrl(storageURL);
                gsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //Load the image into the image view
                        Glide.with(getApplicationContext()).load(uri).override(300, 300)
                                .into(dogPhotoImageView);
                    }
                });
                return view;
            }
        };

        // Set the listviews and adaptors
        infoDogsListView = findViewById(R.id.dog_info_listview);
        infoDogsListView.setAdapter(infoDogsArrayAdapter);

        setUpInformation();
        setUpDatabase();
        setUpReportDatabase();
        setUpReportButton();
    }

    /**
     * Does all the work to set up the firebase database to store the user's dogs
     */
    private void setUpDatabase(){
        // Database setup
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("dogInfo").child(userID);
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //Add the dog info from the database to the arraylist whenever a new dog is found
                DogInfo dogInfo = dataSnapshot.getValue(DogInfo.class);

                boolean toBeAdded = false;

                for (int i = 0; i < activeDogIDs.size(); i++) {
                    if (dogInfo.getDogID().equals(activeDogIDs.get(i))) {
                        toBeAdded = true;
                    }
                }

                if (toBeAdded) {
                    infoDogsArrayList.add(dogInfo);
                    infoDogsArrayAdapter.notifyDataSetChanged();
                }

                // If there are dogs, hide the no-dogs TextView
                if(infoDogsArrayList.size() > 0){
                    noDogsTextView.setVisibility(View.INVISIBLE);
                }else{
                    noDogsTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                DogInfo dogInfo = dataSnapshot.getValue(DogInfo.class);

                boolean toBeAdded = false;

                for (int i = 0; i < activeDogIDs.size(); i++) {
                    if (dogInfo.getDogID().equals(activeDogIDs.get(i))) {
                        toBeAdded = true;
                    }
                }

                if (toBeAdded) {
                    for(int i = 0; i < infoDogsArrayList.size(); i++){
                        if(dogInfo.getDogID().equals(infoDogsArrayList.get(i).getDogID())){
                            infoDogsArrayList.remove(i);
                            i--;
                        }
                    }
                    infoDogsArrayList.add(dogInfo);
                    infoDogsArrayAdapter.notifyDataSetChanged();
                }

                // If there are dogs, hide the no-dogs TextView
                if(infoDogsArrayList.size() > 0){
                    noDogsTextView.setVisibility(View.INVISIBLE);
                }else{
                    noDogsTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // If there are dogs, hide the no-dogs TextView
                if(infoDogsArrayList.size() > 0){
                    noDogsTextView.setVisibility(View.INVISIBLE);
                }else{
                    noDogsTextView.setVisibility(View.VISIBLE);
                }
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
     * Sets up the user information and Firebase authentication. Also sets up the dog info IDs.
     */
    private void setUpInformation(){
        Intent sourceIntent = getIntent();
        userID = sourceIntent.getStringExtra("UserID");

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        currentUserID = currentUser.getUid();

        activeDogIDs = new ArrayList<>();
        String dogIDs = sourceIntent.getStringExtra("DogIDs");
        while (dogIDs.length() > 1) {
            if (dogIDs.charAt(0) == ',') {
                dogIDs = dogIDs.substring(1);
            }
            if (dogIDs.contains(",")) {
                activeDogIDs.add(dogIDs.substring(0, dogIDs.indexOf(",")));
                dogIDs = dogIDs.substring(dogIDs.indexOf(","));
            }
        }
    }

    /**
     * Sets up the functionality of the report user for inappropriate content button.
     */
    private void setUpReportButton(){
        Button reportButton = findViewById(R.id.userReportButton);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder reportAlert =
                        new AlertDialog.Builder(DogInfoActivity.this);

                ReportUserOnClickListener reportUserOnClickListener =
                        new ReportUserOnClickListener();

                // Defining the alert dialog display
                reportAlert.setTitle("Report Dog Walker");
                reportAlert.setMessage("Only report a dog walker if their photos, dog name, or " +
                        "dog description is inappropriate.");
                reportAlert.setPositiveButton("Report User", reportUserOnClickListener);
                reportAlert.setNegativeButton("Cancel", reportUserOnClickListener);
                reportAlert.show();
            }
        });
    }

    /**
     * Sets up the database reference for keeping track of user reports.
     */
    private void setUpReportDatabase() {
        reportDatabaseReference = firebaseDatabase.getReference().child("userReports")
                .child(userID);
    }

    /**
     * The click listener for the report user button.
     */
    private class ReportUserOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
                case DialogInterface.BUTTON_POSITIVE:
                    AlertDialog.Builder confirmReportAlert = new AlertDialog.Builder(
                            DogInfoActivity.this);

                    ConfirmReportOnClickListener confirmReportOnClickListener =
                            new ConfirmReportOnClickListener();

                    confirmReportAlert.setTitle("Confirm Report");
                    confirmReportAlert.setMessage("Would you like to add a comment to your" +
                            " report? The information is for our use only and will not be shared" +
                            " with the Dog Walker.");
                    confirmReportAlert.setPositiveButton("Add a comment",
                            confirmReportOnClickListener);
                    confirmReportAlert.setNegativeButton("Report without a comment",
                            confirmReportOnClickListener);
                    confirmReportAlert.setNeutralButton("Cancel",
                            confirmReportOnClickListener);

                    confirmReportAlert.show();

                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    }

    /**
     * The click listener for the confirming the report.
     */
    private class ConfirmReportOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
                case DialogInterface.BUTTON_POSITIVE:
                    Intent reportIntent = new Intent(DogInfoActivity.this,
                            UserReportActivity.class);
                    reportIntent.putExtra("UserID", userID);
                    reportIntent.putExtra("ReporterID", currentUserID);
                    startActivity(reportIntent);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    Date currentDate = Calendar.getInstance().getTime();
                    UserReport report = new UserReport(userID, currentDate, currentUserID,
                            "");
                    reportDatabaseReference.push().setValue(report);
                    break;

                case DialogInterface.BUTTON_NEUTRAL:
                    break;
            }
        }
    }
}
