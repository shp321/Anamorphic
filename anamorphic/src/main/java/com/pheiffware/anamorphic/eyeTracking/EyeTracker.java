package com.pheiffware.anamorphic.eyeTracking;

import com.pheiffware.lib.and.input.OrientationTracker;
import com.pheiffware.lib.graphics.Matrix4;
import com.pheiffware.lib.graphics.Vec4F;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Tracks the location of the user's eye relative to the center of the screen in 3-dimensional space.  The position is tracked in Screen Coordinates (see AnamorphicCamera).
 * Tracking uses a combination of the camera (for absolute position) and the orientation sensor (for quick, relative, updates and stabilization)
 * Created by Steve on 9/2/2017.
 */

public class EyeTracker
{
    private final float maxSampleAge;
    private final LinkedList<Vec4F> eyeSamples = new LinkedList<>();
    private final LinkedList<Long> sampleTimeStamps = new LinkedList<>();
    private OrientationTracker orientationTracker = new OrientationTracker(true);

    public EyeTracker(float maxSampleAge)
    {
        this.maxSampleAge = maxSampleAge;
    }

    public void addEye(Vec4F eye)
    {
        sampleTimeStamps.add(null);
        eyeSamples.add(eye.copy());

        //Each sample gets a time stamp when the next sample is added.  A sample without a time stamp will never expire.
        if (sampleTimeStamps.size() > 1)
        {
            sampleTimeStamps.set(sampleTimeStamps.size() - 2, System.nanoTime());
        }
    }

    public void zeroOrientation()
    {
        Matrix4 orientationMatrix = orientationTracker.getCurrentOrientation();
        for (Vec4F eyeSample : eyeSamples)
        {
            eyeSample.transformBy(orientationMatrix);
        }
        orientationTracker.zeroOrientationMatrix();
    }

    public Vec4F getEye()
    {
        if (eyeSamples.size() == 0)
        {
            return null;
        }
        Matrix4 orientationMatrix = orientationTracker.getCurrentOrientation();
        long now = System.nanoTime();
        Vec4F totalEye = new Vec4F(1);
        float totalWeight = 0;
        Iterator<Vec4F> eyeIter = eyeSamples.iterator();
        Iterator<Long> eyeTimeIter = sampleTimeStamps.iterator();
        while (eyeIter.hasNext())
        {
            Vec4F eye = eyeIter.next();
            Long sampleTimeStamp = eyeTimeIter.next();

            float ageOfSample = getSampleAge(now, sampleTimeStamp);
            if (ageOfSample > maxSampleAge)
            {
                eyeIter.remove();
                eyeTimeIter.remove();
            }
            else
            {
                float weight = 1.0f - Math.min(1.0f, ageOfSample / maxSampleAge);
                totalWeight += weight;
                Vec4F eyeCopy = eye.copy();
                eyeCopy.transformBy(orientationMatrix);
                totalEye.x(totalEye.x() + eyeCopy.x() * weight);
                totalEye.y(totalEye.y() + eyeCopy.y() * weight);
                totalEye.z(totalEye.z() + eyeCopy.z() * weight);
            }
        }
        if (totalWeight == 0f)
        {
            totalEye = eyeSamples.getFirst().copy();
            totalEye.transformBy(orientationMatrix);
        }
        else
        {
            totalEye.scaleBy(1.0f / totalWeight);
        }
        return totalEye;

    }


    private float getSampleAge(long now, Long sampleTimeStamp)
    {
        if (sampleTimeStamp == null)
        {
            return 0;
        }
        return (float) ((now - sampleTimeStamp) / 1000000000.0);
    }

    public void onOrientationSensorChanged(float[] sensorEventValues)
    {
        orientationTracker.onSensorChanged(sensorEventValues);
    }
}
