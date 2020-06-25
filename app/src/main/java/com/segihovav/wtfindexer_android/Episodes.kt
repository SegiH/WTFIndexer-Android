package com.segihovav.wtfindexer_android

data class Episodes internal constructor(val episodeNumber: Int, val name: String, val releaseDate: String, var favorite: Int, val imdbLink: String, var isCheckedIn: Boolean) {
    fun toggleCheckedInOut() {
        isCheckedIn = !isCheckedIn
    }

    fun toggleFavorite() {
        favorite = if (favorite == 0) 1 else 0
    }

    fun isEpisodeCheckedIn():Boolean { return isCheckedIn }
}