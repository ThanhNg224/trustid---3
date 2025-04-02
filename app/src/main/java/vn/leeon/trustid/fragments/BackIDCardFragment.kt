package vn.leeon.trustid.fragments

import android.annotation.SuppressLint
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.jmrtd.lds.icao.MRZInfo
import vn.leeon.eidsdk.utils.OcrUtils
import vn.leeon.trustid.R
import vn.leeon.trustid.activities.SelectionActivity

class BackIDCardFragment : Fragment() {
    private lateinit var textViewTitle: TextView
    private lateinit var buttonTakePicture: Button
    private lateinit var preview: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val activity: SelectionActivity by lazy {
        requireActivity() as SelectionActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return layoutInflater.inflate(
            R.layout.fragment_camera,
            container, false
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textViewTitle = view.findViewById(R.id.text_view_title)
        buttonTakePicture = view.findViewById(R.id.button_take_picture)
        preview = view.findViewById(R.id.view_finder)
        textViewTitle.text = "Chụp mặt sau CCCD"
        preview.post { startCamera() }
        buttonTakePicture.setOnClickListener { takePhoto() }
    }

    override fun onResume() {
        super.onResume()
        activity.step = 2
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.step = 1
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        imageCapture = ImageCapture.Builder().build()
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(preview.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector,
                    preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("TAG", "Photo capture failed: ${exc.message}", exc)
                }

                @ExperimentalGetImage
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    activity.backBitmap = imageProxy.toBitmap()
                    val start = System.currentTimeMillis()
                    val mediaImage: Image = imageProxy.image ?: return
                    val inputImage: InputImage = InputImage.fromMediaImage(
                        mediaImage, imageProxy.imageInfo.rotationDegrees
                    )
                    recognizer.process(inputImage).addOnSuccessListener {
                        val timeRequired = System.currentTimeMillis() - start
                        OcrUtils.processOcr(
                            results = it, timeRequired = timeRequired,
                            callback = object : OcrUtils.MRZCallback {
                                override fun onMRZRead(mrzInfo: MRZInfo, timeRequired: Long) {
                                    activity.onEidRead(mrzInfo)
                                }

                                override fun onMRZReadFailure(timeRequired: Long) {
                                    Log.e("TAG", "onMRZReadFailure")
                                }

                                override fun onFailure(e: Exception, timeRequired: Long) {
                                    Log.e("TAG", "onFailure: ${e.message}", e)
                                }
                            }
                        )
                    }.addOnFailureListener {
                        Log.e("TAG", "analyze: ${it.message}", it)
                    }
                }
            }
        )
    }
}