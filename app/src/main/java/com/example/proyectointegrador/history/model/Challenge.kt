package com.example.proyectointegrador.history.model

import java.util.Date

data class Challenge(
    val id: String = "",
    val imageUrl: String,
    val title: String,
    val category: String,
    val description: String,
    val durationInDays: Int,
    val startedAt: Date,
    val endDate: Date,
    val extraPoints: Int,
    val status: String,
    val tasks: List<TaskData>
)
data class TaskData(
    val id: String,
    val title: String,
    val points: Int,
    val completed: Boolean,
    val comment: String,
    val location: Location,
    val photoUrl: String?
)

data class Location(
    val city: String,
    val country: String,
    val lat: Double,
    val lng: Double
)