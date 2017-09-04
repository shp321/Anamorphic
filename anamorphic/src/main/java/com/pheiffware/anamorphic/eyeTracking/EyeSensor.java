package com.pheiffware.anamorphic.eyeTracking;

import com.pheiffware.lib.graphics.Vec4F;

/**
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

    public EyeSensor(EyeSensorCalibration eyeSensorCalibration, float cameraPreviewWidth, float cameraPreviewHeight, float cameraFOVY, float cameraFOVX)
    {
        this.eyeSensorCalibration = eyeSensorCalibration;
        this.cameraPreviewWidth = cameraPreviewWidth;
        this.cameraPreviewHeight = cameraPreviewHeight;
        tanFOVX = (float) Math.tan(Math.toRadians(cameraFOVY / 2.0));
        tanFOVY = (float) Math.tan(Math.toRadians(cameraFOVX / 2.0));
    }

    /**
     * @param calibrationZ distance in Screen Coordinates which eye should be from screen during calibrator process.
     */
    public void calibrate(int desiredNumSamples, float calibrationX, float calibrationY, float calibrationZ)
    {
        calibrator = new EyeSensorCalibrator(eyeSensorCalibration, desiredNumSamples, calibrationX, calibrationY, calibrationZ);
    }

    public Vec4F calcEye(float faceWidth, float faceHeight, float facePixelX, float facePixelY)
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
}
