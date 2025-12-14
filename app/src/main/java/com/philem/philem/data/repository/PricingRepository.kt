package com.philem.philem.data.repository

import com.philem.philem.data.api.RetrofitClient
import com.philem.philem.domain.pricing.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PricingRepository {

    private val api = RetrofitClient.pricingApi
    private val regionApi = RetrofitClient.regionApi

    /**
     * URL 분석
     */
    suspend fun analyzeUrl(url: String): Result<AnalyzeUrlResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.analyzeUrl(mapOf("url" to url))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 시세 스냅샷 조회
     */
    suspend fun getSnapshots(modelId: Long, months: Int = 24): Result<List<ModelPriceSnapshot>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSnapshots(modelId, months)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 단일 상품 가격 비교
     */
    suspend fun comparePrice(modelId: Long, condition: String, price: Long): Result<CompareResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.comparePrice(modelId, condition, price)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 번들 상품 가격 비교
     */
    suspend fun compareBundlePrice(bundlePrice: Long, items: List<BundleCompareItem>): Result<BundleCompareResponse> = withContext(Dispatchers.IO) {
        try {
            val request = BundleCompareRequest(bundlePrice, items)
            val response = api.compareBundlePrice(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 동일 상품 매물 추천
     */
    suspend fun getRecommendations(
        modelId: Long,
        userRegionId: Long,
        radiusKm: Int = 10,
        limit: Int = 10
    ): Result<RecommendationsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRecommendations(modelId, userRegionId, radiusKm, limit)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 지역 검색
     */
    suspend fun searchRegions(partialInput: String, limit: Int = 6): Result<List<RegionSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val response = regionApi.searchRegions(partialInput, limit)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
