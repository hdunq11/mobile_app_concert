package com.dungh.concert.network

/**
 * Maps Vietnamese UI labels to backend API filter values.
 */
object ApiMappers {

    private val cityMap = mapOf(
        "Hà Nội" to "Hanoi",
        "TP. Hồ Chí Minh" to "Ho Chi Minh",
        "Đà Nẵng" to "Da Nang",
        "Cần Thơ" to "Can Tho",
        "Hải Phòng" to "Hai Phong",
    )

    private val genreMap = mapOf(
        "Pop" to "pop",
        "K-Pop" to "kpop",
        "Rock" to "rock",
        "Ballad" to "jazz",
        "Jazz" to "jazz",
    )

    fun mapCity(uiCity: String?): String? {
        if (uiCity.isNullOrBlank() || uiCity == "Tất cả") return null
        return cityMap[uiCity] ?: uiCity
    }

    fun mapGenre(uiGenre: String?): String? {
        if (uiGenre.isNullOrBlank() || uiGenre == "Tất cả") return null
        return genreMap[uiGenre] ?: uiGenre.lowercase()
    }
}
