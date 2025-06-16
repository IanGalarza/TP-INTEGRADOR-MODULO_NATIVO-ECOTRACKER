package com.example.proyectointegrador

import android.content.ClipData
import android.os.Bundle
import android.view.DragEvent
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.proyectointegrador.placeholder.PlaceholderContent
import com.example.proyectointegrador.databinding.FragmentChallengeDetailBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.Manifest
import android.widget.ProgressBar

class ChallengeDetailFragment : Fragment() {

    private var item: PlaceholderContent.PlaceholderItem? = null
    private val CAMERA_PERMISSION_CODE = 2001


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    //lateinit var itemDetailTextView: TextView

    private var currentTaskIndexForPhoto: Int? = null
    private var currentPhotoTempFile: File? = null
    private var imageUrl: String? = null
    private val pendingPhotoUris: MutableMap<Int, Uri> = mutableMapOf()

    private val pickPhotoFromGallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadPhotoAndAttachToTask(it, fromCamera = false) }
    }

    private val takePhotoFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoTempFile != null) {
            val uri = Uri.fromFile(currentPhotoTempFile)
            uploadPhotoAndAttachToTask(uri, fromCamera = true)
        } else {
            currentPhotoTempFile?.delete()
        }
    }

    private var _binding: FragmentChallengeDetailBinding? = null

    lateinit var headerImageView: ImageView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val dragListener = View.OnDragListener { v, event ->
        if (event.action == DragEvent.ACTION_DROP) {
            val clipDataItem: ClipData.Item = event.clipData.getItemAt(0)
            val dragData = clipDataItem.text
            item = PlaceholderContent.ITEM_MAP[dragData]
            updateContent()
        }
        true
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera(currentTaskIndexForPhoto ?: 0)
        } else {
            Toast.makeText(requireContext(), "Se requiere permiso de cámara para tomar fotos.", Toast.LENGTH_SHORT).show()
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the placeholder content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item = PlaceholderContent.ITEM_MAP[it.getString(ARG_ITEM_ID)]
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentChallengeDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root

        //itemDetailTextView = binding.challengeDetail
        headerImageView = binding.headerImage!!

        // Carga los datos del desafio
        updateContent()
        rootView.setOnDragListener(dragListener)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        solicitarUbicacion()

        // Aceptar el desafio
        binding.acceptButton?.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.challenge_started), Toast.LENGTH_SHORT).show()
            guardarChallengeEnFirebase()
        }

        return rootView
    }

    private fun updateContent() {

        binding.loadingSpinner?.visibility = View.VISIBLE
        binding.challengeDetailScrollView?.visibility = View.GONE

        item?.imageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .into(headerImageView)
        }
        val challengeId = item?.id ?: return
        item?.let {
            binding.challengeDescription?.text = it.description
            binding.challengeCategory?.text = getString(R.string.challenge_category, it.category)
            binding.challengeDuration?.text = getString(R.string.challenge_duration, it.durationInDays)

            val statusColor = when (it.status.uppercase()) {
                "INACTIVE" -> R.color.red
                else -> R.color.green
            }
            binding.challengeStatus?.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .collection("active_challenges")
                    .document(it.id)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {

                            // Desafío aceptado

                            binding.acceptButton?.visibility = View.GONE
                            val savedStatus = doc.getString("status") ?: "ACTIVE"
                            binding.challengeStatus?.text = getString(R.string.challenge_status, savedStatus)
                            binding.challengeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))

                            // Se oculta la card con los objetivos planos

                            binding.challengeObjectivesCard?.visibility = View.GONE

                            // Se muestra el contenedor de las tareas

                            binding.acceptedObjectivesContainer?.visibility = View.VISIBLE
                            binding.acceptedObjectivesContainer?.removeAllViews()

                            val savedTasks = doc["tasks"] as? List<Map<String, Any>> ?: emptyList()

                            savedTasks.forEachIndexed { index, task ->

                                val objetivoView = layoutInflater.inflate(R.layout.challenge_objective_item, binding.acceptedObjectivesContainer, false)

                                val title = objetivoView.findViewById<TextView>(R.id.objective_title)
                                val checkBox = objetivoView.findViewById<CheckBox>(R.id.objective_checkbox)
                                val comment = objetivoView.findViewById<EditText>(R.id.objective_comment)
                                val addPhotoButton = objetivoView.findViewById<Button>(R.id.button_add_photo)
                                val imageView = objetivoView.findViewById<ImageView>(R.id.objective_image)
                                val placeholderText = objetivoView.findViewById<TextView>(R.id.no_image_placeholder)
                                val saveButton = objetivoView.findViewById<Button>(R.id.button_save)

                                val headerContainer = objetivoView.findViewById<View>(R.id.header_container)
                                val expandableContent = objetivoView.findViewById<View>(R.id.expandable_content)
                                val expandIcon = objetivoView.findViewById<ImageView>(R.id.expand_icon)

                                headerContainer.setOnClickListener {
                                    val isExpanded = expandableContent.visibility == View.VISIBLE

                                    //Mostrar o no el contenido
                                    expandableContent.visibility = if (isExpanded) View.GONE else View.VISIBLE

                                    // Animación de rotación del ícono
                                    val rotationAngle = if (isExpanded) 0f else 180f
                                    expandIcon.animate().rotation(rotationAngle).setDuration(200).start()
                                }

                                val taskTitle = task["title"] as? String ?: "Task"
                                val points = (task["points"] as? Long)?.toInt() ?: 0
                                val comentario = task["comment"] as? String ?: ""
                                val completado = task["completed"] as? Boolean ?: false
                                val photoUrl = task["photoUrl"] as? String

                                title.text = getString(R.string.task_number, index + 1)
                                checkBox.text = "$taskTitle (+$points pts)"
                                checkBox.isChecked = completado
                                comment.setText(comentario)

                                if (!photoUrl.isNullOrBlank()) {
                                    placeholderText.visibility = View.GONE
                                    imageView.visibility = View.VISIBLE
                                    Glide.with(this).load(photoUrl).into(imageView)
                                } else {
                                    imageView.visibility = View.GONE
                                    placeholderText.visibility = View.VISIBLE
                                }

                                addPhotoButton.setOnClickListener {
                                    showPhotoSourceDialog(index)
                                }

                                saveButton.setOnClickListener {
                                    val saveLoader = objetivoView.findViewById<ProgressBar>(R.id.save_loader)
                                    saveButton.visibility = View.GONE
                                    saveLoader.visibility = View.VISIBLE
                                    val uriFotoPendiente = pendingPhotoUris[index]
                                    val nuevoComentario = comment.text.toString()
                                    val nuevoEstado = checkBox.isChecked

                                    val newTask = task.toMutableMap()
                                    newTask["comment"] = nuevoComentario
                                    newTask["completed"] = nuevoEstado
                                    userLocation?.let { location ->
                                        val locMap = mapOf("lat" to location.latitude, "lng" to location.longitude)
                                        newTask["location"] = locMap
                                    }

                                    fun guardarEnFirestore(photoUrl: String? = null) {
                                        if (photoUrl != null) newTask["photoUrl"] = photoUrl
                                        val updatedTasks = savedTasks.toMutableList()
                                        updatedTasks[index] = newTask

                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(user.uid)
                                            .collection("active_challenges")
                                            .document(challengeId)
                                            .update("tasks", updatedTasks)
                                            .addOnSuccessListener {
                                                saveButton.visibility = View.VISIBLE
                                                saveLoader.visibility = View.GONE
                                                Toast.makeText(requireContext(), "Guardado correctamente", Toast.LENGTH_SHORT).show()
                                                pendingPhotoUris.remove(index)

                                            }
                                            .addOnFailureListener {
                                                saveButton.visibility = View.VISIBLE
                                                saveLoader.visibility = View.GONE
                                                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    if (uriFotoPendiente != null) {
                                        subirFotoACloudinary(uriFotoPendiente) { cloudUrl ->
                                            if (cloudUrl == null) {
                                                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                                            } else {
                                                guardarEnFirestore(cloudUrl)
                                            }
                                        }
                                    } else {
                                        guardarEnFirestore()
                                    }
                                }
                                binding.acceptedObjectivesContainer?.addView(objetivoView)
                            }

                        } else {
                            // Desafío no aceptado

                            binding.acceptButton?.visibility = View.VISIBLE
                            binding.challengeStatus?.text = getString(R.string.challenge_status, it.status)
                            binding.challengeObjectivesLabel?.visibility = View.VISIBLE
                            binding.challengeObjectivesContainer?.visibility = View.VISIBLE
                            binding.acceptedObjectivesContainer?.visibility = View.GONE
                            binding.challengeObjectivesContainer?.removeAllViews()

                            it.objectives.forEach { objetivo ->
                                val textView = TextView(requireContext()).apply {
                                    text = "✅ ${objetivo.title} (+${objetivo.points} pts)"
                                    textSize = 16f
                                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                                    setPadding(0, 4, 0, 4)
                                }
                                binding.challengeObjectivesContainer?.addView(textView)
                            }

                            binding.acceptButton?.setOnClickListener {
                                guardarChallengeEnFirebase()
                            }
                        }

                        binding.loadingSpinner?.visibility = View.GONE
                        binding.challengeDetailScrollView?.visibility = View.VISIBLE
                    }
            } else {
                binding.acceptButton?.visibility = View.GONE
            }
        }
    }

    private fun showPhotoSourceDialog(forTaskIndex: Int) {
        currentTaskIndexForPhoto = forTaskIndex

        val options = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar fuente")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickPhotoFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> checkCameraPermissionAndOpen(forTaskIndex)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun checkCameraPermissionAndOpen(forTaskIndex: Int) {
        currentTaskIndexForPhoto = forTaskIndex
        when {
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera(forTaskIndex)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera(forTaskIndex: Int) {
        val tempFile = File.createTempFile("IMG_${UUID.randomUUID()}", ".jpg", requireContext().cacheDir)
        currentPhotoTempFile = tempFile
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            tempFile
        )
        takePhotoFromCamera.launch(photoUri)
    }


    private fun uploadPhotoAndAttachToTask(uri: Uri, fromCamera: Boolean) {
        val taskIndex = currentTaskIndexForPhoto ?: return
        pendingPhotoUris[taskIndex] = uri

        val container = binding.acceptedObjectivesContainer
        val objetivoView = container?.getChildAt(taskIndex)
        val imageView = objetivoView?.findViewById<ImageView>(R.id.objective_image)
        val placeholderText = objetivoView?.findViewById<TextView>(R.id.no_image_placeholder)

        if (imageView != null && placeholderText != null) {
            placeholderText.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(imageView)
        }
    }

    private fun subirFotoACloudinary(uri: Uri, onResult: (String?) -> Unit) {
        val context = requireContext()
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("IMG_${UUID.randomUUID()}", ".jpg", context.cacheDir)
        tempFile.outputStream().use { fileOut -> inputStream?.copyTo(fileOut) }

        val cloudName = "dywphbg73"
        val uploadPreset = "unsigned_challenges"
        val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart("folder", "folder/appsmoviles")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                val urlRegex = """"secure_url"\s*:\s*"([^"]+)"""".toRegex()
                val urlMatch = urlRegex.find(responseBody ?: "")
                val imageUrl = urlMatch?.groups?.get(1)?.value
                requireActivity().runOnUiThread {
                    onResult(imageUrl)
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    onResult(null)
                }
            } finally {
                tempFile.delete()
            }
        }.start()
    }



    private fun guardarChallengeEnFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || item == null) {
            Toast.makeText(requireContext(), getString(R.string.error_auth_or_challenge), Toast.LENGTH_SHORT).show()
            return
        }

        PlaceholderContent.guardarChallengeEnUsuario(
            uid = user.uid,
            challenge = item!!,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.challenge_saved_success), Toast.LENGTH_SHORT).show()

                updateContent()
            },
            onError = {
                Toast.makeText(requireContext(), getString(R.string.error_saving_challenge, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        )
    }

    companion object {

        const val ARG_ITEM_ID = "item_id"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun solicitarUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location: Location? ->
                userLocation = location
            }.addOnFailureListener {
                userLocation = null
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            solicitarUbicacion()
        }
    }
}