package com.okada.android.services

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsService {
    @GET("maps/api/directions/json")
    fun getDirections(
        @Query("mode") mode:String?,
        @Query("transit_routing_preference") transitRouting:String?,
        @Query("origin") from:String?,
        @Query("destination") to:String?,
        @Query("key") key:String
    ): Observable<String?>

    @GET("maps/api/geocode/json")
    fun getAddress(
        @Query("latlng") at:String?,
        @Query("key") key:String
    ): Observable<String?>
}