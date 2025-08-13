package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var predictButton: Button
    private lateinit var resultBox: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "http://10.0.2.2:5000/predict_image"

    // Activity result launchers
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
            predictButton.isEnabled = true
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            currentPhotoPath?.let { path ->
                selectedImageUri = Uri.fromFile(File(path))
                imageView.setImageURI(selectedImageUri)
                predictButton.isEnabled = true
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            showToast("Izin kamera diperlukan untuk mengambil foto")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        takePhotoButton = view.findViewById(R.id.takePhotoButton)
        predictButton = view.findViewById(R.id.predictButton)
        resultBox = view.findViewById(R.id.resultBox)
        progressBar = view.findViewById(R.id.progressBar)

        predictButton.isEnabled = false

        selectImageButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        takePhotoButton.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }

        predictButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                predictImage(uri)
            }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        currentPhotoPath = photoFile.absolutePath

        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )

        takePhotoLauncher.launch(photoUri)
    }

    private fun predictImage(imageUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                showLoading(true)
                resultBox.text = "Menganalisis gambar..."
            }

            try {
                val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                inputStream?.use { stream ->
                    val imageBytes = stream.readBytes()

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "image",
                            "image.jpg",
                            RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
                        )
                        .build()

                    val request = Request.Builder()
                        .url(apiUrl)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    withContext(Dispatchers.Main) {
                        showLoading(false)

                        if (response.isSuccessful) {
                            val json = JSONObject(responseBody)
                            val gender = json.getString("gender")
                            val usage = json.getString("usage")
                            val article = json.getString("article")

                            resultBox.text = """
                                Hasil Prediksi Fashion:
                                
                                Gender: $gender
                                Penggunaan: $usage
                                Jenis Artikel: $article
                            """.trimIndent()
                        } else {
                            try {
                                val errorJson = JSONObject(responseBody)
                                val errorMsg = errorJson.getString("error")
                                resultBox.text = "Error: $errorMsg"
                            } catch (e: Exception) {
                                resultBox.text = "Gagal: ${response.code}"
                            }
                            showToast("Gagal prediksi")
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    resultBox.text = "Error jaringan: ${e.localizedMessage}"
                    showToast("Jaringan gagal")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    resultBox.text = "Error: ${e.localizedMessage}"
                    showToast("Terjadi error")
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        predictButton.isEnabled = !show && selectedImageUri != null
        selectImageButton.isEnabled = !show
        takePhotoButton.isEnabled = !show
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}