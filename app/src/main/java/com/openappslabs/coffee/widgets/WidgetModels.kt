package com.openappslabs.coffee.widgets

enum class WidgetVariant(val key: String) {
    DEFAULT("Default"),
    MATERIAL("Material");

    companion object {
        fun fromKey(key: String?): WidgetVariant =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: DEFAULT
    }
}