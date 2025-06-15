package com.example.proyectointegrador.placeholder

import com.google.firebase.firestore.DocumentSnapshot
import java.util.ArrayList
import java.util.HashMap
import com.google.firebase.firestore.FirebaseFirestore

object PlaceholderContent {

    val ITEMS: MutableList<PlaceholderItem> = ArrayList()
    val ITEM_MAP: MutableMap<String, PlaceholderItem> = HashMap()

    private fun addItem(item: PlaceholderItem) {
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    fun loadChallengesFromFirestore(onComplete: () -> Unit, onError: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("challenges_global")
            .get()
            .addOnSuccessListener { result ->
                PlaceholderContent.ITEMS.clear()
                PlaceholderContent.ITEM_MAP.clear()
                for (document in result) {
                    val objectivesList = (document["objectives"] as? List<Map<String, Any>>)?.map { obj ->
                        Objective(
                            id = obj["id"] as? String ?: "",
                            title = obj["title"] as? String ?: "",
                            points = (obj["points"] as? Long)?.toInt() ?: 0
                        )
                    } ?: emptyList()

                    val item = PlaceholderItem(
                        id = document.id,
                        title = document.getString("title") ?: "Sin título",
                        description = document.getString("description") ?: "",
                        objectives = objectivesList,
                        status = document.getString("status") ?: "INACTIVE",
                        category = document.getString("category") ?: "",
                        imageUrl = document.getString("imageUrl"),
                        durationInDays = (document.getLong("durationInDays") ?: 0L).toInt()
                    )
                    ITEMS.add(item)
                    ITEM_MAP[item.id] = item
                }
                onComplete()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun guardarChallengeEnUsuario(uid: String, challenge: PlaceholderItem, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val now = com.google.firebase.Timestamp.now()

        // Calcular endDate
        val calendar = java.util.Calendar.getInstance()
        calendar.time = now.toDate()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, challenge.durationInDays)
        val endDate = com.google.firebase.Timestamp(calendar.time)

        // Crear lista de tasks diarias a partir de los objectives
        val tasksList = challenge.objectives.map { objective ->
            mapOf(
                "id" to objective.id,
                "title" to objective.title,
                "points" to objective.points,
                "completed" to false,
                "photoUrl" to null,
                "comment" to null,
                "location" to null
            )
        }

        // Mapa completo del desafío activo
        val challengeMap = hashMapOf(
            "title" to challenge.title,
            "description" to challenge.description,
            "status" to "ACTIVE",
            "startedAt" to now,
            "endDate" to endDate,
            "durationInDays" to challenge.durationInDays,
            "category" to challenge.category,
            "imageUrl" to challenge.imageUrl,
            "tasks" to tasksList
        )

        firestore.collection("users")
            .document(uid)
            .collection("active_challenges")
            .document(challenge.id)
            .set(challengeMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }


    fun addItemFromDocument(document: DocumentSnapshot) {
        val objectivesList = (document["objectives"] as? List<Map<String, Any>>)?.map { obj ->
            Objective(
                id = obj["id"] as? String ?: "",
                title = obj["title"] as? String ?: "",
                points = (obj["points"] as? Long)?.toInt() ?: 0
            )
        } ?: emptyList()

        val item = PlaceholderItem(
            id = document.id,
            title = document.getString("title") ?: "Sin título",
            description = document.getString("description") ?: "",
            objectives = objectivesList,
            status = document.getString("status") ?: "INACTIVE",
            category = document.getString("category") ?: "",
            imageUrl = document.getString("imageUrl"),
            durationInDays = (document.getLong("durationInDays") ?: 0L).toInt()
        )
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    data class PlaceholderItem(
        val id: String,
        val title: String,
        val description: String,
        val objectives: List<Objective>,
        val status: String,
        val category: String,
        val durationInDays: Int,
        val imageUrl: String?
    ) {
        override fun toString(): String = title
    }

    data class Objective(
        val id: String,
        val title: String,
        val points: Int
    )
}
