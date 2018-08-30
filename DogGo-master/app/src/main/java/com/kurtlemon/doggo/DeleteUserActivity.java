package com.kurtlemon.doggo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DeleteUserActivity extends AppCompatActivity {

    // UI elements.
    private Button deleteButton;

    // Firebase authentication fields.
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;

    // Firebase database information.
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    // Firebase storage fields.
    private FirebaseStorage storage;
    private StorageReference storageReference;

    // User information.
    private String userID;
    private boolean reauthSuccessful;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_user);

        setUpUserInformation();
        setUpDogDatabase();
        setUpDogStorage();
        setUpDeleteButton();
    }

    /**
     * Sets up the functionality of the delete user button.
     */
    private void setUpDeleteButton(){
        deleteButton = findViewById(R.id.userDeleteFinalButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                reauthenticateUser();
                if(reauthSuccessful) {
                    deleteDatabaseEntries();
                    deletePhotoStorage();
                    deleteUser();
                }
            }
        });
    }

    /**
     * If the user has not signed in recently they need to be re-authenticated before they can
     * access the user information.
     */
    private void reauthenticateUser(){
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                String providerID = profile.getProviderId();

                if (providerID.equals("google.com")) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(user.getEmail(),
                            null);

                    user.reauthenticate(credential).addOnCompleteListener(
                            new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
                } else if (providerID.equals("emailLink")) {
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(),
                            null);

                    user.reauthenticate(credential).addOnCompleteListener(
                            new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                }
                            });
                }
                reauthSuccessful = true;
            }
        } else {
            Toast.makeText(getApplicationContext(), "There's been a user error. Try signing" +
                    " out and signing back in.", Toast.LENGTH_SHORT).show();
            reauthSuccessful = false;
        }
    }

    /**
     * Retrieves the use information from Firebase authentication.
     */
    private void setUpUserInformation(){
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        userID = user.getUid();
    }

    /**
     * Sets up the Firebase database.
     */
    private void setUpDogDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference()
                .child("dogInfo").child(userID);
    }

    /**
     * Sets up the Firebase photo storage.
     */
    private void setUpDogStorage(){
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference().child("doggo/" + userID);
    }

    /**
     * After the database has been set up, delete all relevant entries from it.
     */
    private void deleteDatabaseEntries(){
        databaseReference.removeValue();
    }

    /**
     * After the photo storage has been set up, delete all relevant entries from it.
     */
    private void deletePhotoStorage(){
        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });
    }

    /**
     * Once the user has been set up and re-authenticated, delete their information.
     */
    private void deleteUser(){
        user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(getApplicationContext(), "Thank you for using DogGo",
                            Toast.LENGTH_SHORT).show();
                    restartApp();
                } else {
                    reauthenticateUser();
                    Toast.makeText(getApplicationContext(), "There's been an error. Please " +
                            "try again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Restarts the app after everything has been deleted to show the initial start state again.
     */
    private void restartApp(){
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }
}
