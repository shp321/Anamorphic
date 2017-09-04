package com.pheiffware.anamorphic;

import com.pheiffware.lib.and.input.OrientationTracker;
import com.pheiffware.lib.graphics.Vec4F;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Tracks the location of the user's eye relative to the center of the screen in 3-dimensional space.  The position is tracked in Screen Coordinates (see AnamorphicCamera).
 * Tracking uses a combination of the camera (for absolute position) and the orientation sensor (for quick, relative, updates and stabilization)
 * Created by Steve on 9/2/2017.
 */

public class EyeTracker2
{
    private final float maxSampleAge;
    private final LinkedList<Vec4F> eyeSamples = new LinkedList<>();
    private final LinkedList<Long> sampleTimeStamps = new LinkedList<>();
    private OrientationTracker orientationTracker = new OrientationTracker(true);
    private Vec4F stableEye = null;

    public EyeTracker2(float maxSampleAge)
    {
        this.maxSampleAge = maxSampleAge;
    }

    void addEye(Vec4F eye)
    {
        sampleTimeStamps.add(null);
        eyeSamples.add(eye.copy());

        //Each sample gets a time stamp when the next sample is added.  A sample without a time stamp will never expire.
        if (sampleTimeStamps.size() > 1)
        {
            sampleTimeStamps.set(sampleTimeStamps.size() - 2, System.nanoTime());
        }
    }

    Vec4F getEye()
    {
        Vec4F averageEye = getAverageEye();
        if (averageEye == null)
        {
            return null;
        }
        if (stableEye == null)
        {
            stableEye = averageEye;
        }
        else
        {
            Vec4F diff = Vec4F.sub(stableEye, averageEye);
            if (diff.magnitude() > 1)
            {
                stableEye = averageEye;
                orientationTracker.zeroOrientationMatrix();
            }
        }

        Vec4F finalValue = stableEye.copy();
        finalValue.transformBy(orientationTracker.getCurrentOrientation());
        return finalValue;
    }

    Vec4F getAverageEye()
    {
        if (eyeSamples.size() == 0)
        {
            return null;
        }
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
                totalEye.addTo(eye);
            }
        }
        totalEye.scaleBy(1.0f / totalWeight);
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

    public void zeroOrientation()
    {
        //TODO: Remove after testing
    }
}
