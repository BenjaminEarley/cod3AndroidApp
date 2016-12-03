package com.benjaminearley.cod3

import dagger.Module
import dagger.Provides
import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query


@Module
class Cod3ApiModule {

    interface Cod3ApiInterface {
        @GET("/Location/GetEms")
        fun getEms(@Query("Latitude") latitude: Double, @Query("Longitude") longitude: Double, @Query("metersInRadius") metersInRadius: Int): Single<Response>

        @GET("/Location/GetPolice")
        fun getPolice(@Query("Latitude") latitude: Double, @Query("Longitude") longitude: Double, @Query("metersInRadius") metersInRadius: Int): Single<Response>
    }

    @Provides
    @ApiScope
    fun providesCod3ApiInterface(retrofit: Retrofit): Cod3ApiInterface {
        return retrofit.create<Cod3ApiInterface>(Cod3ApiInterface::class.java)
    }
}