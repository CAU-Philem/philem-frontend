package com.philem.philem.data.api

import com.philem.philem.domain.pricing.dto.RegionSearchResult
import retrofit2.http.GET
import retrofit2.http.Query

interface RegionApiService {
    @GET("/regions/search")
    suspend fun searchRegions(
        @Query("partialInput") partialInput: String,
        @Query("limit") limit: Int = 6
    ): List<RegionSearchResult>
}

