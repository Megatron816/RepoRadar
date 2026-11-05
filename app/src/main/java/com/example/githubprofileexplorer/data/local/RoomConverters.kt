package com.example.githubprofileexplorer.data.local

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun topicsToString(topics: List<String>): String = topics.joinToString(DELIMITER)

    @TypeConverter
    fun stringToTopics(value: String): List<String> =
        value.split(DELIMITER).filter(String::isNotBlank)

    private companion object {
        const val DELIMITER = "\u001F"
    }
}
