package com.kurtlemon.doggo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LegalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        Button tacButton = findViewById(R.id.tacButton);
        tacButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent tacIntent = new Intent(LegalActivity.this,
                        TermsAndConditionsActivity.class);
                startActivity(tacIntent);
            }
        });
        Button ppButton = findViewById(R.id.ppButton);
        ppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent ppIntent = new Intent(LegalActivity.this,
                        PrivacyPolicyActivity.class);
                startActivity(ppIntent);
            }
        });
        Button touButton = findViewById(R.id.touButton);
        touButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent touIntent = new Intent(LegalActivity.this,
                        TermsOfUseActivity.class);
                startActivity(touIntent);
            }
        });
    }
}
