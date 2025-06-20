package com.example.proyectointegrador.history.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.history.model.Challenge
import com.google.android.material.card.MaterialCardView

class ChallengeHistoryAdapter(
    private var challenges: List<Challenge>,
    private val onItemClick: (Challenge) -> Unit,
    private val onDownloadPdf: (Challenge) -> Unit
) : RecyclerView.Adapter<ChallengeHistoryAdapter.ChallengeViewHolder>() {

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentTextView: TextView = itemView.findViewById(R.id.content)
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(challenges[position])
                }
            }
            itemView.setOnLongClickListener { view ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showContextMenu(view, challenges[position])
                }
                true
            }
        }

        private fun showContextMenu(view: View, challenge: Challenge) {
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.history_context_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_download_pdf -> {
                        onDownloadPdf(challenge)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.challenge_list_content, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.contentTextView.text = challenge.title

        // Cambiar el color de fondo segun el estado del challenge

        when (challenge.status) {
            "ACTIVE" -> {
                (holder.itemView as MaterialCardView).setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.green)
                )
            }
            "CANCELLED" -> {
                (holder.itemView as MaterialCardView).setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.red)
                )
            }
            "COMPLETED" -> {
                (holder.itemView as MaterialCardView).setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.aqua_green)
                )
            }
            else -> {
                (holder.itemView as MaterialCardView).setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.gray)
                )
            }
        }
    }


    override fun getItemCount(): Int = challenges.size

    fun updateData(newChallenges: List<Challenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }
}

