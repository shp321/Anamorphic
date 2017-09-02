package com.pheiffware.anamorphic;

import com.pheiffware.lib.graphics.Matrix4;
import com.pheiffware.lib.graphics.Vec4F;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Steve on 9/2/2017.
 */

public class EyeBuffer
{
    private static final float MaxEyeTime = 0.4f;
    LinkedList<Vec4F> eyes = new LinkedList<>();
    LinkedList<Long> eyeTimes = new LinkedList<>();

    void addEye(Vec4F eye)
    {
        eyeTimes.add(System.nanoTime());
        eyes.add(eye.copy());
    }


    Vec4F getEye(Matrix4 orientationMatrix)
    {
        if (eyes.size() == 0)
        {
            return null;
        }
        long now = System.nanoTime();
        Vec4F totalEye = new Vec4F(1);
        float totalWeight = 0;
        Iterator<Vec4F> eyeIter = eyes.iterator();
        Iterator<Long> eyeTimeIter = eyeTimes.iterator();
        while (eyeIter.hasNext())
        {
            Vec4F eye = eyeIter.next();
            long eyeTime = eyeTimeIter.next();

            float timeDiff = getTimeDiff(now, eyeTime);
            if (timeDiff > MaxEyeTime && eyes.size() > 1)
            {
                eyeIter.remove();
                eyeTimeIter.remove();
            }
            else
            {
                float weight = 1.0f - Math.min(1.0f, timeDiff / MaxEyeTime);
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
            totalEye = eyes.getFirst().copy();
            totalEye.transformBy(orientationMatrix);
        }
        else
        {
            totalEye.scaleBy(1.0f / totalWeight);
        }
        return totalEye;

    }

    void updateEyes(Matrix4 orientationMatrix)
    {
        long now = System.nanoTime();
        Iterator<Vec4F> eyeIter = eyes.iterator();
        Iterator<Long> eyeTimeIter = eyeTimes.iterator();
        while (eyeIter.hasNext())
        {
            Vec4F eye = eyeIter.next();
            long eyeTime = eyeTimeIter.next();

            float timeDiff = getTimeDiff(now, eyeTime);

            if (timeDiff > MaxEyeTime && eyes.size() > 1)
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

    private float getTimeDiff(long now, long previous)
    {
        return (float) ((now - previous) / 1000000000.0);
    }
}
