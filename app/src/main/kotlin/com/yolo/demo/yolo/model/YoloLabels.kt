package com.yolo.demo.yolo.model

import android.content.Context

object YoloLabels {
    fun loadLabels(context: Context): List<String> {
        return context.assets.open("yolo_labels.txt").bufferedReader().use { reader ->
            reader.readLines()
        }
    }
}
