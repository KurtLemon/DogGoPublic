package com.kurtlemon.doggo;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Request code for Firebase sign in
    private final int SIGN_IN_REQUEST = 1;
    private static final int LOCATION_REQUEST_CODE = 2;
    private static final int REQUEST_CHECK_SETTINGS = 3;

    // Shared preferences information
    private final String SHARED_PREFERENCES_FILENAME = "storedData";
    private final String FIRST_TIME_KEY_TEXT = "firstTime";
    private final String MY_CAMPUS_KEY_TEXT = "myCampus";
    private final String WALK_TIME_KEY_TEXT = "walkTime";

    // Firebase Authentication fields
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    // Firebase storage fields
    private FirebaseStorage storage;
    private StorageReference gsReference;

    // Firebase database fields
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

    // Necessary global views
    private View headerView;

    // On screen XML items
    private Button walkDogButton;
    private Button findDogButton;

    // User information
    private String userID;
    private String userName;
    private String userEmail;

    // User dog data
    private ArrayList<DogInfo> myDogsArrayList;

    // If the user photos are loaded
    private boolean photosLoaded = false;

    // If it's the user's first time on the app.
    private boolean firstTime;

    // Warnings
    private boolean noCampus;
    private boolean noWalkTime;
    private boolean defaultWalkTimeOkay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpSharedPreferences();
        if (firstTime) {
            displayFirstTimeInfo();
        }

        // Firebase Authentication setup
        firebaseAuth = FirebaseAuth.getInstance();

        // Firebase storage setup
        storage = FirebaseStorage.getInstance();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    // User is already signed in
                    saveUserInformation(user);
                    setUpPhotoStorage();
                    setUpHeaderPersonalization();
                }else{
                    // New user or signed out user
                    //Start a sign-in activity
                    Intent signInIntent = AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(
                                    Arrays.asList(
                                            new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER)
                                                    .build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER)
                                                    .build()
                                    )
                            ).build();
                    startActivityForResult(signInIntent, SIGN_IN_REQUEST);
                }
            }
        };

        // DEFAULT CODE: Setting up the toolbar, FAB, and drawer layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayHelp();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        headerView = navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(this);

        setUpDogButtons();
    }

    /**
     * When the activity resumes, re-add the authStateListener so it's running again.
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    /**
     * When the activity is paused, remove the authStateListener so its not using up resources in
     * the background.
     *
     */
    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
        photosLoaded = true;
    }

    /**
     * Opening and closing the drawer layout.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Sets up the main menu. Adds the sign out button to the main activity.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Currently the only item available for clicking is the sign out button, at which the current
     * user is signed out.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Menu item selection functionality.
     *
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.my_dogs) {
            Intent myDogsIntent = new Intent(MainActivity.this, MyDogsActivity.class);
            photosLoaded = true;
            startActivity(myDogsIntent);
        } else if (id == R.id.my_campus) {
            Intent myCampusIntent =
                    new Intent(MainActivity.this, MyCampusActivity.class);
            photosLoaded = true;
            startActivity(myCampusIntent);
        } else if (id == R.id.about) {
            Intent aboutUsIntent =
                    new Intent(MainActivity.this, AboutDogGoActivity.class);
            photosLoaded = true;
            startActivity(aboutUsIntent);
        } else if (id == R.id.account_settings) {
            Intent accountSettingsIntent =
                    new Intent(MainActivity.this, AccountSettingsActivity.class);
            photosLoaded = true;
            startActivity(accountSettingsIntent);
        } else if (id == R.id.legal) {
            Intent legalIntent = new Intent(MainActivity.this, LegalActivity.class);
            photosLoaded = true;
            startActivity(legalIntent);
        } else if (id == R.id.doggo_instagram) {
            Uri uri = Uri.parse("http://instagram.com/_u/a_literally_penguin");
            Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

            likeIng.setPackage("com.instagram.android");

            try {
                startActivity(likeIng);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://instagram.com/dog.go.app")));
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    /**
     * Used to save any necessary local information obtained by user sign in. Currently fetches only
     * the user ID for referencing data that will be stored, but later will most likely fetch the
     * name and email for display.
     *
     * @param user
     */
    private void saveUserInformation(FirebaseUser user){
        userID = user.getUid();
        userName = user.getDisplayName();
        userEmail = user.getEmail();
    }

    /**
     * Used to set up the correct display for the personalized information text fields. User's
     * name/google id, and user's email. The photos are handled in a separate method that is tied to
     * the database listener.
     */
    private void setUpHeaderPersonalization(){
        TextView userNameTextView = headerView.findViewById(R.id.userNameTextView);
        userNameTextView.setText(userName);
        TextView userEmailTextView = headerView.findViewById(R.id.userEmailTextView);
        userEmailTextView.setText(userEmail);
    }

    /**
     * Updated the user profile header with their dogs' photos. This is to be called from the
     * ChildEventListener that is attached to the dog photo database, which is why it passes in the
     * DogInfo object. The dog info object is the current dog that has just been fetched from the
     * database.
     *
     * @param dogInfo
     */
    private void refreshHeaderPhotos(DogInfo dogInfo){
        final LinearLayout userDogPhotosLayout = headerView.findViewById(R.id.userDogPhotosLayout);

        final ImageView dogPhotoImageView = new ImageView(MainActivity.this);
        if (!photosLoaded) {
            String storageURL = "gs://doggo-38323.appspot.com/doggo/" + userID + "/"
                    + dogInfo.getDogID() + ".png";
            gsReference = storage.getReferenceFromUrl(storageURL);
            gsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Load the image into the image view
                    Glide.with(getApplicationContext()).load(uri).override(200, 200)
                            .into(dogPhotoImageView);
                    userDogPhotosLayout.addView(dogPhotoImageView);
                }
            });
        }
    }

    /**
     * Sets up the user's photos and database references so the user profiles section includes
     * pictures of thier dogs.
     *
     */
    private void setUpPhotoStorage(){
        myDogsArrayList = new ArrayList<>();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("dogInfo").child(userID);
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //Add the dog info from the database to the arraylist whenever a new dog is found
                DogInfo dogInfo = dataSnapshot.getValue(DogInfo.class);
                if(dogInfo != null) {
                    myDogsArrayList.add(dogInfo);
                    refreshHeaderPhotos(dogInfo);
                }
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
     * Sets up the functionality of both the walk button and find button on the main screen.
     */
    private void setUpDogButtons(){
        // Functionality of the "Go For a Walk" button
        walkDogButton = findViewById(R.id.walkDogButton);
        walkDogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences =
                        getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
                noCampus = sharedPreferences.getString(MY_CAMPUS_KEY_TEXT, "").equals("");
                noWalkTime = sharedPreferences.getInt(WALK_TIME_KEY_TEXT, 0) == 0;
                // If the user has multiple dogs registered, ask which will be walking
                if (noCampus) {
                    AlertDialog.Builder noCampusAlert =
                            new AlertDialog.Builder(MainActivity.this);
                    noCampusAlert.setTitle("Set a Campus");
                    noCampusAlert.setMessage("To show up on the Dog Map you need to select what " +
                            "campus you're walking on or near. Tap the side-bar and select My " +
                            "Campus to set one.");
                    noCampusAlert.show();
                }
                if (noWalkTime) {
                    AlertDialog.Builder noWalkTimeAlert =
                            new AlertDialog.Builder(MainActivity.this);
                    noWalkTimeAlert.setTitle("Set a Walk Time");
                    noWalkTimeAlert.setMessage("You haven't set a walk time yet. Tap the " +
                            "side-bar and navigate to Account Settings to set one.");
                    noWalkTimeAlert.show();
                }
                if(myDogsArrayList.size() >= 1){
                    if (checkLocationPermission()) {
                        createLocationRequest();
                    } else {
                        requestLocationPermission();
                    }
                } else {
                    AlertDialog.Builder noDogsAlert =
                            new AlertDialog.Builder(MainActivity.this);
                    noDogsAlert.setTitle("You have no dog profiles with us. Please add one before" +
                            " walking.");
                    NoDogsOnClickListener noDogsOnClickListener = new NoDogsOnClickListener();
                    noDogsAlert.setPositiveButton("Create a Profile Now",
                            noDogsOnClickListener);
                    noDogsAlert.setNegativeButton("Cancel", noDogsOnClickListener);
                    noDogsAlert.show();
                }
            }
        });

        // Functionality of the "Find Dogs" button
        findDogButton = findViewById(R.id.findDogButton);
        findDogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createFindDogLocationRequest();
            }
        });
    }

    /**
     * The OnClickListener class to handle when the user wants to go for a walk.
     */
    private class WalkOnClickListener implements DialogInterface.OnClickListener {
        ArrayList<String> activeDogIDs;
        public WalkOnClickListener(ArrayList<String> activeDogIDs) {
            this.activeDogIDs = activeDogIDs;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (activeDogIDs.size() < 1) {
                        Toast.makeText(getApplicationContext(),
                                "Please select at least one dog to walk with",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Intent walkDogIntent =
                                new Intent(MainActivity.this, WalkDogActivity.class);
                        walkDogIntent.putStringArrayListExtra("activeDogIDs", activeDogIDs);
                        photosLoaded = true;
                        startActivity(walkDogIntent);
                    }
                    break;
                case DialogInterface.BUTTON_NEGATIVE:

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * The on click listener for the warning that there are no dogs registered to walk.
     */
    private class NoDogsOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
                case DialogInterface.BUTTON_POSITIVE:
                    Intent addDogIntent = new Intent(MainActivity.this,
                            AddDogActivity.class);
                    photosLoaded = true;
                    startActivity(addDogIntent);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:

                    break;
            }
        }
    }

    /**
     * The on click listener for the alert dialog to review the legal information.
     */
    private class FirstTimeOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Intent legalIntent = new Intent(MainActivity.this, LegalActivity.class);
            startActivity(legalIntent);
        }
    }

    /**
     * Checks ACCESS_FINE_LOCATION permissions of the app and returns a boolean if granted, does not
     * request permissions.
     *
     * @return boolean if permissions have been granted
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests permission for the app to access ACCESS_FINE_LOCATION.
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    /**
     * Callback after permissions are requested. If the permissions are granted it continues as
     * normal, if not then it will Toast alert the user and re-request permissions.
     *
     * @param requestCode The request code of the permissions requested
     * @param permissions The string permission
     * @param grantResults The results granted (yes, no, or cancel)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions have been granted, continue as normal
                createLocationRequest();
            } else {
                Toast.makeText(this,
                        "DogGo requires location permissions for you to meet and walk dogs",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Creates the location request to establish an initial position for the user finding dogs.
     */
    protected void createFindDogLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Intent findDogsIntent =
                        new Intent(MainActivity.this, MeetDogsActivity.class);
                photosLoaded = true;
                startActivity(findDogsIntent);
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {

                    }
                }
            }
        });
    }

    /**
     * Creates the location request required by the WalkDogLocationService to determine permissions.
     */
    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                final String[] dogNamesArray = new String[myDogsArrayList.size()];
                final ArrayList<String> activeDogIDs = new ArrayList<>();
                for(int i = 0; i < myDogsArrayList.size(); i++){
                    dogNamesArray[i] = myDogsArrayList.get(i).getName();
                }
                if ((!noWalkTime || defaultWalkTimeOkay) && !noCampus) {
                    AlertDialog.Builder walkAlert =
                            new AlertDialog.Builder(MainActivity.this);
                    walkAlert.setTitle("Select the dog(s) who will be walking today.");
                    walkAlert.setMultiChoiceItems(dogNamesArray, null,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i,
                                                    boolean isClicked) {
                                    if (isClicked) {
                                        for (DogInfo dogInfo : myDogsArrayList) {
                                            if (dogInfo.getName().equals(dogNamesArray[i])) {
                                                activeDogIDs.add(dogInfo.getDogID());
                                            }
                                        }
                                    } else {
                                        for (DogInfo dogInfo : myDogsArrayList) {
                                            if (dogInfo.getName().equals(dogNamesArray[i])) {
                                                activeDogIDs.remove(dogInfo.getDogID());
                                            }
                                        }
                                    }
                                }
                            });
                    WalkOnClickListener walkOnClickListener = new WalkOnClickListener(activeDogIDs);
                    walkAlert.setPositiveButton("Let's go!", walkOnClickListener);
                    walkAlert.setNegativeButton("Cancel", walkOnClickListener);
                    walkAlert.show();
                }

            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {

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
        firstTime = sharedPreferences.getBoolean(FIRST_TIME_KEY_TEXT, true);
        noCampus = sharedPreferences.getString(MY_CAMPUS_KEY_TEXT, "").equals("");
        noWalkTime = sharedPreferences.getInt(WALK_TIME_KEY_TEXT, 0) == 0;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(FIRST_TIME_KEY_TEXT, false);
        editor.commit();
    }

    /**
     * Displays help info.
     */
    private void displayHelp(){
        AlertDialog.Builder firstTimeAlert =
                new AlertDialog.Builder(MainActivity.this);
        firstTimeAlert.setTitle("Welcome to DogGo!");
        firstTimeAlert.setMessage("If you're here to walk your dog, get started by tapping " +
                "the side bar and creating a profile for your dogs in the \"My Dogs\" " +
                "section. If you're here to pet dogs, just close this window and tap " +
                "\"Find Dogs\"\n\nMore information and settings can also be found by tapping " +
                "the side bar.");
        firstTimeAlert.show();
    }

    /**
     * Displays the legal information on first startup.
     */
    private void displayFirstTimeInfo(){
        AlertDialog.Builder firstTimeAlert =
                new AlertDialog.Builder(MainActivity.this);
        firstTimeAlert.setTitle("Welcome to DogGo!");
        firstTimeAlert.setMessage("Before using DogGo you must review our Terms and Conditions, " +
                "Privacy Policy, and Terms of Use Regarding User Generated Content. Any use of " +
                "DogGo will constitute acceptance of these agreements. To review them, select " +
                "below. Thank you for downloading and using DogGo!");
        FirstTimeOnClickListener firstTimeOnClickListener = new FirstTimeOnClickListener();
        firstTimeAlert.setPositiveButton("Review Agreements", firstTimeOnClickListener);
        firstTimeAlert.show();
    }
}
