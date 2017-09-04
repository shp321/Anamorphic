package com.pheiffware.anamorphic.eyeTracking;

/**
 * Created by Steve on 9/3/2017.
 */

public class EyeSensorCalibration
{
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

    public EyeSensorCalibration(float normWidthCalibration, float normHeightCalibration, float offsetX, float offsetY)
    {
        this.normWidthCalibration = normWidthCalibration;
        this.normHeightCalibration = normHeightCalibration;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
}
