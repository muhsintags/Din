package com.example.data.network

import com.example.data.model.MiraclePost
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface MiraclesApiService {
    @GET("api/v1/miracles")
    suspend fun getMiracles(): Response<List<MiraclePost>>

    @GET("api/v1/miracles.json")
    suspend fun getMiraclesJson(): Response<List<MiraclePost>>

    @GET("data.json")
    suspend fun getDataJson(): Response<List<MiraclePost>>

    @GET("miracles.json")
    suspend fun getMiraclesRootJson(): Response<List<MiraclePost>>

    @GET("db.json")
    suspend fun getDbJson(): Response<List<MiraclePost>>

    @GET("posts.json")
    suspend fun getPostsJson(): Response<List<MiraclePost>>

    @GET
    suspend fun getFromUrl(@Url url: String): Response<List<MiraclePost>>

    @GET("api/v1/miracles/search")
    suspend fun searchMiracles(
        @Query("q") query: String?,
        @Query("tag") tag: String?
    ): Response<List<MiraclePost>>
}

