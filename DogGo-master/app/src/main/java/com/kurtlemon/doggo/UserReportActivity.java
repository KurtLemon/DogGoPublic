package com.kurtlemon.doggo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;

public class UserReportActivity extends AppCompatActivity {

    // Firebase database fields
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reportDatabaseReference;

    // ID information
    private String userID;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report);
        setUpInformation();
        setUpReportDatabase();
        setUpReportButton();
    }

    /**
     * Sets up the necessary information about the users.
     */
    private void setUpInformation(){
        Intent sourceIntent = getIntent();
        userID = sourceIntent.getStringExtra("UserID");
        currentUserID = sourceIntent.getStringExtra("ReporterID");
    }

    /**
     * Sets up access to the Firebase database for filing the report.
     */
    private void setUpReportDatabase() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        reportDatabaseReference = firebaseDatabase.getReference().child("userReports")
                .child(userID);
    }

    /**
     * Sets up the report button functionality.
     */
    private void setUpReportButton() {
        Button reportButton = findViewById(R.id.reportButton);
        final EditText reportEditText = findViewById(R.id.reportMessageEditText);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date currentDate = Calendar.getInstance().getTime();
                String comment = reportEditText.getText().toString();
                Log.d("DEBUG, comment", comment);
                UserReport report = new UserReport(userID, currentDate, currentUserID, comment);
                reportDatabaseReference.push().setValue(report);
                Toast.makeText(UserReportActivity.this,
                        "Thank you for filing a report. We'll look into that!",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
