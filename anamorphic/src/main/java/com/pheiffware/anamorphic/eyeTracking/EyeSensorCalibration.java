package com.pheiffware.anamorphic.eyeTracking;

import android.content.SharedPreferences;

/**
 * Holds the information describing a calibration of the eye sensor.  This can transform the information from a face tracker to an eye position in "Screen Coordinates".
 * Created by Steve on 9/3/2017.
 */

public class EyeSensorCalibration
{
    private static final String NORM_WIDTH = "normWidthCalibration";
    private static final String NORM_HEIGHT = "normHeightCalibration";
    private static final String OFFSET_X = "offsetX";
    private static final String OFFSET_Y = "offsetY";
    float normWidthCalibration;
    float normHeightCalibration;
    float offsetX;
    float offsetY;

    public EyeSensorCalibration()
    {
        normWidthCalibration = 1;
        normHeightCalibration = 1;
        offsetX = 0;
        offsetY = 0;
    }

    public EyeSensorCalibration(SharedPreferences pref)
    {
        normWidthCalibration = pref.getFloat(NORM_WIDTH, 1);
        normHeightCalibration = pref.getFloat(NORM_HEIGHT, 1);
        offsetX = pref.getFloat(OFFSET_X, 0);
        offsetY = pref.getFloat(OFFSET_Y, 0);
    }

    public void write(SharedPreferences.Editor editor)
    {
        editor.putFloat(NORM_WIDTH, normWidthCalibration);
        editor.putFloat(NORM_HEIGHT, normHeightCalibration);
        editor.putFloat(OFFSET_X, offsetX);
        editor.putFloat(OFFSET_Y, offsetY);

    }

}
