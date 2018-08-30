package com.kurtlemon.doggo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MeetDogsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnMarkerClickListener {

    // Location request code for Google Maps.
    private static final int LOCATION_REQUEST_CODE = 2;

    // Shared preferences information
    private final String SHARED_PREFERENCES_FILENAME = "storedData";
    private final String CAMPUS_KEY_TEXT = "myCampus";

    // Dimensions
    private final int MARKER_WIDTH = 75;
    private final int MARKER_HEIGHT = 75;

    // Google Maps amd location fields
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // Firebase Database Fields.
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dogLocationDatabaseReference;
    private ChildEventListener dogLocationChildEventListener;

    // The ID of the campus the user is registered to
    private String campusID;

    // List of locations to appear on the map
    private ArrayList<DogLocation> dogLocationArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meet_dogs);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize the ArrayList of dog locations
        dogLocationArrayList = new ArrayList<>();

        setUpSharedPreferences();
        setUpDogDatabase();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e("ERROR", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("ERROR", "Can't find style.");
        }

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnInfoWindowLongClickListener(this);
        mMap.setOnMarkerClickListener(this);


        // Check permissions
        if(checkLocationPermission()){
            mMap.setMyLocationEnabled(true);
            setUpLastKnownLocation();
            setUpUserLocationUpdates();
        }else{
            // Permission has not been granted.
            // Request permission
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            }
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

    /** Sets up information and recollection of last known location upon starting the activity
     *
     */
    private void setUpLastKnownLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Checks permissions.
        if(checkLocationPermission()){
            // The user has given permission
            // Use the location services client to get last known location.
            Task<Location> locationTask = mFusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        LatLng userLatLng = new LatLng(location.getLatitude(),
                                location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17.0f));
                    }
                }
            });
        }else{
            // The user has not given permissions.
            ActivityCompat.requestPermissions(this, new String[]
                    {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    /**
     * Sets up live updates for the user location.
     */
    private void setUpUserLocationUpdates(){
        final LocationRequest locationRequest = new LocationRequest();
        // Set up location request intervals
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

        // Update the user's current location using the location services client
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings((builder.build()));
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                LocationCallback locationCallback = new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        for(Location location : locationResult.getLocations()){
                            // Get the user's location and zoom to it
                            LatLng latlng = new LatLng(location.getLatitude(),
                                    location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17.0f));
                        }
                    }
                };

                // Check permissions.
                if (checkLocationPermission()){
                    // Permission is granted.
                    mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                            locationCallback, null);
                } else {
                    // Permission not granted.
                    ActivityCompat.requestPermissions(MeetDogsActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE);
                }
            }
        });
    }

    /**
     * Sets up the shared preferences file for retrieving the campusID.
     */
    private void setUpSharedPreferences(){
        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        campusID = sharedPreferences.getString(CAMPUS_KEY_TEXT, "");
    }

    /**
     * Sets up the database for sending dog locations and also fetched the user ID.
     */
    private void setUpDogDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance();

        dogLocationDatabaseReference = firebaseDatabase.getReference().child("dogLocation")
                .child(campusID);
        dogLocationChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // Add a new location to the list of available dogs
                DogLocation dogLocation = dataSnapshot.getValue(DogLocation.class);
                dogLocationArrayList.add(dogLocation);
                fillMarkers();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // Fetch the location that changed and update the visible locations
                DogLocation changedDogLocation = dataSnapshot.getValue(DogLocation.class);
                ArrayList<DogLocation> toRemove = new ArrayList<>();
                for(DogLocation dogLocation : dogLocationArrayList){
                    if(dogLocation.compareTo(changedDogLocation) == 0){
                        toRemove.add(dogLocation);
                    }
                }
                dogLocationArrayList.removeAll(toRemove);
                dogLocationArrayList.add(changedDogLocation);
                fillMarkers();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Find and remove the marker from the map
                DogLocation removedDogLocation = dataSnapshot.getValue(DogLocation.class);
                ArrayList<DogLocation> toRemove = new ArrayList<>();
                for(DogLocation dogLocation : dogLocationArrayList){
                    if(dogLocation.compareTo(removedDogLocation) == 0){
                        toRemove.add(dogLocation);
                    }
                }
                dogLocationArrayList.removeAll(toRemove);
                fillMarkers();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        dogLocationDatabaseReference.addChildEventListener(dogLocationChildEventListener);
    }

    /**
     * Fills the map with all the appropriate markers and their information/InfoWindows.
     */
    private void fillMarkers(){
        // Delete all existing markers
        mMap.clear();

        for (DogLocation dogLocation : dogLocationArrayList) {
            LatLng latLng = new LatLng(dogLocation.getLatitude(), dogLocation.getLongitude());

            BitmapDrawable pawPrintBitmap = (BitmapDrawable) getResources()
                    .getDrawable(R.mipmap.paw_print_marker_logo);
            android.graphics.Bitmap smallPawPrintMarker = Bitmap.createScaledBitmap(
                    pawPrintBitmap.getBitmap(), MARKER_WIDTH, MARKER_HEIGHT, false);

            MarkerOptions pawPrintMarker = new MarkerOptions();
            pawPrintMarker.position(latLng);

            // SET TITLE TO A LIST OF ALL THE ACTIVE DOG IDs
            String dogIDs = "";
            for (String id : dogLocation.getActiveDogIDs()) {
                dogIDs += id + ",";
            }
            pawPrintMarker.title(dogIDs);
            pawPrintMarker.snippet(dogLocation.getUserID());
            pawPrintMarker.icon(BitmapDescriptorFactory.fromBitmap(smallPawPrintMarker));


            mMap.addMarker(pawPrintMarker);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        Intent infoIntent = new Intent(MeetDogsActivity.this, DogInfoActivity.class);
        infoIntent.putExtra("UserID", marker.getSnippet());
        infoIntent.putExtra("DogIDs", marker.getTitle());
        startActivity(infoIntent);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Intent infoIntent = new Intent(MeetDogsActivity.this, DogInfoActivity.class);
        infoIntent.putExtra("UserID", marker.getSnippet());
        infoIntent.putExtra("DogIDs", marker.getTitle());
        startActivity(infoIntent);
        return true;
    }
}
