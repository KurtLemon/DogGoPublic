package com.kurtlemon.doggo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class WalkDogActivity extends FragmentActivity implements OnMapReadyCallback {

    // Location request code for Google Maps.
    private static final int LOCATION_REQUEST_CODE = 2;

    // Google Maps and location fields
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // The active dogs that are walking with the user.
    private ArrayList<String> activeDogIDs;

    // Intent to start the location broadcasting service.
    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_dog);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent startedIntent = getIntent();

        activeDogIDs = startedIntent.getStringArrayListExtra("activeDogIDs");

        serviceIntent = new Intent(getApplicationContext(), DogWalkLocationService.class);
        serviceIntent.putStringArrayListExtra("activeDogIDs", activeDogIDs);
        startService(serviceIntent);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.emergency_cancel_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(serviceIntent);
                Snackbar.make(view, "Emergency button pressed: You are no longer " +
                        "broadcasting your location. ", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Action", null).show();
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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

        // Checking permissions.
        if(checkLocationPermission()){
            // The user has allowed location permission.
            mMap.setMyLocationEnabled(true);
            setUpLastKnownLocation();
            setUpUserLocationUpdates();
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                // The user has not allowed location permission.
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

    /** Runs after the user has been asked for location permissions. If successful, set up the last
     *      known location and location updates.
     *
     * @param requestCode The code that was sent to the activity.
     * @param permissions The permissions required.
     * @param grantResults The results granted.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Permission for location services has been granted
                // Set up last known location services and location updates
                setUpLastKnownLocation();
                setUpUserLocationUpdates();
            }
        }
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
                    ActivityCompat.requestPermissions(WalkDogActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopService(serviceIntent);
        super.onDestroy();
    }
}
