package com.kurtlemon.doggo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AccountSettingsActivity extends AppCompatActivity {

    // Shared preferences information
    private final String SHARED_PREFERENCES_FILENAME = "storedData";
    private final String WALK_TIME_KEY_TEXT = "walkTime";

    // XML display fields
    private EditText walkTimeEditText;
    private Button saveSettingsButton;
    private Button deleteAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        walkTimeEditText = findViewById(R.id.walkTimeEditText);

        setUpSharedPreferences();
        setUpSaveButton();
        setUpDeleteAccountButton();
    }

    /**
     * Sets up the shared preferences.
     */
    private void setUpSharedPreferences(){
        SharedPreferences sharedPreferences =
                getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
        String walkTimeString = "" + sharedPreferences.getInt(WALK_TIME_KEY_TEXT, 30);
        walkTimeEditText.setText(walkTimeString);
    }

    /**
     * Sets up the button to save the settings.
     */
    private void setUpSaveButton(){
        saveSettingsButton = findViewById(R.id.saveSettingsButton);
        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int walkTime = Integer.parseInt(walkTimeEditText.getText().toString());

                SharedPreferences sharedPreferences =
                        getSharedPreferences(SHARED_PREFERENCES_FILENAME, 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if(walkTime != 0){
                    editor.putInt(WALK_TIME_KEY_TEXT, walkTime);
                }else {
                    editor.putInt(WALK_TIME_KEY_TEXT, 30);
                }
                editor.commit();
                Toast.makeText(AccountSettingsActivity.this, "Settings Saved",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        });
    }

    /**
     * Sets up the button to delete the user's information from the app. This sends the user to the
     * delete activity to confirm their choice.
     */
    private void setUpDeleteAccountButton(){
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent deleteUserInfoIntent = new Intent(AccountSettingsActivity.this,
                        DeleteUserActivity.class);
                startActivity(deleteUserInfoIntent);
            }
        });
    }
}