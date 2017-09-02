package com.pheiffware.anamorphic;

import com.pheiffware.lib.graphics.Matrix4;
import com.pheiffware.lib.graphics.Vec4F;

/**
 * Created by Steve on 9/2/2017.
 */

public class HoloCamera
{
    private static final float screenHeight = 1;
    Matrix4 viewMatrix;
    Matrix4 projectionMatrix;

    /**
     * @param eyeX
     * @param eyeY
     * @param eyeZ
     * @param aspect ratio of width to height
     */
    HoloCamera(float eyeX, float eyeY, float eyeZ, float aspect)
    {
        float near = 1.0f;
        float far = 100.0f;
        Vec4F screenCenter = new Vec4F(1);
        screenCenter.setX(-eyeX);
        screenCenter.setY(-eyeY);
        screenCenter.setZ(-eyeZ);
        screenCenter.setW(1);
        viewMatrix = Matrix4.newTranslation(screenCenter.x(), screenCenter.y(), screenCenter.z());

        float screenWidth = aspect * screenHeight;

        //Where the center of the screen will be projected.  All projected (x,y) screen coordinates should be offset by this
        float projectedCenterX = screenCenter.x() / -screenCenter.z();
        float projectedCenterY = screenCenter.y() / -screenCenter.z();

        //Once screen coordinates are offset, they should be scaled based on projected screen dimensions
        //We want to scale coordinates to ranges y: [-1,1], x: [-aspect, aspect]
        float halfProjectedWidth = screenWidth / (2 * -screenCenter.z());
        float halfProjectedHeight = screenHeight / (2 * -screenCenter.z());

        //xProjected = (x/z - projectedCenterX) / projectedWidth
        //xProjected = (x/z) / projectedWidth - projectedCenterX / projectedWidth
        //xProjected = (x/z) * xScale + xOffset

        //Scale by inverse of 1/2 width
        float xScale = 1 / halfProjectedWidth;

        //Offset is positive, because it will be multiplied by z, but then divided by -z
        float xOffset = projectedCenterX / halfProjectedWidth;

        //Scale by inverse of 1/2 height
        float yScale = 1 / halfProjectedHeight;

        //Offset is positive, because it will be multiplied by z, but then divided by -z
        float yOffset = projectedCenterY / halfProjectedHeight;

        float[] m = new float[16];
        //@formatter:off
                m[0] = xScale;  m[4] = 0;       m[8] =  xOffset;                        m[12] = 0;
                m[1] = 0;       m[5] = yScale;  m[9] =  yOffset;                        m[13] = 0;
                m[2] = 0;       m[6] = 0;       m[10] = -(far + near) / (far - near);   m[14] = -2 * far * near / (far - near);
                m[3] = 0;       m[7] = 0;       m[11] = -1;                             m[15] = 0;
                //@formatter:on
        projectionMatrix = Matrix4.newFromFloats(m);
    }

}
