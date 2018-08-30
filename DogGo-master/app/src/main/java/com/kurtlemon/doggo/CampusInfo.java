package com.kurtlemon.doggo;

import java.util.UUID;

/**
 * Created by Kurt Lamon on 1/29/2018.
 */

public class CampusInfo {

    // Campus name
    private String name;

    // Campus location information
    private String state;
    private String city;

    // Campus radius. This currently isn't used, but will be necessary for implementing GeoFence
    //  functionality.
    private int radius;

    // Campus coordinates used for map functions.
    private double latitude;
    private double longitude;

    // Campus ID used for database connections.
    private String id;

    /**
     * Default Value Constructor-- Should never be used.
     */
    public CampusInfo() {
        this.name = "";
        this.state = "";
        this.city = "";
        this.radius = 0;
        this.latitude = 0;
        this.longitude = 0;
        this.id = "";
    }

    /**
     * Constructor without a campus ID. A random one is created.
     *
     * @param name Campus name.
     * @param state The state that the campus is in.
     * @param city The city that the campus is in.
     * @param radius The size of the campus, currently not used (defaulted to be 1).
     * @param latitude The latitude of the campus.
     * @param longitude The longitude of the campus.
     */
    public CampusInfo(String name, String state, String city, int radius, double latitude,
                      double longitude) {
        this.name = name;
        this.state = state;
        this.city = city;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
        this.id = "" + UUID.randomUUID();
    }

    /**
     * Constructor with a campus ID.
     *
     * @param name Campus name.
     * @param state The state that the campus is in.
     * @param city The city that the campus is in.
     * @param radius The size of the campus, currently not used (defaulted to be 1).
     * @param latitude The latitude of the campus.
     * @param longitude The longitude of the campus.
     * @param id The campus ID.
     */
    public CampusInfo(String name, String state, String city, int radius, double latitude,
                      double longitude, String id) {
        this.name = name;
        this.state = state;
        this.city = city;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
        this.id = id;
    }

    // Getters and Setters.
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

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

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    /**
     * Searches the CampusInfo object for the search term. Searches within the name, state, and
     * city.
     *
     * @param term The string search term.
     * @return Boolean true if the term is found, false if the term is not found.
     */
    public boolean search(String term){
        return name.toLowerCase().contains(term) || state.toLowerCase().contains(term)
                || city.toLowerCase().contains(term);
    }
}
