package com.kurtlemon.doggo;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

public class MyDogsActivity extends AppCompatActivity {

    // Activity constants
    private final int ADD_DOG_REQUEST = 1;
    private final int EDIT_DOG_REQUEST = 2;

    // Firebase storage fields
    private FirebaseStorage storage;
    private StorageReference gsReference;

    // Firebase database fields
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

    // Firebase user information
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;

    // ID information
    private String userID;

    // List of dogs that belong to the user
    private ArrayList<DogInfo> myDogsArrayList;

    // Fields needed to display the dog list
    private ListView myDogsListView;
    private ArrayAdapter<DogInfo> myDogsArrayAdapter;

    // Other views
    private TextView noDogsTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_dogs);

        // TextView for if there are no dogs to display
        noDogsTextView = findViewById(R.id.noDogsTextView);
        noDogsTextView.setVisibility(View.VISIBLE);

        // Firebase storage setup
        storage = FirebaseStorage.getInstance();

        // List of user's dogs
        myDogsArrayList = new ArrayList<>();

        // Custom array adaptor setup
        myDogsArrayAdapter = new ArrayAdapter<DogInfo>(this, R.layout.my_dog_listview_row,
                R.id.dogNameTextView,
                myDogsArrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Sub-views for each list item
                TextView dogNameTextView = view.findViewById(R.id.dogNameTextView);
                final ImageView dogPhotoImageView = view.findViewById(R.id.dogPhotoImageView);

                dogNameTextView.setText(myDogsArrayList.get(position).getName());

                // Setup for the storage
                String storageURL = "gs://doggo-38323.appspot.com/doggo/" + userID + "/"
                        + myDogsArrayList.get(position).getDogID() + ".png";
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
        myDogsListView = findViewById(R.id.myDogsListView);
        myDogsListView.setAdapter(myDogsArrayAdapter);

        // Opens a dialog to determine if the user wants to delete a dog, edit the info, or cancel
        myDogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AlertDialog.Builder editAlert = new AlertDialog.Builder(MyDogsActivity.this);

                DogEditOnClickListener dogEditOnClickListener = new DogEditOnClickListener(i);

                // Defining the alert dialog display
                editAlert.setTitle("Edit " + myDogsArrayList.get(i).getName() + "'s info");
                editAlert.setMessage("What would you like to do?");
                editAlert.setPositiveButton("Cancel", dogEditOnClickListener);
                editAlert.setNeutralButton("Delete", dogEditOnClickListener);
                editAlert.setNegativeButton("Edit", dogEditOnClickListener);
                editAlert.show();
            }
        });

        setUpDatabase();

        // Button to add a new dog
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addDogIntent = new Intent(
                        MyDogsActivity.this, AddDogActivity.class);
                startActivityForResult(addDogIntent, ADD_DOG_REQUEST);
            }
        });
    }

    /**
     * Does all the work to set up the firebase database to store the user's dogs
     */
    private void setUpDatabase(){
        // Authentication setup
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        userID = user.getUid();

        // Database setup
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("dogInfo").child(userID);
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //Add the dog info from the database to the arraylist whenever a new dog is found
                DogInfo dogInfo = dataSnapshot.getValue(DogInfo.class);
                myDogsArrayList.add(dogInfo);
                myDogsArrayAdapter.notifyDataSetChanged();

                // If there are dogs, hide the no-dogs TextView
                if(myDogsArrayList.size() > 0){
                    noDogsTextView.setVisibility(View.INVISIBLE);
                }else{
                    noDogsTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                DogInfo dogInfo = dataSnapshot.getValue(DogInfo.class);
                for(int i = 0; i < myDogsArrayList.size(); i++){
                    if(dogInfo.getDogID().equals(myDogsArrayList.get(i).getDogID())){
                        myDogsArrayList.remove(i);
                        i--;
                    }
                }
                myDogsArrayList.add(dogInfo);
                myDogsArrayAdapter.notifyDataSetChanged();

                // If there are dogs, hide the no-dogs TextView
                if(myDogsArrayList.size() > 0){
                    noDogsTextView.setVisibility(View.INVISIBLE);
                }else{
                    noDogsTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // If there are dogs, hide the no-dogs TextView
                if(myDogsArrayList.size() > 0){
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

    // Inner class to handle the list view clicks
    private class DogEditOnClickListener implements DialogInterface.OnClickListener{
        private int position;
        public DogEditOnClickListener(int position){
            this.position = position;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch(i){
                case DialogInterface.BUTTON_POSITIVE:
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    AlertDialog.Builder deleteConfirmAlert =
                            new AlertDialog.Builder(MyDogsActivity.this);

                    DogDeleteOnClickListener dogDeleteOnClickListener =
                            new DogDeleteOnClickListener(position);

                    // Defining the alert dialog display
                    deleteConfirmAlert.setTitle("Delete");
                    deleteConfirmAlert.setMessage("Are you sure you want to delete " +
                            myDogsArrayList.get(position).getName() + "'s info");
                    deleteConfirmAlert.setPositiveButton("Yes, Delete",
                            dogDeleteOnClickListener);
                    deleteConfirmAlert.setNegativeButton("No, Go Back",
                            dogDeleteOnClickListener);
                    deleteConfirmAlert.show();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    Intent editDogIntent = new Intent(
                            MyDogsActivity.this, AddDogActivity.class);
                    editDogIntent.putExtra("name",
                            myDogsArrayList.get(position).getName());
                    editDogIntent.putExtra("description",
                            myDogsArrayList.get(position).getDescription());
                    editDogIntent.putExtra("id", myDogsArrayList.get(position).getDogID());
                    startActivityForResult(editDogIntent, EDIT_DOG_REQUEST);
                    break;
                default:
                    break;
            }
        }
    }

    // Inner class to handle deleting a dog's information.
    private class DogDeleteOnClickListener implements DialogInterface.OnClickListener{
        private int position;
        public DogDeleteOnClickListener(int position){
            this.position = position;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch(i){
                case DialogInterface.BUTTON_POSITIVE:
                    // Set up the storage reference and delete a specific dog photo
                    String storageURL = "gs://doggo-38323.appspot.com/doggo/" + userID + "/"
                            + myDogsArrayList.get(position).getDogID() + ".png";
                    gsReference = storage.getReferenceFromUrl(storageURL);
                    gsReference.delete();

                    // Set up the database reference and delete a specific dog info
                    databaseReference = firebaseDatabase.getReference().child("dogInfo")
                            .child(userID).child(myDogsArrayList.get(position).getDogID());
                    databaseReference.removeValue();

                    // Delete a dog's info from the local data
                    myDogsArrayList.remove(position);

                    // Update the view
                    myDogsArrayAdapter.notifyDataSetChanged();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * If the activity returns from the edit activity, it updates the data set to get current images
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        myDogsArrayAdapter.notifyDataSetChanged();
    }
}
