package com.pheiffware.anamorphic;

import com.pheiffware.lib.graphics.Matrix4;
import com.pheiffware.lib.graphics.Vec4F;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Steve on 9/2/2017.
 */

public class EyeTracker
{
    private static final float MaxEyeTime = 0.1f;
    private LinkedList<Vec4F> samples = new LinkedList<>();
    private LinkedList<Long> sampleTimeStamps = new LinkedList<>();

    void addEye(Vec4F eye)
    {
        sampleTimeStamps.add(null);
        samples.add(eye.copy());

        //Each sample gets a time stamp when the next sample is added.  A sample without a time stamp will never expire.
        if (sampleTimeStamps.size() > 1)
        {
            sampleTimeStamps.set(sampleTimeStamps.size() - 2, System.nanoTime());
        }
    }

    void updateEyes(Matrix4 orientationMatrix)
    {
        long now = System.nanoTime();
        Iterator<Vec4F> eyeIter = samples.iterator();
        Iterator<Long> eyeTimeIter = sampleTimeStamps.iterator();
        while (eyeIter.hasNext())
        {
            Vec4F eye = eyeIter.next();
            Long sampleTimeStamp = eyeTimeIter.next();

            float ageOfSample = getSampleAge(now, sampleTimeStamp);

            if (ageOfSample > MaxEyeTime)
            {
                eyeIter.remove();
                eyeTimeIter.remove();
            }
            else
            {
                eye.transformBy(orientationMatrix);
            }
        }
    }

    Vec4F getEye(Matrix4 orientationMatrix)
    {
        if (samples.size() == 0)
        {
            return null;
        }
        long now = System.nanoTime();
        Vec4F totalEye = new Vec4F(1);
        float totalWeight = 0;
        Iterator<Vec4F> eyeIter = samples.iterator();
        Iterator<Long> eyeTimeIter = sampleTimeStamps.iterator();
        while (eyeIter.hasNext())
        {
            Vec4F eye = eyeIter.next();
            Long sampleTimeStamp = eyeTimeIter.next();

            float ageOfSample = getSampleAge(now, sampleTimeStamp);
            if (ageOfSample > MaxEyeTime)
            {
                eyeIter.remove();
                eyeTimeIter.remove();
            }
            else
            {
                float weight = 1.0f - Math.min(1.0f, ageOfSample / MaxEyeTime);
                totalWeight += weight;
                Vec4F eyeCopy = eye.copy();
                eyeCopy.transformBy(orientationMatrix);
                totalEye.setX(totalEye.x() + eyeCopy.x() * weight);
                totalEye.setY(totalEye.y() + eyeCopy.y() * weight);
                totalEye.setZ(totalEye.z() + eyeCopy.z() * weight);
            }
        }
        if (totalWeight == 0f)
        {
            totalEye = samples.getFirst().copy();
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
}
