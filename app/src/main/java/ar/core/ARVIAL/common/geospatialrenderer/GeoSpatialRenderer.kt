//package ar.core.ARVIAL.common.geospatialrenderer
//
//import android.content.Context
//import android.opengl.GLSurfaceView
//import android.opengl.Matrix
//import android.util.Log
//import android.view.GestureDetector
//import android.view.MotionEvent
//import com.google.ar.core.*
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import ar.core.ARVIAL.common.helpers.DisplayRotationHelper
//import ar.core.ARVIAL.common.helpers.SnackbarHelper
//import ar.core.ARVIAL.common.helpers.TrackingStateHelper
//import ar.core.ARVIAL.common.samplerender.*
//import ar.core.ARVIAL.common.samplerender.Mesh
//import ar.core.ARVIAL.common.samplerender.Shader.BlendFactor.*
//import ar.core.ARVIAL.common.samplerender.arcore.BackgroundRenderer
//import ar.core.ARVIAL.common.samplerender.arcore.PlaneRenderer
//import ar.core.ARVIAL.geospatial.AnchorManager
//import ar.core.ARVIAL.geospatial.getLocationCoords
//import java.io.IOException
//import java.util.ArrayList
//import java.util.concurrent.atomic.AtomicBoolean
//import kotlin.collections.HashMap
//
//// Re-using your existing constants
//private const val TAG = "GeospatialActivity" // Renamed from original for clarity in this file
//private const val Z_NEAR = 0.1f
//private const val Z_FAR = 100.0f
//private const val MAXIMUM_ANCHORS = 10 // Used for UI feedback
//
//// State enums from your original code
//enum class GeospatialState {
//    UNINITIALIZED, UNSUPPORTED, EARTH_STATE_ERROR, PRETRACKING, LOCALIZING, LOCALIZING_FAILED, LOCALIZED
//}
//
//enum class AnchorType {
//    GEOSPATIAL, TERRAIN, ROOFTOP
//}
//
///**
// * Encapsulates all ARCore and OpenGL rendering logic previously found in GeospatialActivity.
// * This class implements `SampleRender.Renderer` and also `GestureDetector.OnGestureListener`
// * to handle taps for anchor placement.
// *
// * @param context The application context, used for loading assets and showing snackbars.
// * @param onStatusMessageChanged Callback to update the UI status text in Compose.
// * @param onGeospatialStateChanged Callback to update the GeospatialState in Compose.
// * @param onAnchorCountChanged Callback to update the anchor count for UI button visibility.
// */
//class GeospatialRenderer(
//    private val context: Context, // Application context
//    private val onStatusMessageChanged: (String) -> Unit,
//    private val onGeospatialStateChanged: (GeospatialState) -> Unit,
//    private val onAnchorCountChanged: (Int) -> Unit
//) : SampleRender.Renderer, GestureDetector.OnGestureListener {
//
//    // ARCore Session: Set externally by the hosting Activity/Composable
//    var session: Session? = null
//        set(value) {
//            field = value
//            value?.let {
//                if (::backgroundRenderer.isInitialized) {
//                    it.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
//                    hasSetTextureNames.set(true)
//                } else {
//                    // Defer setting texture names if backgroundRenderer not yet initialized
//                    hasSetTextureNames.set(false)
//                }
//                Log.d(TAG, "ARCore Session set in renderer.")
//            }
//        }
//
//    // ARCore Helpers
//    private lateinit var displayRotationHelper: DisplayRotationHelper
//    private lateinit var trackingStateHelper: TrackingStateHelper
//    private lateinit var messageSnackbarHelper: SnackbarHelper
//
//    // Rendering Objects (Initialized in onSurfaceCreated)
//    private lateinit var render: SampleRender
//    private lateinit var planeRenderer: PlaneRenderer
//    private lateinit var backgroundRenderer: BackgroundRenderer
//    private lateinit var virtualSceneFramebuffer: Framebuffer
//    private val hasSetTextureNames = AtomicBoolean(false) // Use AtomicBoolean for thread safety
//
//    // Virtual Objects
//    private lateinit var virtualObjectMesh: Mesh
//    private lateinit var geospatialAnchorVirtualObjectShader: Shader
//    private lateinit var terrainAnchorVirtualObjectShader: Shader
//
//    // Point Cloud
//    private lateinit var pointCloudVertexBuffer: VertexBuffer
//    private lateinit var pointCloudMesh: Mesh
//    private lateinit var pointCloudShader: Shader
//    private var lastPointCloudTimestamp: Long = 0
//
//    // Streetscape Geometry
//    private var isRenderStreetscapeGeometry = false
//    private lateinit var streetscapeGeometryTerrainShader: Shader
//    private lateinit var streetscapeGeometryBuildingShader: Shader
//    private val wallsColor = ArrayList<FloatArray>()
//    private val streetscapeGeometryToMeshes: MutableMap<StreetscapeGeometry, Mesh> = HashMap()
//
//    // Anchor Management (Your existing AnchorManager)
//    internal val anchorManager = AnchorManager() // Make internal for testing in Composable
//    private val anchorsLock = Any() // From your original code
//
//    // Temporary matrices (to avoid re-allocation per frame)
//    private val modelMatrix = FloatArray(16)
//    private val viewMatrix = FloatArray(16)
//    private val projectionMatrix = FloatArray(16)
//    private val modelViewMatrix = FloatArray(16)
//    private val modelViewProjectionMatrix = FloatArray(16)
//
//    // Tap Handling for anchor placement
//    private val singleTapLock = Any()
//    @Volatile private var queuedSingleTap: MotionEvent? = null
//
//    // Geospatial State for UI reporting
//    private var currentGeospatialState: GeospatialState = GeospatialState.UNINITIALIZED
//    // Anchor Type selected by UI
//    var currentAnchorType: AnchorType = AnchorType.GEOSPATIAL
//
//    private var lastStatusText: String? = null // To avoid redundant UI updates
//
//    init {
//        displayRotationHelper = DisplayRotationHelper(context)
//        trackingStateHelper = TrackingStateHelper(context)
//        messageSnackbarHelper = SnackbarHelper()
//
//        // Initialize wall colors (from your original code)
//        wallsColor.add(floatArrayOf(0.5f, 0.0f, 0.5f, 0.3f))
//        wallsColor.add(floatArrayOf(0.5f, 0.5f, 0.0f, 0.3f))
//        wallsColor.add(floatArrayOf(0.0f, 0.5f, 0.5f, 0.3f))
//    }
//
//    // --- SampleRender.Renderer Implementation (Copied from your Activity's onSurfaceCreated/Changed/DrawFrame) ---
//
//    override fun onSurfaceCreated(render: SampleRender) {
//        this.render = render
//        try {
//            planeRenderer = PlaneRenderer(render)
//            backgroundRenderer = BackgroundRenderer(render)
//            virtualSceneFramebuffer = Framebuffer(render, 1, 1)
//
//            // Virtual object (ARCore geospatial)
//            val virtualObjectTexture = Texture.createFromAsset(
//                render, "models/120.png", Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.SRGB
//            )
//            virtualObjectMesh = Mesh.createFromAsset(render, "models/center_size_ball.obj")
//            geospatialAnchorVirtualObjectShader =
//                Shader.createFromAssets(
//                    render, "shaders/ar_unlit_object.vert", "shaders/ar_unlit_object.frag", null
//                ).setTexture("u_Texture", virtualObjectTexture)
//
//            // Virtual object (Terrain anchor marker)
//            val terrainAnchorVirtualObjectTexture =
//                Texture.createFromAsset(
//                    render, "models/spatial_marker_yellow.png", Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.SRGB
//                )
//            terrainAnchorVirtualObjectShader =
//                Shader.createFromAssets(
//                    render, "shaders/ar_unlit_object.vert", "shaders/ar_unlit_object.frag", null
//                ).setTexture("u_Texture", terrainAnchorVirtualObjectTexture)
//
//            backgroundRenderer.setUseDepthVisualization(render, false)
//            backgroundRenderer.setUseOcclusion(render, false)
//
//            // Point cloud
//            pointCloudShader =
//                Shader.createFromAssets(
//                    render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", null
//                ).setVec4("u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f))
//                    .setFloat("u_PointSize", 5.0f)
//            pointCloudVertexBuffer = VertexBuffer(render, 4, null)
//            val pointCloudVertexBuffers = arrayOf(pointCloudVertexBuffer)
//            pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS, null, pointCloudVertexBuffers)
//
//            // Streetscape Geometry shaders
//            streetscapeGeometryBuildingShader =
//                Shader.createFromAssets(
//                    render, "shaders/streetscape_geometry.vert", "shaders/streetscape_geometry.frag", null
//                ).setBlend(DST_ALPHA, ONE)
//
//            streetscapeGeometryTerrainShader =
//                Shader.createFromAssets(
//                    render, "shaders/streetscape_geometry.vert", "shaders/streetscape_geometry.frag", null
//                ).setBlend(DST_ALPHA, ONE)
//
//            // If session was already set, update texture names now that backgroundRenderer is ready
//            session?.let {
//                it.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
//                hasSetTextureNames.set(true)
//            }
//
//        } catch (e: IOException) {
//            Log.e(TAG, "Failed to read a required asset file", e)
//            messageSnackbarHelper.showError(context, "Failed to read a required asset file: $e")
//        }
//    }
//
//    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
//        displayRotationHelper.onSurfaceChanged(width, height)
//        virtualSceneFramebuffer.resize(width, height)
//    }
//
//    override fun onDrawFrame(render: SampleRender) {
//        val currentSession = session ?: return
//
//        // Texture names should only be set once on a GL thread unless they change.
//        // This is done during onDrawFrame rather than onSurfaceCreated since the session is not
//        // guaranteed to have been initialized during the execution of onSurfaceCreated.
//        if (hasSetTextureNames.compareAndSet(false, true)) { // Only set once
//            currentSession.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
//        }
//
//        // -- Update per-frame state
//        displayRotationHelper.updateSessionIfNeeded(currentSession)
//        updateStreetscapeGeometries(currentSession.getAllTrackables(StreetscapeGeometry::class.java))
//
//        val frame: Frame
//        try {
//            frame = currentSession.update()
//        } catch (e: CameraNotAvailableException) {
//            Log.e(TAG, "Camera not available during onDrawFrame", e)
//            messageSnackbarHelper.showError(context, "Camera not available. Try restarting the app.")
//            return
//        }
//        val camera = frame.camera
//
//        backgroundRenderer.updateDisplayGeometry(frame)
//        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
//
//        val earth = currentSession.earth
//        if (earth != null) {
//            updateGeospatialState(earth)
//        }
//
//        // Handle UI messages based on geospatial state
//        val message = when (currentGeospatialState) {
//            GeospatialState.UNINITIALIZED -> "Initializing AR..." // Custom message for initial state
//            GeospatialState.UNSUPPORTED -> context.resources.getString(ar.core.ARVIAL.geospatial.R.string.status_unsupported)
//            GeospatialState.PRETRACKING -> context.resources.getString(ar.core.ARVIAL.geospatial.R.string.status_pretracking)
//            GeospatialState.EARTH_STATE_ERROR -> context.getString(ar.core.ARVIAL.geospatial.R.string.status_earth_state_error)
//            GeospatialState.LOCALIZING -> context.getString(ar.core.ARVIAL.geospatial.R.string.status_localize_hint)
//            GeospatialState.LOCALIZING_FAILED -> context.getString(ar.core.ARVIAL.geospatial.R.string.status_localize_timeout)
//            GeospatialState.LOCALIZED -> {
//                if (lastStatusText == context.getString(ar.core.ARVIAL.geospatial.R.string.status_localize_hint) || lastStatusText == "Initializing AR...") {
//                    context.getString(ar.core.ARVIAL.geospatial.R.string.status_localize_complete)
//                } else {
//                    lastStatusText // Retain message if not a state transition
//                }
//            }
//        }
//        if (message != null && lastStatusText != message) {
//            lastStatusText = message
//            onStatusMessageChanged(message)
//        }
//
//        // Update anchor count for button visibility
//        onAnchorCountChanged(anchorManager.anchorCount)
//
//
//        // Handle user input.
//        handleTap(frame, camera.trackingState)
//
//        // -- Draw background
//        if (frame.timestamp != 0L) {
//            backgroundRenderer.drawBackground(render)
//        }
//
//        // If not tracking, don't draw 3D objects.
//        if (camera.trackingState != TrackingState.TRACKING || currentGeospatialState != GeospatialState.LOCALIZED) {
//            return
//        }
//
//        // -- Draw virtual objects
//        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
//        camera.getViewMatrix(viewMatrix, 0)
//
//        // Point Cloud
//        frame.acquirePointCloud().use { pointCloud ->
//            if (pointCloud.timestamp > lastPointCloudTimestamp) {
//                pointCloudVertexBuffer.set(pointCloud.points)
//                lastPointCloudTimestamp = pointCloud.timestamp
//            }
//            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
//            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
//            render.draw(pointCloudMesh, pointCloudShader)
//        }
//
//        // Visualize planes.
//        planeRenderer.drawPlanes(
//            render, currentSession.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projectionMatrix
//        )
//
//        // Visualize anchors created by touch.
//        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
//
//        // -- Draw Streetscape Geometries.
//        if (isRenderStreetscapeGeometry) {
//            var index = 0
//            for ((streetscapeGeometry, mesh) in streetscapeGeometryToMeshes) {
//                if (streetscapeGeometry.trackingState != TrackingState.TRACKING) {
//                    continue
//                }
//                val pose = streetscapeGeometry.meshPose
//                pose.toMatrix(modelMatrix, 0)
//
//                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//                Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
//
//                if (streetscapeGeometry.type == StreetscapeGeometry.Type.BUILDING) {
//                    val color = wallsColor[index % wallsColor.size]
//                    index += 1
//                    streetscapeGeometryBuildingShader
//                        .setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
//                        .setVec4("u_Color", color)
//                    render.draw(mesh, streetscapeGeometryBuildingShader, virtualSceneFramebuffer)
//                } else if (streetscapeGeometry.type == StreetscapeGeometry.Type.TERRAIN) {
//                    val color = floatArrayOf(0.0f, 1.0f, 0.0f, 0.3f) // Green for terrain
//                    streetscapeGeometryTerrainShader
//                        .setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
//                        .setVec4("u_Color", color)
//                    render.draw(mesh, streetscapeGeometryTerrainShader, virtualSceneFramebuffer)
//                }
//            }
//        }
//
//        // Draw Anchors
//        anchorManager.setAnchors.forEach { (_, anchor) ->
//            if (anchor.trackingState == TrackingState.TRACKING) {
//                anchor.pose.toMatrix(modelMatrix, 0)
//                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//                Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
//
//                // Choose shader based on anchor type or terrain anchor state
//                val shader = when (anchor.terrainAnchorState) {
//                    // Assuming you want a distinct look for successfully resolved terrain anchors
//                    Anchor.TerrainAnchorState.SUCCESS -> terrainAnchorVirtualObjectShader
//                    // For other states (pending, failed, etc.) or standard geospatial anchors
//                    else -> geospatialAnchorVirtualObjectShader
//                }
//
//                shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
//                render.draw(virtualObjectMesh, shader, virtualSceneFramebuffer)
//            }
//        }
//
//        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
//    }
//
//    // --- Helper methods (Adapting your original methods for internal use) ---
//
//    fun setRenderStreetscapeGeometry(render: Boolean) {
//        this.isRenderStreetscapeGeometry = render
//    }
//
//    private fun addGeospatialAnchor(earth: Earth, geospatialPose: GeospatialPose) {
//        synchronized(anchorsLock) {
//            if (anchorManager.anchorCount >= MAXIMUM_ANCHORS) {
//                messageSnackbarHelper.showMessage(context, "Maximum anchors reached (${MAXIMUM_ANCHORS}).")
//                return
//            }
//
//            // Create anchor based on the currentAnchorType selected in the UI
//
//            anchorManager.setLocationAsAnchor(earth.getLocationCoords(), earth) //earth.createAnchor(geospatialPose.latitude, geospatialPose.longitude, geospatialPose.altitude, geospatialPose.eastUpNorthernPose)
//
//            Log.d(TAG, "Anchor added: ${earth.getLocationCoords()}. Total: ${anchorManager.anchorCount}")
//            onAnchorCountChanged(anchorManager.anchorCount) // Update UI
//        }
//    }
//
//    fun clearAllAnchors() {
//        synchronized(anchorsLock) {
//            anchorManager.removeAllAnchors()
//            onAnchorCountChanged(0) // Update UI
//            Log.d(TAG, "All anchors cleared.")
//        }
//    }
//
//    private fun updateGeospatialState(earth: Earth) {
//        val newState = when {
//            earth.trackingState == TrackingState.STOPPED -> GeospatialState.EARTH_STATE_ERROR
//            earth.trackingState == TrackingState.PAUSED && currentGeospatialState == GeospatialState.UNINITIALIZED -> GeospatialState.PRETRACKING
//            earth.trackingState == TrackingState.PAUSED -> currentGeospatialState // Keep current state if merely paused
//            earth.trackingState == TrackingState.TRACKING && earth.cameraGeospatialPose.horizontalAccuracy > 5.0 -> GeospatialState.LOCALIZING
//            earth.trackingState == TrackingState.TRACKING && earth.cameraGeospatialPose.horizontalAccuracy <= 5.0 -> GeospatialState.LOCALIZED
//            else -> GeospatialState.UNINITIALIZED // Fallback for unknown state
//        }
//        if (newState != currentGeospatialState) {
//            currentGeospatialState = newState
//            onGeospatialStateChanged(newState) // Notify Compose UI
//        }
//    }
//
//    private fun handleTap(frame: Frame, cameraTrackingState: TrackingState) {
//        val tap = synchronized(singleTapLock) {
//            val currentTap = queuedSingleTap
//            queuedSingleTap = null
//            currentTap
//        } ?: return // No tap to process
//
//        // Only process tap if ARCore is tracking and localized
//        if (cameraTrackingState != TrackingState.TRACKING || currentGeospatialState != GeospatialState.LOCALIZED) {
//            return
//        }
//
//        val earth = session?.earth ?: return
//
//        for (hit in frame.hitTest(tap)) {
//            val trackable = hit.trackable
//            // Only add anchor if we hit a plane or an estimated surface point
//            if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
//                (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
//            ) {
//                // Get the geospatial pose at the hit location
//                val hitGeospatialPose = earth.getGeospatialPose(hit.hitPose)
//
//                // Use the hit pose directly for simplicity, or add specific altitude adjustments
//                // for terrain/rooftop if your logic requires it.
//                // For instance, you might adjust altitude for terrain:
//                // val altitudeForTerrain = earth.getTerrainAnchorAltitude(hitGeospatialPose.latitude, hitGeospatialPose.longitude)
//                // In your original code, you directly used `earth.cameraGeospatialPose.altitude - 1.0`
//                // for new anchors, which is a relative altitude. I'll stick to that for now if you prefer.
//                val anchorAltitude = hitGeospatialPose.altitude // Using direct hit altitude
//
//                addGeospatialAnchor(
//                    earth,
//                    GeospatialPose(
//                        hitGeospatialPose.latitude,
//                        hitGeospatialPose.longitude,
//                        anchorAltitude,
//                        hitGeospatialPose.eastUpNorthernPose
//                    )
//                )
//                break // Only handle the first valid hit
//            }
//        }
//    }
//
//    // Streetscape Geometry specific logic (copied as is)
//    private fun updateStreetscapeGeometries(streetscapeGeometries: Collection<StreetscapeGeometry>) {
//        streetscapeGeometryToMeshes.entries.removeIf { (geometry, _) -> geometry.trackingState == TrackingState.STOPPED }
//
//        for (geometry in streetscapeGeometries) {
//            if (geometry.trackingState == TrackingState.TRACKING) {
//                if (!streetscapeGeometryToMeshes.containsKey(geometry)) {
//                    val vertexBuffer = VertexBuffer(render, 3, geometry.mesh.vertices)
//                    val indexBuffer = IndexBuffer(render, geometry.mesh.indices)
//                    val mesh = Mesh(render, Mesh.PrimitiveMode.TRIANGLES, indexBuffer, arrayOf(vertexBuffer))
//                    streetscapeGeometryToMeshes[geometry] = mesh
//                }
//            }
//        }
//    }
//
//    // --- GestureDetector.OnGestureListener Implementation (Copied as is) ---
//    override fun onDown(e: MotionEvent): Boolean = true
//    override fun onShowPress(e: MotionEvent) {}
//    override fun onSingleTapUp(e: MotionEvent): Boolean {
//        synchronized(singleTapLock) {
//            queuedSingleTap = e
//        }
//        return true
//    }
//    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
//    override fun onLongPress(e: MotionEvent) {}
//    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
//}