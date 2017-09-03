package com.pheiffware.anamorphic;

import android.os.Bundle;
import android.view.View;

import com.pheiffware.lib.and.gui.LoggedActivity;

public class Anamorphic extends LoggedActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anamorphic);
        View calibrateButton = findViewById(R.id.calibrateButton);
        final AnamorphicFragment anamorphicFragment = (AnamorphicFragment) getSupportFragmentManager().findFragmentById(R.id.anamorphicFragment);
        calibrateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                anamorphicFragment.startCalibration();
            }
        });
    }
}
