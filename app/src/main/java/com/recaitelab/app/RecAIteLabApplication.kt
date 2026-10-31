package com.recaitelab.app

import android.app.Application
import com.recaitelab.app.di.AppContainer

/** 进程入口：只负责初始化手动依赖容器，业务逻辑不放这里。 */
class RecAIteLabApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}
