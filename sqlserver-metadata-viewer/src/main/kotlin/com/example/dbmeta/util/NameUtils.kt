package com.example.dbmeta.util

object NameUtils {
    fun toCamelCase(name: String): String {
        if (name.isBlank()) return name
        val parts = name.lowercase().split("_")
        return parts.first() + parts.drop(1).joinToString("") { part ->
            part.replaceFirstChar { c -> c.uppercase() }
        }
    }
}

object KotlinTypeMapper {
    fun fromJavaType(javaType: String): String = when (javaType) {
        "java.lang.String" -> "String"
        "java.lang.Integer", "int", "java.lang.Short", "short" -> "Int"
        "java.lang.Long", "long" -> "Long"
        "java.lang.Double", "double", "java.lang.Float", "float" -> "Double"
        "java.math.BigDecimal" -> "BigDecimal"
        "java.time.LocalDateTime", "java.sql.Timestamp" -> "LocalDateTime"
        "java.time.LocalDate", "java.sql.Date" -> "LocalDate"
        "java.time.LocalTime", "java.sql.Time" -> "LocalTime"
        "java.lang.Boolean", "boolean" -> "Boolean"
        else -> "Any"
    }
}
