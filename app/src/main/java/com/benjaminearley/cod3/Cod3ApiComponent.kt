package com.benjaminearley.cod3

import dagger.Component

@ApiScope
@Component(dependencies = arrayOf(NetComponent::class), modules = arrayOf(Cod3ApiModule::class))
interface Cod3ApiComponent {
    fun inject(activity: MainActivity)
}
