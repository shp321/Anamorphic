package com.pheiffware.anamorphic;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.opengl.GLES20;
import android.util.Log;

import com.pheiffware.anamorphic.eyeTracking.EyeSensor;
import com.pheiffware.anamorphic.eyeTracking.EyeSensorCalibration;
import com.pheiffware.anamorphic.eyeTracking.EyeTracker;
import com.pheiffware.lib.AssetLoader;
import com.pheiffware.lib.and.graphics.AndGraphicsUtils;
import com.pheiffware.lib.and.gui.graphics.openGL.GameRenderer;
import com.pheiffware.lib.and.gui.graphics.openGL.SystemInfo;
import com.pheiffware.lib.and.input.OrientationTracker;
import com.pheiffware.lib.and.input.TouchAnalyzer;
import com.pheiffware.lib.geometry.collada.Collada;
import com.pheiffware.lib.geometry.collada.ColladaFactory;
import com.pheiffware.lib.geometry.collada.ColladaObject3D;
import com.pheiffware.lib.graphics.GraphicsException;
import com.pheiffware.lib.graphics.Matrix4;
import com.pheiffware.lib.graphics.Mesh;
import com.pheiffware.lib.graphics.Vec4F;
import com.pheiffware.lib.graphics.managed.GLCache;
import com.pheiffware.lib.graphics.managed.engine.MeshDataManager;
import com.pheiffware.lib.graphics.managed.engine.MeshHandle;
import com.pheiffware.lib.graphics.managed.engine.ObjectHandle;
import com.pheiffware.lib.graphics.managed.engine.renderers.CubeDepthRenderer;
import com.pheiffware.lib.graphics.managed.frameBuffer.FrameBuffer;
import com.pheiffware.lib.graphics.managed.light.HoloLighting;
import com.pheiffware.lib.graphics.managed.light.Lighting;
import com.pheiffware.lib.graphics.managed.program.GraphicsConfig;
import com.pheiffware.lib.graphics.managed.program.RenderProperty;
import com.pheiffware.lib.graphics.managed.program.RenderPropertyValue;
import com.pheiffware.lib.graphics.managed.program.VertexAttribute;
import com.pheiffware.lib.graphics.managed.techniques.Std3DTechnique;
import com.pheiffware.lib.graphics.managed.techniques.Tech2D.Std2DTechnique;
import com.pheiffware.lib.graphics.managed.texture.TextureCubeMap;
import com.pheiffware.lib.graphics.utils.MeshGenUtils;
import com.pheiffware.lib.graphics.utils.PheiffGLUtils;
import com.pheiffware.lib.utils.dom.XMLParseException;

import java.io.IOException;
import java.util.EnumMap;

/**
 * Created by Steve on 9/2/2017.
 */

class AnamorphicRenderer extends GameRenderer
{
    private static final int numMonkeys = 30;
    private static final float CALIBRATION_X = 0.25f;
    private static final float CALIBRATION_Y = 0.5f;
    private static final float CALIBRATION_Z = 4.0f;
    private Std2DTechnique color2DTechnique;
    private Std3DTechnique color3DTechnique;
    private HoloLighting lighting;
    private TextureCubeMap[] cubeDepthTextures;
    private MeshDataManager manager;
    private AnamorphicCamera anamorphicCamera;
    //Represents the position of the eye relative to surface of the direct center of the screen if screen is flat.
    private final Vec4F absEyePosition = new Vec4F(0, 0, 2.7f, 1);
    //private final Vec4F cameraEyePosition = new Vec4F(0, 0, 4, 1);

    private MeshHandle[] monkeyHandles;
    private ObjectHandle monkeyGroupHandle;
    private MeshHandle screenHandle;
    private MeshHandle calibrationHandle;
    private CubeDepthRenderer cubeDepthRenderer;
    private OrientationTracker orientationTracker;
    private EyeSensor eyeSensor;
    private EyeSensorCalibration eyeSensorCalibration;
    private EyeTracker eyeTracker;


    public AnamorphicRenderer(int cameraPreviewWidth, int cameraPreviewHeight, float fovX, float fovY, EyeSensorCalibration eyeSensorCalibration)
    {
        super(AndGraphicsUtils.GL_VERSION_30, AndGraphicsUtils.GL_VERSION_30, "shaders");
        if (eyeSensorCalibration == null)
        {
            eyeSensorCalibration = new EyeSensorCalibration();
            eyeSensor = new EyeSensor(eyeSensorCalibration, cameraPreviewWidth, cameraPreviewHeight, fovX, fovY);
            calibrateEyeSensor();
        }
        else
        {
            eyeSensor = new EyeSensor(eyeSensorCalibration, cameraPreviewWidth, cameraPreviewHeight, fovX, fovY);
        }
    }

    @Override
    protected void onSurfaceCreated(AssetLoader al, GLCache glCache, SystemInfo systemInfo) throws GraphicsException
    {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        anamorphicCamera = new AnamorphicCamera(2.0f, -1.5f, 25.0f);
        eyeTracker = new EyeTracker(0.2f);
        PheiffGLUtils.enableAlphaTransparency();
        orientationTracker = new OrientationTracker(true);
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

//            lighting = new HoloLighting(new float[]{0.2f, 0.2f, 0.2f, 1.0f}, new float[]{1, 0.5f, 2, 1}, new float[]{0.7f, 0.7f, 0.7f, 1.0f}, new boolean[]{true});
        lighting = new HoloLighting(new float[]{0.2f, 0.2f, 0.2f, 1.0f}, new float[]{0, 0, 2.7f, 1}, new float[]{0.7f, 0.7f, 0.7f, 1.0f}, new boolean[]{true});
        //lighting.setCastsCubeShadow(0, 1);
        cubeDepthTextures = new TextureCubeMap[Lighting.numLightsSupported];
        cubeDepthTextures[0] = glCache.buildCubeDepthTex(1024, 1024).build();
        cubeDepthRenderer = new CubeDepthRenderer(glCache, 0.1f, 100.0f);
        PheiffGLUtils.enableAlphaTransparency();

        color3DTechnique = glCache.buildTechnique(Std3DTechnique.class, GraphicsConfig.TEXTURED_MATERIAL, false);
        color2DTechnique = glCache.buildTechnique(Std2DTechnique.class,
                GraphicsConfig.TEXTURED_2D, false,
                GraphicsConfig.COLOR_VERTEX_2D, true);

        ColladaFactory colladaFactory = new ColladaFactory();
        try
        {
            Collada collada = colladaFactory.loadCollada(al, "meshes/test_render.dae");

            //Lookup object from loaded file by "name" (what user named it in editing tool)
            ColladaObject3D monkey = collada.objects.get("Monkey");

            //From a given object get all meshes which should be rendered with the given material (in this case there is only one mesh which uses the single material defined in the file).
            Mesh mesh = monkey.getMesh(0);

            manager = new MeshDataManager();
            monkeyHandles = new MeshHandle[numMonkeys];
            monkeyHandles[0] = manager.addStaticMesh(
                    mesh,
                    color3DTechnique,
                    new RenderPropertyValue[]
                            {
                                    new RenderPropertyValue(RenderProperty.MAT_COLOR, new float[]{0f, 1f, 1f, 1f}),
                                    new RenderPropertyValue(RenderProperty.SPEC_MAT_COLOR, new float[]{1f, 1f, 1f, 1f}),
                                    new RenderPropertyValue(RenderProperty.SHININESS, 100f)
                            });
            for (int i = 1; i < numMonkeys; i++)
            {
                monkeyHandles[i] = monkeyHandles[0].copy();
            }
            monkeyGroupHandle = new ObjectHandle();
            monkeyGroupHandle.setMeshHandles(monkeyHandles);
            cubeDepthRenderer.add(monkeyGroupHandle);
            EnumMap<VertexAttribute, float[]> data = new EnumMap<>(VertexAttribute.class);
            data.put(VertexAttribute.POSITION4, MeshGenUtils.genSingleQuadPositionData(0, 0, 0, 2f, VertexAttribute.POSITION4));
            data.put(VertexAttribute.NORMAL3, new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1});
            Mesh screenQuad = new Mesh(6, data, MeshGenUtils.genSingleQuadIndexData());
            Mesh calibrationQuad = MeshGenUtils.genSingleQuadMeshTexColor(0, 0, -1, 0.01f, VertexAttribute.POSITION4, new float[]{0, 0, 0, 0.5f});

            screenHandle = manager.addStaticMesh(screenQuad,
                    color3DTechnique,
                    new RenderPropertyValue[]
                            {
                                    new RenderPropertyValue(RenderProperty.MAT_COLOR, new float[]{0.5f, 0.5f, 0.5f, 0.155f}),
                                    new RenderPropertyValue(RenderProperty.SPEC_MAT_COLOR, new float[]{1f, 1f, 1f, 0.5f}),
                                    new RenderPropertyValue(RenderProperty.SHININESS, 5f)
                            });

            screenHandle.setProperty(RenderProperty.MODEL_MATRIX, Matrix4.newIdentity());

            calibrationHandle = manager.addStaticMesh(
                    calibrationQuad,
                    color2DTechnique,
                    new RenderPropertyValue[]{new RenderPropertyValue(RenderProperty.MODEL_MATRIX, Matrix4.newIdentity())});

            manager.packAndTransfer();
            for (int i = 0; i < numMonkeys; i++)
            {
                Matrix4 transform = Matrix4.newTranslation(0.0f, 0.0f, 0.5f - i * 1f);
                transform.scaleBy(0.3f, 0.3f, 0.3f);
                monkeyHandles[i].setProperty(RenderProperty.MODEL_MATRIX, transform);
            }
        }
        catch (IOException | XMLParseException e)
        {
            throw new GraphicsException(e);
        }
    }

    @Override
    public void onDrawFrame() throws GraphicsException
    {


//        Vec4F eye = absEyePosition.copy();
//        eye.transformBy(orientationMatrix);

        Vec4F eye = eyeTracker.getEye();
        if (eye == null)
        {
            return;
        }

        anamorphicCamera.setPosition(eye.x(), eye.y(), eye.z());

        //Render shadows
        //renderShadowDepth(orientationMatrix);


        //Bind main frame buffer
        FrameBuffer.main.bind(0, 0, getSurfaceWidth(), getSurfaceHeight());
        GLES20.glViewport(0, 0, getSurfaceWidth(), getSurfaceHeight());
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glClearDepthf(1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        color3DTechnique.setProperty(RenderProperty.PROJECTION_MATRIX, anamorphicCamera.getProjectionMatrix());
        color3DTechnique.setProperty(RenderProperty.VIEW_MATRIX, anamorphicCamera.getViewMatrix());
        color3DTechnique.setProperty(RenderProperty.LIGHTING, lighting);
        color3DTechnique.setProperty(RenderProperty.DEPTH_Z_CONST, cubeDepthRenderer.getDepthZConst());
        color3DTechnique.setProperty(RenderProperty.DEPTH_Z_FACTOR, cubeDepthRenderer.getDepthZFactor());
        color3DTechnique.setProperty(RenderProperty.CUBE_DEPTH_TEXTURES, cubeDepthTextures);

        color3DTechnique.applyConstantProperties();
        for (int i = 0; i < numMonkeys; i++)
        {
            monkeyHandles[i].drawTriangles();
        }
        screenHandle.drawTriangles();

        color2DTechnique.setProperty(RenderProperty.PROJECTION_MATRIX, Matrix4.newOrtho2D(getSurfaceWidth() / (float) getSurfaceHeight()));
        color2DTechnique.setProperty(RenderProperty.VIEW_MATRIX, Matrix4.newIdentity());
        calibrationHandle.setProperty(RenderProperty.MODEL_MATRIX, Matrix4.newTranslation(0.25f, 0.5f, 0));
        calibrationHandle.drawTriangles();
        GLES20.glFinish();
    }

    private void renderShadowDepth(Matrix4 orientationMatrix)
    {
        Vec4F positions = lighting.getPositions().copy();
        positions.transformByAll(orientationMatrix);
        positions.setIndex(0);
        cubeDepthRenderer.render(positions.x(), positions.y(), positions.z(), cubeDepthTextures[0]);
    }

    @Override
    public void onSurfaceResize(int width, int height)
    {
        super.onSurfaceResize(width, height);
        anamorphicCamera.setAspect(width / (float) height);
    }

    @Override
    public void onTouchTransformEvent(TouchAnalyzer.TouchTransformEvent event)
    {
        if (event.numPointers == 1)
        {
            //Scale distance of eye from the screen
            absEyePosition.z((float) (absEyePosition.z() + event.transform.translation.x / 100.0f));
        }
    }

    @Override
    protected void onSensorChanged(int type, float[] values, long timestamp)
    {
        switch (type)
        {
            case Sensor.TYPE_ROTATION_VECTOR:
                orientationTracker.onSensorChanged(values);
                eyeTracker.onOrientationSensorChanged(values);
                break;
        }
    }

    public void faceMissing()
    {
        Log.i("Face", "Missing");
    }

    public void updateFace(float width, float height, float eulerY, float eulerZ, PointF position, PointF leftEyePosition)
    {
        eyeTracker.zeroOrientation();
        Vec4F eye = eyeSensor.calcEye(width, height, position.x, position.y); //, eulerY, eulerZ);
        Log.i("Face", "(" + eye.x() + "," + eye.y() + "," + eye.z() + ")");
        eyeTracker.addEye(eye);
    }

    public void calibrateEyeSensor()
    {
        //TODO: Z-calibration should be in real units and converted, based on physical screen size to Screen Coordinates
        eyeSensor.calibrate(50, CALIBRATION_X, CALIBRATION_Y, CALIBRATION_Z);
    }
}