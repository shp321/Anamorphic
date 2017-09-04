package com.pheiffware.anamorphic.eyeTracking;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Steve on 9/3/2017.
 */
class EyeSensorCalibrator
{
    private final EyeSensorCalibration calibration;
    private final int desiredNumSamples;
    private final float calibrationX;
    private final float calibrationY;
    private final float calibrationZ;

    private final List<Float> normalizedFaceWidths = new LinkedList<>();
    private final List<Float> normalizedFaceHeights = new LinkedList<>();
    private final List<Float> projectedFaceXs = new LinkedList<>();
    private final List<Float> projectedFaceYs = new LinkedList<>();

    public EyeSensorCalibrator(EyeSensorCalibration calibration, int desiredNumSamples, float calibrationX, float calibrationY, float calibrationZ)
    {
        this.calibration = calibration;
        this.desiredNumSamples = desiredNumSamples;
        this.calibrationX = calibrationX;
        this.calibrationY = calibrationY;
        this.calibrationZ = calibrationZ;
    }

    public void submitSample(float normalizedFaceWidth, float normalizedFaceHeight, float projectedFaceX, float projectedFaceY)
    {
        normalizedFaceWidths.add(normalizedFaceWidth);
        normalizedFaceHeights.add(normalizedFaceHeight);
        projectedFaceXs.add(projectedFaceX);
        projectedFaceYs.add(projectedFaceY);
    }

    void calibrate()
    {
        int numSamples = normalizedFaceWidths.size();
        float averageNormWidth = 0;
        float averageNormHeight = 0;
        float averageProjectedX = 0;
        float averageProjectedY = 0;
        for (int i = 0; i < numSamples; i++)
        {
            averageNormWidth += normalizedFaceWidths.get(i);
            averageNormHeight += normalizedFaceHeights.get(i);
            averageProjectedX += projectedFaceXs.get(i);
            averageProjectedY += projectedFaceYs.get(i);
        }
        averageNormWidth /= numSamples;
        averageNormHeight /= numSamples;
        averageProjectedX /= numSamples;
        averageProjectedY /= numSamples;

        calibration.normWidthCalibration = averageNormWidth * calibrationZ;
        calibration.normHeightCalibration = averageNormHeight * calibrationZ;
        calibration.offsetX = -calibrationX - averageProjectedX * calibrationZ;
        calibration.offsetY = -calibrationY - averageProjectedY * calibrationZ;
    }

    int numSamples()
    {
        return normalizedFaceWidths.size();
    }

    boolean done()
    {
        return numSamples() >= desiredNumSamples;
    }

}
