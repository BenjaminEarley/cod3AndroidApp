package com.benjaminearley.cod3

import dagger.Module
import dagger.Provides
import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.http.GET


@Module
class Cod3ApiModule {

    interface Cod3ApiInterface {
        @GET("/test")
        fun test(): Single<Unit>
    }

    @Provides
    @ApiScope
    fun providesCod3ApiInterface(retrofit: Retrofit): Cod3ApiInterface {
        return retrofit.create<Cod3ApiInterface>(Cod3ApiInterface::class.java)
    }
}