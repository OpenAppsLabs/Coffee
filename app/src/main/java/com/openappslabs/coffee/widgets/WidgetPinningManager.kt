package com.openappslabs.coffee.widgets

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.openappslabs.coffee.utils.Constants

fun handleWidgetPinning(
    context: Context,
    shape: String,
    variant: String,
    manager: AppWidgetManager,
    activity: Activity?
) {
    val receiverClass = CoffeeWidgetReceiver::class.java

    if (manager.isRequestPinAppWidgetSupported) {
        val callbackIntent = Intent(context, receiverClass).apply {
            putExtra(Constants.Widget.KEY_SHAPE, shape)
            putExtra(Constants.Widget.KEY_VARIANT, variant)
        }

        val successCallback = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            callbackIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bundle = Bundle()
        val previewViews = createApiPreview(context, shape, variant)
        bundle.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)

        manager.requestPinAppWidget(
            ComponentName(context, receiverClass),
            bundle,
            successCallback
        )
        activity?.moveTaskToBack(true)
    }
}
