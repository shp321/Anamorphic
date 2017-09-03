package com.pheiffware.anamorphic;

import android.util.Log;

import com.pheiffware.lib.graphics.Vec4F;
import com.pheiffware.lib.graphics.projection.FieldOfViewProjection;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Steve on 9/2/2017.
 */

public class EyeReader
{
    private final Calibration calibration;
    private Calibrator calibrator;

    public EyeReader(float cameraPreviewWidth, float cameraPreviewHeight, float cameraHorizontalFOV, float cameraVerticalFOV)
    {
        calibration = new Calibration(cameraPreviewWidth, cameraPreviewHeight, cameraHorizontalFOV, cameraVerticalFOV);
    }

    /**
     * @param calibrationZ distance in Screen Coordinates which eye should be from screen during calibrator process.
     */
    public void startCalibration(int desiredNumSamples, float calibrationX, float calibrationY, float calibrationZ)
    {
        calibrator = new Calibrator(desiredNumSamples, calibrationX, calibrationY, calibrationZ, calibration);
    }

    public Vec4F findEye(float faceWidth, float faceHeight, float facePixelX, float facePixelY, float eulerY, float eulerZ)
    {
        if (calibrator != null)
        {
            calibrator.sample(faceWidth, faceHeight, facePixelX, facePixelY);

            Log.i("Face", "Calibrator W = " + calibration.normWidthCalibration);
            Log.i("Face", "Calibrator H = " + calibration.normHeightCalibration);
            Log.i("Face", "Calibrator X = " + calibration.offsetX);
            Log.i("Face", "Calibrator Y = " + calibration.offsetY);
            if (calibrator.done())
            {
                calibrator = null;
            }
        }
        return calibration.calcEye(faceWidth, faceHeight, facePixelX, facePixelY);
    }

    private static class Calibration
    {
        private final float cameraPreviewWidth;
        private final float cameraPreviewHeight;
        private final float cameraHorizontalFOV;
        private final float cameraVerticalFOV;

        float normWidthCalibration;
        float normHeightCalibration;
        float offsetX;
        float offsetY;

        public Calibration(float cameraPreviewWidth, float cameraPreviewHeight, float cameraHorizontalFOV, float cameraVerticalFOV)
        {
            this.cameraPreviewWidth = cameraPreviewWidth;
            this.cameraPreviewHeight = cameraPreviewHeight;
            this.cameraHorizontalFOV = cameraHorizontalFOV;
            this.cameraVerticalFOV = cameraVerticalFOV;
        }

        Vec4F calcEye(float faceWidth, float faceHeight, float facePixelX, float facePixelY)
        {
            float normalizedWidth = faceWidth / cameraPreviewWidth;
            float normalizedHeight = faceHeight / cameraPreviewHeight;
            float eyeZWidth = normWidthCalibration / normalizedWidth;
            float eyeZHeight = normHeightCalibration / normalizedHeight;
            float eyeZ = (eyeZWidth + eyeZHeight) / 2;
            float eyeX = -(calcProjectedX(facePixelX) * eyeZ + offsetX);
            float eyeY = -(calcProjectedY(facePixelY) * eyeZ + offsetY);
            return new Vec4F(eyeX, eyeY, eyeZ, 1);
        }

        final float calcNormX(float faceCameraPixelX)
        {
            return (faceCameraPixelX - cameraPreviewWidth / 2) / (cameraPreviewWidth / 2);
        }

        final float calcNormY(float faceCameraPixelY)
        {
            return (faceCameraPixelY - cameraPreviewHeight / 2) / (cameraPreviewHeight / 2);
        }

        final float calcProjectedX(float facePixelX)
        {
            return calcNormX(facePixelX) / FieldOfViewProjection.fovToScreenScaleFactor(cameraHorizontalFOV);
        }

        final float calcProjectedY(float facePixelY)
        {
            return calcNormY(facePixelY) / FieldOfViewProjection.fovToScreenScaleFactor(cameraVerticalFOV);
        }

    }

    private static class Calibrator
    {
        private final int desiredNumSamples;
        private final float calibrationX;
        private final float calibrationY;
        private final float calibrationZ;

        private final List<Float> faceWidths = new LinkedList<>();
        private final List<Float> faceHeights = new LinkedList<>();
        private final List<Float> faceXs = new LinkedList<>();
        private final List<Float> faceYs = new LinkedList<>();
        private final Calibration calibration;

        public Calibrator(int desiredNumSamples, float calibrationX, float calibrationY, float calibrationZ, Calibration calibration)
        {
            this.desiredNumSamples = desiredNumSamples;
            this.calibrationX = calibrationX;
            this.calibrationY = calibrationY;
            this.calibrationZ = calibrationZ;
            this.calibration = calibration;
        }

        public void sample(float faceWidth, float faceHeight, float facePixelX, float facePixelY)
        {
            faceWidths.add(faceWidth);
            faceHeights.add(faceHeight);
            faceXs.add(facePixelX);
            faceYs.add(facePixelY);
            calibrate();
        }


        void calibrate()
        {
            int numSamples = faceWidths.size();
            float averageNormWidth = 0;
            float averageNormHeight = 0;
            float averageX = 0;
            float averageY = 0;
            for (int i = 0; i < numSamples; i++)
            {
                averageNormWidth += faceWidths.get(i);
                averageNormHeight += faceHeights.get(i);
                averageX += calibration.calcProjectedX(faceXs.get(i)) * calibrationZ;
                averageY += calibration.calcProjectedY(faceYs.get(i)) * calibrationZ;
            }
            averageNormWidth /= numSamples;
            averageNormHeight /= numSamples;
            averageNormWidth /= calibration.cameraPreviewWidth;
            averageNormHeight /= calibration.cameraPreviewHeight;

            calibration.normWidthCalibration = averageNormWidth * calibrationZ;
            calibration.normHeightCalibration = averageNormHeight * calibrationZ;

            averageX /= numSamples;
            averageY /= numSamples;
            calibration.offsetX = -calibrationX - averageX;
            calibration.offsetY = -calibrationY - averageY;
        }

        int numSamples()
        {
            return faceWidths.size();
        }

        boolean done()
        {
            return numSamples() >= desiredNumSamples;
        }

    }

}
