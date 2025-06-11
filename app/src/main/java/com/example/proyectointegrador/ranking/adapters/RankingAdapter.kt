package com.example.proyectointegrador.ranking.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.ranking.model.UserRanking

class RankingAdapter(private val users: List<UserRanking>) :
    RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val positionText: TextView = itemView.findViewById(R.id.ranking_position)
        val nameText: TextView = itemView.findViewById(R.id.ranking_username)
        val pointsText: TextView = itemView.findViewById(R.id.ranking_points)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val user = users[position]
        holder.positionText.text = "#${position + 1}"
        holder.nameText.text = user.name
        holder.pointsText.text = "${user.points} pts"
    }

    override fun getItemCount(): Int = users.size
}