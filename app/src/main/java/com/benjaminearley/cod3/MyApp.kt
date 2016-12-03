package com.benjaminearley.cod3

import android.app.Application


class MyApp : Application() {

    companion object {
        lateinit var netComponent: NetComponent
        lateinit var cod3ApiComponent: Cod3ApiComponent
    }

    override fun onCreate() {
        super.onCreate()

        netComponent = DaggerNetComponent.builder()
                .appModule(AppModule(this))
                .netModule(NetModule("https://???.com"))
                .build()

        cod3ApiComponent = DaggerCod3ApiComponent.builder()
                .netComponent(netComponent)
                .cod3ApiModule(Cod3ApiModule())
                .build()

    }
}
