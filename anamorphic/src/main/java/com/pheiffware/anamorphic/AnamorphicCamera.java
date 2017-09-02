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
    //The view matrix representing the current view.  This is always a translation.
    private final Matrix4 viewMatrix;

    private final AnamorphicProjection projection;

    AnamorphicCamera(float screenHeight, float zNear, float zFar)
    {
        viewMatrix = Matrix4.newIdentity();
        projection = new AnamorphicProjection(screenHeight, zNear, zFar);
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
        projection.setScreenCenter(-x, -y, -z);
    }

    public void setAspect(float aspect)
    {
        projection.setAspect(aspect);
    }

    public Matrix4 getProjectionMatrix()
    {
        return projection.getProjectionMatrix();
    }

    public Matrix4 getViewMatrix()
    {
        return viewMatrix;
    }

}
