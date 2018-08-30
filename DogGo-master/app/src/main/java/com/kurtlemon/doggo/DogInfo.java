package com.kurtlemon.doggo;

/**
 * Created by Kurt Lamon on 12/29/2017.
 *
 * Represents the information that each user provides about one dog.
 */

public class DogInfo {
    // Dog information
    private String dogID;
    private String name;
    private String description;

    /**
     * Default value constructor: DO NOT USE
     */
    public DogInfo(){
        dogID = "";
        name = "";
        description = "";
    }

    /**
     * Constructor, takes the dog's unique ID, name, and description
     *
     * @param dogID
     * @param name
     * @param description
     */
    public DogInfo(String dogID, String name, String description){
        this.dogID = dogID;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters.
    public String getDogID() {
        return dogID;
    }

    public void setDogID(String dogID) {
        this.dogID =dogID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
