package com.openappslabs.coffee.utils

object Constants {
    object Links {
        const val GITHUB_ORG = "https://github.com/OpenAppsLabs"
        const val GITHUB_REPO = "https://github.com/OpenAppsLabs/Coffee"
        const val LICENSE = "https://www.gnu.org/licenses/gpl-3.0.en.html"
        const val SUPPORT_EMAIL = "mailto:openappslabs@gmail.com"
    }

    object Timer {
        val TIME_OPTIONS = listOf(5, 15, 30, 45, 60, 120)
    }

    object Service {
        const val CHANNEL_ID = "coffee_service_channel"
        const val NOTIFICATION_ID = 1
        
        const val ACTION_STOP = "com.openappslabs.coffee.ACTION_STOP"
        const val ACTION_EXTEND = "com.openappslabs.coffee.ACTION_EXTEND"
        const val EXTRA_DURATION_MINUTES = "DURATION_MINUTES"
    }

    object Widget {
        const val KEY_SHAPE = "widget_shape"
        const val KEY_VARIANT = "widget_variant"
    }
}