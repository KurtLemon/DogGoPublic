package com.kurtlemon.doggo;

import java.util.ArrayList;

/**
 * Created by Kurt Lamon on 2/7/2018.
 */

public class DogLocation implements Comparable<DogLocation>{
    // Location information
    private double latitude;
    private double longitude;

    // ID information
    private String userID;
    private ArrayList<String> activeDogIDs;

    public DogLocation() {
        latitude = 0;
        longitude = 0;
        userID = "";
        activeDogIDs = new ArrayList<>();
    }

    public DogLocation(double latitude, double longitude, String userID,
                       ArrayList<String> activeDogIDs) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.userID = userID;
        this.activeDogIDs = activeDogIDs;
    }

    // Getters and Setters.
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public ArrayList<String> getActiveDogIDs() {
        return activeDogIDs;
    }

    public void setActiveDogIDs(ArrayList<String> activeDogIDs) {
        this.activeDogIDs = activeDogIDs;
    }

    /**
     * Compares two DogLocations for similarity. Note: this only compares their IDs, which in theory
     * should be different.
     *
     * @param other The second DogLocation.
     * @return integer: 1 if different, 0 if same.
     */
    public int compareTo(DogLocation other){
        if(this.userID.equals(other.getUserID())){
            return 0;
        }
        return 1;
    }

}
