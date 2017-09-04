package com.pheiffware.anamorphic.eyeTracking;

import com.pheiffware.lib.graphics.Vec4F;

/**
 * Calculates the position of the viewer's eye relative to the center of the screen in "Screen Coordinates" (see Anamorphic Camera).
 * Created by Steve on 9/3/2017.
 */
public class EyeSensor
{
    private final float cameraPreviewWidth;
    private final float cameraPreviewHeight;
    private final float tanFOVX;
    private final float tanFOVY;

    private final EyeSensorCalibration eyeSensorCalibration;
    private EyeSensorCalibrator calibrator;
    private String calibration;

    public EyeSensor(EyeSensorCalibration eyeSensorCalibration, float cameraPreviewWidth, float cameraPreviewHeight, float cameraFOVY, float cameraFOVX)
    {
        this.eyeSensorCalibration = eyeSensorCalibration;
        this.cameraPreviewWidth = cameraPreviewWidth;
        this.cameraPreviewHeight = cameraPreviewHeight;
        tanFOVX = (float) Math.tan(Math.toRadians(cameraFOVY / 2.0));
        tanFOVY = (float) Math.tan(Math.toRadians(cameraFOVX / 2.0));
    }

    /**
     * Causes the eye sensor to start calibrating.  The viewer should hold their face so that their "eye" is at the given calibration (x,y,z).
     * While calibrating, each new call to getEyePosition() will update the calibration and produce an eye position using the latest calibration.
     *
     * @param desiredNumSamples the number of calibration samples to take
     * @param calibrationX      the x position the eye should be at during the calibration process
     * @param calibrationY      the y position the eye should be at during the calibration process
     * @param calibrationZ      the z position the eye should be at during the calibration process
     */
    public void calibrate(int desiredNumSamples, float calibrationX, float calibrationY, float calibrationZ)
    {
        calibrator = new EyeSensorCalibrator(eyeSensorCalibration, desiredNumSamples, calibrationX, calibrationY, calibrationZ);
    }

    /**
     * Given information about the face, calculate the eye position in "Screen Coordinates".
     *
     * @param faceWidth
     * @param faceHeight
     * @param facePixelX
     * @param facePixelY
     * @return
     */
    public Vec4F getEyePosition(float faceWidth, float faceHeight, float facePixelX, float facePixelY)
    {
        float normalizedWidth = faceWidth / cameraPreviewWidth;
        float normalizedHeight = faceHeight / cameraPreviewHeight;
        float projectedX = calcProjectedX(facePixelX);
        float projectedY = calcProjectedY(facePixelY);
        if (calibrator != null)
        {
            calibrator.submitSample(normalizedWidth, normalizedHeight, projectedX, projectedY);
            calibrator.calibrate();
            if (calibrator.done())
            {
                calibrator = null;
            }
        }

        float eyeZWidth = eyeSensorCalibration.normWidthCalibration / normalizedWidth;
        float eyeZHeight = eyeSensorCalibration.normHeightCalibration / normalizedHeight;
        float eyeZ = (eyeZWidth + eyeZHeight) / 2;
        float eyeX = -(projectedX * eyeZ + eyeSensorCalibration.offsetX);
        float eyeY = -(projectedY * eyeZ + eyeSensorCalibration.offsetY);
        return new Vec4F(eyeX, eyeY, eyeZ, 1);
    }

    private float calcNormX(float faceCameraPixelX)
    {
        return (faceCameraPixelX - cameraPreviewWidth / 2) / (cameraPreviewWidth / 2);
    }

    private float calcNormY(float faceCameraPixelY)
    {
        return (faceCameraPixelY - cameraPreviewHeight / 2) / (cameraPreviewHeight / 2);
    }

    private float calcProjectedX(float facePixelX)
    {
        return calcNormX(facePixelX) * tanFOVX;
    }

    private float calcProjectedY(float facePixelY)
    {
        return calcNormY(facePixelY) * tanFOVY;
    }

    public boolean isCalibrating()
    {
        return calibrator != null;
    }

    public EyeSensorCalibration getCalibration()
    {
        return eyeSensorCalibration;
    }
}
