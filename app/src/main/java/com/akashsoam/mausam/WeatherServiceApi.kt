package com.akashsoam.mausam

import com.akashsoam.mausam.models.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherServiceApi {
    @GET("2.5/weather")
    fun getWeatherDetails(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") appid: String,
        @Query("units") metric: String
    ): retrofit2.Call<WeatherResponse>
}