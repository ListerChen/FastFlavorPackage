
package com.lister.flavourpackagetest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class FlavourActivity extends AppCompatActivity {

    private TextView mChannelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flavour);

        mChannelText = findViewById(R.id.text_channel);
        mChannelText.setText(FlavorUtils.getSignatureInfo(this));
    }
}