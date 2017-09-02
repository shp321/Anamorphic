package com.pheiffware.anamorphic;

import com.pheiffware.lib.graphics.Matrix4;

/**
 * A camera which produces anamorphic projections.  When using this camera, all object models, lights and camera position are expressed in "screen coordinates".
 * Screen Coordinates:
 * X-Axis: points in the x direction in the plane of the physical screen, according to the device's display's notion of x.  Right is positive.
 * Y-Axis: points in the y direction in the plane of the physical screen, according to the device's display's notion of y.  Up is positive (towards top of screen).
 * Z-Axis points in the direction normal to screen.  Into ("behind") the screen is negative, out of the screen is positive.
 * Scale: The scale of all units is based on the height of the display.
 * The top of the display will have a y-value of +height/2.  This will result in the right side of the display having x-value of +width/2.
 * Z coordinates have the same scale.
 * <p>
 * If height is set to 2.0, then one can think of all coordinates being specified in units of "one half screen heights".
 * Created by Steve on 9/2/2017.
 */

public class AnamorphicCamera
{
    //The width of the screen in screen coordinates
    private final float screenWidth;
    //The height of the screen in screen coordinates
    private final float screenHeight;

    //Distance of closest z clipping plane (in screen coordinates).  This can be negative to put the near clipping plane in front of the screen (allowing objects to be projected above the screen).
    private final float zNear;

    //Distance of farthest z clipping plane (in screen coordinates)
    private final float zFar;

    //The view matrix representing the current view.  This is always a translation.
    private final Matrix4 viewMatrix;

    //The projection matrix describing the screen's location
    private final Matrix4 projectionMatrix;


    AnamorphicCamera(float screenHeight, float aspect, float zNear, float zFar)
    {
        this.screenHeight = screenHeight;
        screenWidth = aspect * screenHeight;
        this.zNear = zNear;
        this.zFar = zFar;

        viewMatrix = Matrix4.newIdentity();
        projectionMatrix = Matrix4.newIdentity();
    }


    /**
     * Set the position of the camera in "screen coordinates".
     *
     * @param x
     * @param y
     * @param z
     */
    void setPosition(float x, float y, float z)
    {
        float screenCenterX = -x;
        float screenCenterY = -y;
        float screenCenterZ = -z;
        viewMatrix.setTranslate(screenCenterX, screenCenterY, screenCenterZ);

        //Where the center of the screen will be projected.  All projected (x,y) screen coordinates should be offset by this
        float projectedCenterX = screenCenterX / -screenCenterZ;
        float projectedCenterY = screenCenterY / -screenCenterZ;

        //Once screen coordinates are offset, they should be scaled based on projected screen dimensions
        //We want to scale coordinates to ranges y: [-1,1], x: [-aspect, aspect]
        //We use 1/2 projected width/height because to match the screen coordinate convention that (width/2,height/2,0) corresponds to the upper right corner of the screen
        float halfProjectedWidth = screenWidth / (2 * -screenCenterZ);
        float halfProjectedHeight = screenHeight / (2 * -screenCenterZ);

        //xProjected = (x/z - projectedCenterX) / halfProjectedWidth
        //xProjected = (x/z) / halfProjectedWidth - projectedCenterX / halfProjectedWidth
        //xProjected = (x/z) * xScale + xOffset

        //Scale by inverse of 1/2 width
        float xScale = 1 / halfProjectedWidth;

        //Offset is positive, because it will be multiplied by z, but then divided by -z
        float xOffset = projectedCenterX / halfProjectedWidth;

        //Scale by inverse of 1/2 height
        float yScale = 1 / halfProjectedHeight;

        //Offset is positive, because it will be multiplied by z, but then divided by -z
        float yOffset = projectedCenterY / halfProjectedHeight;

        float near = zNear + z;
        float far = zFar + z;
        float[] m = new float[16];
        //@formatter:off
                m[0] = xScale;  m[4] = 0;       m[8] =  xOffset;                        m[12] = 0;
                m[1] = 0;       m[5] = yScale;  m[9] =  yOffset;                        m[13] = 0;
                m[2] = 0;       m[6] = 0;       m[10] = -(far + near) / (far - near);   m[14] = -2 * far * near / (far - near);
                m[3] = 0;       m[7] = 0;       m[11] = -1;                             m[15] = 0;
                //@formatter:on
        projectionMatrix.set(m);
    }

    public Matrix4 getProjectionMatrix()
    {
        return projectionMatrix;
    }

    public Matrix4 getViewMatrix()
    {
        return viewMatrix;
    }
}
