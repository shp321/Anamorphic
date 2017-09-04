package com.pheiffware.anamorphic;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.pheiffware.anamorphic.eyeTracking.EyeSensorCalibration;
import com.pheiffware.lib.and.gui.graphics.openGL.BaseGameFragment;
import com.pheiffware.lib.and.gui.graphics.openGL.GameView;
import com.pheiffware.lib.and.input.CameraDisplayInfo;
import com.pheiffware.lib.and.input.CameraUtils;
import com.pheiffware.lib.graphics.FilterQuality;

import java.io.IOException;

/**
 * Created by Steve on 9/2/2017.
 */

public class AnamorphicFragment extends BaseGameFragment
{
    private static final int CAMERA_PREVIEW_HEIGHT = 400;
    private static final int speedMode = FaceDetector.ACCURATE_MODE;
    //private static final int speedMode = FaceDetector.FAST_MODE;
    private static final boolean useAutoFocus = false;

    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 30465;

    private EyeSensorCalibration calibration;
    FaceDetector faceDetector;
    CameraSource cameraSource;
    private boolean alreadyAskedForCamera;
    private AnamorphicRenderer renderer;

    public AnamorphicFragment()
    {
        super(new int[]{Sensor.TYPE_ROTATION_VECTOR}, new int[]{
                SensorManager.SENSOR_DELAY_FASTEST});
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getContext().getSharedPreferences(
                "anamorphicData", Context.MODE_PRIVATE);

        calibration = new EyeSensorCalibration(sharedPref);
    }

    @Override
    public GameView onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        alreadyAskedForCamera = false;
        faceDetector = new FaceDetector.Builder(getContext()).
                setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
//                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMinFaceSize(0.01f)
                .setTrackingEnabled(true)
                .setMode(speedMode)
                .build();
        faceDetector.setProcessor(
                new LargestFaceFocusingProcessor.Builder(faceDetector, new FaceTracker())
                        .build());

        if (!faceDetector.isOperational())
        {
            //TODO: Proper error handling
            Log.e("FACE", "Face detector dependencies are not yet available.");
            getActivity().finish();
        }
        try
        {

            //TODO: Switched width/height information, because we are in vertical mode.  Not clear how
            CameraDisplayInfo cameraInfo = CameraUtils.getCameraDisplayInfo(getContext(), CameraCharacteristics.LENS_FACING_FRONT);
            int cameraPreviewWidth = (int) (CAMERA_PREVIEW_HEIGHT / cameraInfo.pixelAspect);
            cameraSource = new CameraSource.Builder(getContext(), faceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedPreviewSize(cameraPreviewWidth, CAMERA_PREVIEW_HEIGHT)
                    .setAutoFocusEnabled(useAutoFocus)
                    .setRequestedFps(30)
                    .build();
            this.renderer = new AnamorphicRenderer(cameraPreviewWidth, CAMERA_PREVIEW_HEIGHT, cameraInfo.fovY, cameraInfo.fovX, calibration);
            return new GameView(getContext(), renderer, FilterQuality.MEDIUM, true);
        }
        catch (CameraAccessException e)
        {
            //TODO: Proper error handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i("Permissions", "Resume");
        startCamera();
    }

    @Override
    public void onPause()
    {
        Log.i("Permissions", "Pause");
        stopCamera();
        calibration = renderer.getEyeSensorCalibration();
        SharedPreferences sharedPref = getContext().getSharedPreferences(
                "anamorphicData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        calibration.write(editor);
        editor.apply();
        super.onPause();
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        cameraSource.release();
        faceDetector.release();
        renderer = null;
    }


    private void startCamera()
    {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            if (alreadyAskedForCamera)
            {
                //TODO: Proper error reporting
                Log.e("Permissions", "App cannot function without the camera!");
                getActivity().finish();
                return;
            }
            else
            {
                Log.i("Permissions", "Request permissions");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUEST_CODE);
                alreadyAskedForCamera = true;
                return;
            }
        }
        try
        {
            cameraSource.start();
        }
        catch (SecurityException e)
        {
            throw new RuntimeException("Permission was granted and immediately rejected", e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not access camera", e);
        }
    }

    private void stopCamera()
    {
        cameraSource.stop();
    }

    public void startCalibration()
    {
        renderer.calibrateEyeSensor();
    }


    private class FaceTracker extends Tracker<Face>
    {
        @Override
        public void onNewItem(int faceId, final Face face)
        {
            getView().queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    renderer.faceOnCamera();
                    renderer.faceUpdated(face);
                }
            });
        }

        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults,
                             final Face face)
        {
            getView().queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    renderer.faceUpdated(face);
                }
            });
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults)
        {
            getView().queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    renderer.faceOffCamera();
                }
            });
        }

        @Override
        public void onDone()
        {
        }
    }

}
