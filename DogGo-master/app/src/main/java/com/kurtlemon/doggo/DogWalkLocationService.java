package com.kurtlemon.doggo;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class DogWalkLocationService extends IntentService {

    private int SERVICE_DURATION;

    // Location Services
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    // Firebase database fields
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dogDatabaseReference;

    // Firebase user information
    private FirebaseAuth firebaseAuth;

    // Shared preferences information
    private final String SHARED_PREFERENCES_FILENAME = "storedData";
    private final String WALK_TIME_KEY_TEXT = "walkTime";
    private final String CAMPUS_KEY_TEXT = "myCampus";

    // The ID of the campus the user is registered to
    private String campusID;

    // User ID is used to identify the individual user to the database.
    private String userID;

    // ArrayList of active dog IDs for this walk
    private ArrayList<String> activeDogIDs;

    private boolean itsTimeToStop = false;

    /**
     * Constructor that just calls the IntentService constructor
     */
    public DogWalkLocationService() {
        super("DogWalkLocationService");
    }

    /**
     * Once the intent that calls the service is started a timer runs for the set service duration
     * and then terminates the service. If the user navigates backwards in the app the service will
     * need to be stopped from elsewhere.
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            int counter = SERVICE_DURATION;
            while (counter > 0) {
                counter--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (itsTimeToStop) {
                    counter = 0;
                    stopSelf();
                }
            }
            stopSelf();
        }
    }

    /**
     * Runs as soon as the service is started. Initializes the ArrayList of active dog information,
     * retrieves necessary information from shared preferences, and establishes connections to the
     * database. It also prepares the location provider, gets the last known location, and creates
     * a location request to get updates.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        activeDogIDs = intent.getStringArrayListExtra("activeDogIDs");
        setUpSharedPreferences();
        setUpDogDatabase();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (checkLocationPermission()) {
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    DogLocation dogLocation = new DogLocation(location.getLatitude(),
                            location.getLongitude(), userID, activeDogIDs);
                    dogDatabaseReference.setValue(dogLocation);
                }
            });
        }
        createLocationRequest();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    DogLocation dogLocation = new DogLocation(location.getLatitude(),
                            location.getLongitude(), userID, activeDogIDs);
                    if (!itsTimeToStop) {
                        dogDatabaseReference.setValue(dogLocation);
                    }
                }
            }
        };
        startLocationUpdates();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Sets up the shared preferences connection and fetches the campus ID and the location tracking
     *  duration.
     */
    private void setUpSharedPreferences() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        campusID = sharedPreferences.getString(CAMPUS_KEY_TEXT, "ERROR: No Campus");
        SERVICE_DURATION = sharedPreferences.getInt(WALK_TIME_KEY_TEXT, 30) * 60;
    }

    /**
     * Checks the permissions and returns if the user has grated permission to check location.
     *
     * @return
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Creates a high accuracy location request for 1 per second.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Starts the location updates cycle.
     */
    private void startLocationUpdates(){
        if (checkLocationPermission()) {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
                    null);
        }
    }

    /**
     * Sets up the database for sending dog locations and also fetched the user ID.
     */
    private void setUpDogDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();
        dogDatabaseReference = firebaseDatabase.getReference().child("dogLocation").child(campusID)
                .child(userID);
    }

    @Override
    public void onDestroy() {
        itsTimeToStop = true;
        dogDatabaseReference.removeValue();
        super.onDestroy();
    }
}
