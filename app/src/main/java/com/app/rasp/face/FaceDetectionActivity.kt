package com.app.rasp.face

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.app.rasp.R
import com.app.rasp.base.BaseActivity
import com.app.rasp.databinding.ActivityFaceDetectionBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RuntimePermissions
class FaceDetectionActivity : BaseActivity<ActivityFaceDetectionBinding>() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    // High-accuracy landmark detection and face classification
    val highAccuracyOpts by lazy {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    }

    // Real-time contour detection
    val realTimeOpts by lazy {
        FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    }

    override fun initViews() {
        needsCameraPermissionWithPermissionCheck()

        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun getLayoutId(): Int = R.layout.activity_face_detection

    @NeedsPermission(Manifest.permission.CAMERA)
    fun needsCameraPermission() {
        startCamera()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        /*   imageCapture.takePicture(
               outputOptions,
               ContextCompat.getMainExecutor(this),
               object : ImageCapture.OnImageSavedCallback {
                   override fun onError(exc: ImageCaptureException) {
                       Log.e("onError", "Photo capture failed: ${exc.message}", exc)
                   }

                   override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                       val savedUri = Uri.fromFile(photoFile)
                       val msg = "Photo capture succeeded: $savedUri"
                       Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                       Log.d("onImageSaved", msg)
                   }
               })*/

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                super.onCaptureSuccess(imageProxy)
                imageProxy.image?.let {
                    val image = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
                    val detector = FaceDetection.getClient()
                    val result = detector.process(image)
                        .addOnSuccessListener { faces ->
                            for (face in faces) {
                                val bounds = face.boundingBox
                                val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                                val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                                // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                // nose available):
                                val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                                leftEar?.let {
                                    val leftEarPos = leftEar.position
                                }

                                // If contour detection was enabled:
                                val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
                                val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points

                                // If classification was enabled:
                                if (face.smilingProbability != null) {
                                    val smileProb = face.smilingProbability
                                }
                                if (face.rightEyeOpenProbability != null) {
                                    val rightEyeOpenProb = face.rightEyeOpenProbability
                                }

                                // If face tracking was enabled:
                                if (face.trackingId != null) {
                                    val id = face.trackingId
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                exception.printStackTrace()
            }
        })
    }


    private fun startCamera() {
        imageCapture = ImageCapture.Builder().build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}