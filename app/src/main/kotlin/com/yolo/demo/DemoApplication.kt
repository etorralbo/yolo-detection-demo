package com.yolo.demo

import android.app.Application
import com.yolo.demo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DemoApplication)
            modules(appModule)
        }
    }
}
