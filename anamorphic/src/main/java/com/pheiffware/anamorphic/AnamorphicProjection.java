package com.pheiffware.anamorphic;

import com.pheiffware.lib.graphics.projection.Projection;

/**
 * Projects points onto the given screen, which may not be centered in the user's view.  In practice this is used as part of the AnamorphicCamera:
 * 1. first offset points (so they move with the physical screen)
 * 2. project points
 * This gives the illusion of a universe attached to the screen itself.
 * <p>
 * Created by Steve on 9/2/2017.
 */

public class AnamorphicProjection extends Projection
{
    //The height of the screen in screen coordinates
    private final float screenHeight;

    //Distance of closest z clipping plane (in screen coordinates).  This can be negative to put the near clipping plane in front of the screen (allowing objects to be projected above the screen).
    private final float near;

    //Distance of farthest z clipping plane (in screen coordinates)
    private final float far;

    private float screenCenterX = 0, screenCenterY = 0, screenCenterZ = 0;

    private float aspect = 1;

    /**
     * Sets up given projection, with screenCenter at (0,0,0) and aspect of 1.
     *
     * @param screenHeight
     * @param near
     * @param far
     */
    public AnamorphicProjection(float screenHeight, float near, float far)
    {
        this.screenHeight = screenHeight;
        this.near = near;
        this.far = far;
        updateProjection();
    }

    public void setScreenCenter(float screenCenterX, float screenCenterY, float screenCenterZ)
    {
        this.screenCenterX = screenCenterX;
        this.screenCenterY = screenCenterY;
        this.screenCenterZ = screenCenterZ;
        updateProjection();
    }

    public void setAspect(float aspect)
    {
        this.aspect = aspect;
        updateProjection();
    }

    private void updateProjection()
    {
        float screenWidth = screenHeight * aspect;

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
        float xOffset = projectedCenterX / halfProjectedWidth; // == 2 * screenCenterX / screenWidth

        //Scale by inverse of 1/2 height
        float yScale = 1 / halfProjectedHeight;

        //Offset is positive, because it will be multiplied by z, but then divided by -z
        float yOffset = projectedCenterY / halfProjectedHeight;  // == 2 * screenCenterY / screenHeight

        setProjection(xScale, yScale, xOffset, yOffset, near - screenCenterZ, far - screenCenterZ);
    }

}
