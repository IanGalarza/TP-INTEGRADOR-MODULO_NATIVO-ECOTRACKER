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

class ChallengeDetailFragment : Fragment() {

    private var item: PlaceholderContent.PlaceholderItem? = null

    //lateinit var itemDetailTextView: TextView
    private var toolbarLayout: CollapsingToolbarLayout? = null

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

        toolbarLayout = binding.toolbarLayout
        //itemDetailTextView = binding.challengeDetail
        headerImageView = binding.headerImage!!

        // Carga los datos del desafio
        updateContent()
        rootView.setOnDragListener(dragListener)

        // Aceptar el desafio
        binding.acceptButton?.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.challenge_started), Toast.LENGTH_SHORT).show()
            guardarChallengeEnFirebase()
        }

        return rootView
    }

    private fun updateContent() {
        toolbarLayout?.title = item?.title

        binding.loadingSpinner?.visibility = View.VISIBLE
        binding.challengeDetailScrollView?.visibility = View.GONE

        item?.imageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .into(headerImageView)
        }

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
                            binding.challengeStatus?.text = getString(R.string.challenge_status, "ACTIVE")
                            binding.challengeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))

                            // Ocultamos la card vieja con el listado plano
                            binding.challengeObjectivesCard?.visibility = View.GONE

                            // Mostramos el contenedor nuevo
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

                                val taskTitle = task["title"] as? String ?: "Tarea"
                                val points = (task["points"] as? Long)?.toInt() ?: 0
                                val comentario = task["comment"] as? String ?: ""
                                val completado = task["completed"] as? Boolean ?: false
                                val photoUrl = task["photoUrl"] as? String

                                title.text = "Tarea ${index + 1}"
                                checkBox.text = "$taskTitle (+$points pts)"
                                checkBox.isChecked = completado
                                comment.setText(comentario)

                                // Mostrar imagen si existe
                                if (!photoUrl.isNullOrBlank()) {
                                    placeholderText.visibility = View.GONE
                                    imageView.visibility = View.VISIBLE
                                    Glide.with(this).load(photoUrl).into(imageView)
                                } else {
                                    imageView.visibility = View.GONE
                                    placeholderText.visibility = View.VISIBLE
                                }

                                addPhotoButton.setOnClickListener {
                                    Toast.makeText(requireContext(), "Agregar foto para '$taskTitle'", Toast.LENGTH_SHORT).show()
                                    // TODO: subir imagen
                                }

                                saveButton.setOnClickListener {
                                    val nuevoComentario = comment.text.toString()
                                    val nuevoEstado = checkBox.isChecked

                                    // Guardar en Firestore
                                    val newTask = task.toMutableMap()
                                    newTask["comment"] = nuevoComentario
                                    newTask["completed"] = nuevoEstado

                                    // Reemplazar tarea en lista
                                    val updatedTasks = savedTasks.toMutableList()
                                    updatedTasks[index] = newTask

                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(user.uid)
                                        .collection("active_challenges")
                                        .document(it.id.toString())
                                        .update("tasks", updatedTasks)
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "Guardado correctamente", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
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



    // Guardar el challenge en la coleccion del usuario

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
}