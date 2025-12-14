package com.philem.philem.data.api

import com.philem.philem.domain.pricing.dto.*
import retrofit2.http.*

interface PricingApiService {

    /**
     * 1. URL 분석 API
     * POST /listings/models-from-url
     */
    @POST("/listings/models-from-url")
    suspend fun analyzeUrl(
        @Body request: Map<String, String>
    ): AnalyzeUrlResponse

    /**
     * 2. 시세 스냅샷 조회 API
     * GET /models/{modelId}/snapshots
     */
    @GET("/models/{modelId}/snapshots")
    suspend fun getSnapshots(
        @Path("modelId") modelId: Long,
        @Query("months") months: Int = 24
    ): List<ModelPriceSnapshot>

    /**
     * 3. 단일 상품 가격 비교 API
     * GET /models/{modelId}/compare
     */
    @GET("/models/{modelId}/compare")
    suspend fun comparePrice(
        @Path("modelId") modelId: Long,
        @Query("condition") condition: String,
        @Query("price") price: Long
    ): CompareResponse

    /**
     * 4. 번들 상품 가격 비교 API
     * POST /bundles/compare
     */
    @POST("/bundles/compare")
    suspend fun compareBundlePrice(
        @Body request: BundleCompareRequest
    ): BundleCompareResponse

    /**
     * 5. 동일 상품 매물 추천 API
     * GET /recommendations
     */
    @GET("/recommendations")
    suspend fun getRecommendations(
        @Query("modelId") modelId: Long,
        @Query("userRegionId") userRegionId: Long,
        @Query("radiusKm") radiusKm: Int = 10,
        @Query("limit") limit: Int = 10,
        @Query("condition") condition: String? = null,
        @Query("itemType") itemType: String = "ALL"
    ): RecommendationsResponse

    /**
     * 6. 지역 검색 API
     * GET /regions/search
     */
    @GET("/regions/search")
    suspend fun searchRegions(
        @Query("partialInput") partialInput: String,
        @Query("limit") limit: Int = 6
    ): List<RegionSearchResult>
}
