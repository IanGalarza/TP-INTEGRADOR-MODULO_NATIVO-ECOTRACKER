package com.example.proyectointegrador.placeholder

import java.util.ArrayList
import java.util.HashMap
import com.google.firebase.firestore.FirebaseFirestore

object PlaceholderContent {

    val ITEMS: MutableList<PlaceholderItem> = ArrayList()

    val ITEM_MAP: MutableMap<String, PlaceholderItem> = HashMap()


    private fun addItem(item: PlaceholderItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    fun loadChallengesFromFirestore(onComplete: () -> Unit, onError: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("challenges_global")
            .get()
            .addOnSuccessListener { result ->
                PlaceholderContent.ITEMS.clear()
                PlaceholderContent.ITEM_MAP.clear()
                for (document in result) {
                    val item = PlaceholderItem(
                        id = document.id,
                        title = document.getString("title") ?: "Sin título",
                        description = document.getString("description") ?: "",
                        objectives = document.get("objectives") as? List<String> ?: emptyList(),
                        status = document.getString("status") ?: "INACTIVE",
                        category = document.getString("category") ?: "",
                        imageUrl = document.getString("imageUrl")
                    )
                    PlaceholderContent.ITEMS.add(item)
                    PlaceholderContent.ITEM_MAP[item.id] = item
                }
                onComplete()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    data class PlaceholderItem(
        val id: String,
        val title: String,
        val description: String,
        val objectives: List<String>,
        val status: String,
        val category: String,
        val imageUrl: String?
    ) {
        override fun toString(): String = title
    }
}