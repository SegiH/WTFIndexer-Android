package com.segihovav.wtfindexer_android

import android.graphics.Color
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.segihovav.wtfindexer_android.EpisodeAdapter.MyViewHolder
import java.util.*

class EpisodeAdapter(private val episode: List<String>?, private val episodeInfo: List<String?>) : RecyclerView.Adapter<MyViewHolder>() {
    class MyViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val episode: TextView = view.findViewById(R.id.name)
        val episodeInfo: TextView = view.findViewById(R.id.episodeInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.rowlayout, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.episode.text = if (episode != null && episode[position].contains("<A")) Html.fromHtml(episode[position], HtmlCompat.FROM_HTML_MODE_LEGACY) else (episode!![position])

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(MainActivity.context))

        if (episode[position].contains("<A")) {
            if (sharedPreferences.getBoolean("DarkThemeOn", false)) holder.episode.setLinkTextColor(Color.rgb(255, 255, 255)) else holder.episode.setLinkTextColor(Color.rgb(0, 0, 0))
        }

        holder.episode.movementMethod = LinkMovementMethod.getInstance()

        if (episodeInfo[position] != null) holder.episodeInfo.text = episodeInfo[position]
    }

    override fun getItemCount(): Int {
        return episode?.size ?: 0
    }
}