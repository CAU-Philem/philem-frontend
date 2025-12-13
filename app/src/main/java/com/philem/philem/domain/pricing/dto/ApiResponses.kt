package com.philem.philem.domain.pricing.dto

import com.google.gson.annotations.SerializedName

// 1. URL 분석 API 응답
data class AnalyzeUrlResponse(
    @SerializedName("listing_id") val listingId: Long,
    @SerializedName("post_url") val postUrl: String,
    @SerializedName("is_bundle") val isBundle: Boolean,
    @SerializedName("bundle_index") val bundleIndex: Int,
    @SerializedName("bundle_total_price") val bundleTotalPrice: Long,
    val items: List<ListingItem>
)

data class ListingItem(
    @SerializedName("model_id") val modelId: Long,
    @SerializedName("model_name") val modelName: String,
    val condition: String, // A, B, C
    val price: Long,
    @SerializedName("price_type") val priceType: String,
    val role: String, // BODY, LENS
    @SerializedName("bundle_index") val bundleIndex: Int
)

// 2. 단일 상품 비교 API 응답
data class CompareResponse(
    @SerializedName("model_id") val modelId: Long,
    val condition: String,
    @SerializedName("input_price") val inputPrice: Long,
    val available: Boolean,
    val reason: String?,
    @SerializedName("ref_year") val refYear: Int,
    @SerializedName("ref_month") val refMonth: Int,
    @SerializedName("ref_avg_price") val refAvgPrice: Long,
    @SerializedName("diff_price") val diffPrice: Long,
    @SerializedName("percent_vs_ref") val percentVsRef: Double,
    val direction: String // HIGHER, LOWER, SAME
)

// 3. 번들 비교 API 요청
data class BundleCompareRequest(
    @SerializedName("bundle_price") val bundlePrice: Long,
    val items: List<BundleCompareItem>
)

data class BundleCompareItem(
    @SerializedName("model_id") val modelId: Long,
    val condition: String
)

// 4. 번들 비교 API 응답
data class BundleCompareResponse(
    @SerializedName("bundle_price") val bundlePrice: Long,
    val available: Boolean,
    val reason: String?,
    @SerializedName("ref_year") val refYear: Int,
    @SerializedName("ref_month") val refMonth: Int,
    @SerializedName("ref_price") val refPrice: Long,
    @SerializedName("diff_price") val diffPrice: Long,
    @SerializedName("percent_vs_ref") val percentVsRef: Double,
    val direction: String,
    val items: List<BundleItemRef>
)

data class BundleItemRef(
    @SerializedName("model_id") val modelId: Long,
    val condition: String,
    @SerializedName("ref_year") val refYear: Int,
    @SerializedName("ref_month") val refMonth: Int,
    @SerializedName("ref_avg_price") val refAvgPrice: Long
)

// 5. 추천 매물 API 응답
data class RecommendationsResponse(
    val modelId: Long,
    val userRegionId: Long,
    val radiusKm: Int,
    val byCondition: Map<String, List<ListingSummary>>
)

data class ListingSummary(
    val listingSeq: Long,
    val listingId: String,
    val modelId: Long,
    val regionId: Long,
    val price: Long,
    val condition: String,
    val postUrl: String,
    val updatedAt: String
)

