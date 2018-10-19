package com.example.bernyspc.mechse;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mMechanic, mCaruser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMechanic = (Button)findViewById(R.id.mechanic);
        mCaruser = (Button)findViewById(R.id.caruser);

        startService(new Intent(MainActivity.this, onAppKilled.class));

        mMechanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,MechLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mCaruser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,CaruserLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }
}


