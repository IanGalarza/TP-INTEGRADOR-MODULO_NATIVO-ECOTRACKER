package com.example.proyectointegrador.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R

class ActiveChallengeAdapter(
    private val challenges: List<Pair<String, String>>, // (id, title)
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ActiveChallengeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.title)
        val card: View = view

        fun bind(challengeId: String, title: String) {
            titleText.text = title
            card.setOnClickListener { onClick(challengeId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_challenge, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = challenges.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (id, title) = challenges[position]
        holder.bind(id, title)
    }
}
