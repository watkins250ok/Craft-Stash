package com.example.data.database

import androidx.room.TypeConverter
import com.example.domain.model.MatchStatus
import com.example.domain.model.ProjectStatus
import com.example.domain.model.QuantityMode

class Converters {
    @TypeConverter
    fun fromQuantityMode(value: QuantityMode): String = value.name

    @TypeConverter
    fun toQuantityMode(value: String): QuantityMode = try {
        QuantityMode.valueOf(value)
    } catch (e: Exception) {
        QuantityMode.EXACT
    }

    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = try {
        ProjectStatus.valueOf(value)
    } catch (e: Exception) {
        ProjectStatus.PLANNED
    }

    @TypeConverter
    fun fromMatchStatus(value: MatchStatus): String = value.name

    @TypeConverter
    fun toMatchStatus(value: String): MatchStatus = try {
        MatchStatus.valueOf(value)
    } catch (e: Exception) {
        MatchStatus.EXACT_MATCH
    }
}
