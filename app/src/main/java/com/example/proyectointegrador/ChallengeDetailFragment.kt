package com.example.proyectointegrador

import android.content.ClipData
import android.os.Bundle
import android.view.DragEvent
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        updateContent()
        rootView.setOnDragListener(dragListener)

        binding.fab?.setOnClickListener {
            Toast.makeText(requireContext(), "¡Challenge iniciado!", Toast.LENGTH_SHORT).show()
            guardarChallengeEnFirebase()
        }

        return rootView
    }

    private fun updateContent() {
        toolbarLayout?.title = item?.title

        item?.imageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .into(headerImageView)
        }

        item?.let {
            binding.challengeDescription?.text = it.description
            binding.challengeCategory?.text = "Category: ${it.category}"
            binding.challengeStatus?.text = "Status: ${it.status}"

            binding.challengeObjectivesContainer?.removeAllViews()
            it.objectives.forEach { objetivo ->
                val textView = TextView(requireContext()).apply {
                    text = "• $objetivo"
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    setPadding(0, 4, 0, 4)
                }
                binding.challengeObjectivesContainer?.addView(textView)
            }

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
                            binding.fab?.visibility = View.GONE
                            binding.challengeStatus?.text = "Status: ACTIVE"
                        } else {
                            binding.fab?.visibility = View.VISIBLE
                            binding.fab?.setOnClickListener {
                                guardarChallengeEnFirebase()
                            }
                        }
                    }
            } else {
                binding.fab?.visibility = View.GONE
            }
        }


    }

    private fun guardarChallengeEnFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || item == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado o challenge inválido", Toast.LENGTH_SHORT).show()
            return
        }

        PlaceholderContent.guardarChallengeEnUsuario(
            uid = user.uid,
            challenge = item!!,
            onSuccess = {
                Toast.makeText(requireContext(), "Challenge iniciado y guardado ✅", Toast.LENGTH_SHORT).show()
            },
            onError = {
                Toast.makeText(requireContext(), "Error al guardar: ${it.message}", Toast.LENGTH_SHORT).show()
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