package com.pheiffware.anamorphic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.pheiffware.lib.and.gui.graphics.openGL.BaseGameFragment;
import com.pheiffware.lib.and.gui.graphics.openGL.GameView;
import com.pheiffware.lib.graphics.FilterQuality;

import java.io.IOException;
import java.util.List;

/**
 * Created by Steve on 9/2/2017.
 */

public class AnamorphicFragment extends BaseGameFragment
{
    private static final int CAMERA_PREVIEW_HEIGHT = 320;
    private static final int CAMERA_PREVIEW_WIDTH = 240;
    //    private static final int CAMERA_PREVIEW_HEIGHT = 400;
//    private static final int CAMERA_PREVIEW_WIDTH = (int) (CAMERA_PREVIEW_HEIGHT * 0.303848512);

    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 30465;
    FaceDetector faceDetector;
    CameraSource cameraSource;
    private boolean alreadyAskedForCamera;
    private AnamorphicRenderer renderer;

    @Override
    public GameView onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        alreadyAskedForCamera = false;

        faceDetector = new FaceDetector.Builder(getContext()).
                setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMinFaceSize(0.01f)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setTrackingEnabled(true)
//                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
//                .setMode(FaceDetector.FAST_MODE)
                .build();
        faceDetector.setProcessor(
                new LargestFaceFocusingProcessor.Builder(faceDetector, new FaceTracker())
                        .build());


        if (!faceDetector.isOperational())
        {
            //TODO: Proper error
            Log.e("FACE", "Face detector dependencies are not yet available.");
            getActivity().finish();
        }
        cameraSource = new CameraSource.Builder(getContext(), faceDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30)
                .build();
        this.renderer = new AnamorphicRenderer(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT);
        return new GameView(getContext(), renderer, FilterQuality.MEDIUM, true, true);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        cameraSource.release();
        faceDetector.release();
        renderer = null;
    }

    @Override
    public void onPause()
    {
        Log.i("Permissions", "Pause");
        super.onPause();
        cameraSource.stop();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i("Permissions", "Resume");
        startCamera();

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
//            Camera camera = Camera.open(CameraSource.CAMERA_FACING_FRONT);
//            System.out.println(camera.getParameters().getHorizontalViewAngle());
//            System.out.println(camera.getParameters().getVerticalViewAngle());
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


    private class FaceTracker extends Tracker<Face>
    {
        float averageWidth = 0;
        int numSamples = 0;

        private void sendFaceUpdate(final boolean newFace, Face face)
        {
            if (face != null)
            {
                final PointF leftEyePosition;
                Landmark leftEye = getLandmark(face, Landmark.LEFT_EYE);
                if (leftEye != null)
                {
                    leftEyePosition = leftEye.getPosition();
                }
                else
                {
                    leftEyePosition = null;
                }

                final float width = face.getWidth();
                final float height = face.getHeight();
                final float eulerY = face.getEulerY();
                final float eulerZ = face.getEulerZ();
                final PointF position = face.getPosition();
                getView().queueEvent(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        renderer.updateFace(width, height, eulerY, eulerZ, position, leftEyePosition);
                    }
                });
            }

        }

        @Override
        public void onNewItem(int faceId, final Face face)
        {
            //Log.i("Face", "New findEye: " + faceId);
            sendFaceUpdate(true, face);
        }

        private Landmark getLandmark(Face face, int type)
        {
            List<Landmark> landmarks = face.getLandmarks();
            for (Landmark landmark : landmarks)
            {
                if (landmark.getType() == type)
                {
                    return landmark;
                }
            }

            return null;
        }

        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults,
                             Face face)
        {
            sendFaceUpdate(false, face);


//            Log.i("Face", "Left Eye: " + leftEyePosition + " Right Eye: " + rightEyePosition);

//            Log.i("Face", "Tracking findEye: RY = " + findEye.getEulerY() + " + RZ = " + findEye.getEulerZ());
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults)
        {
            //Log.i("Face", "Face missing");
            getView().queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    renderer.faceMissing();
                }
            });
        }

        @Override
        public void onDone()
        {
            //Log.i("Face", "Done");
        }
    }

    public void startCalibration()
    {
        renderer.startCalibration();
    }
}
