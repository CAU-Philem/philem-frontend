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
// ApiResponses.kt 수정
data class RecommendationsResponse(
    @SerializedName("model_id") val modelId: Long,
    @SerializedName("user_region_id") val userRegionId: Long,
    @SerializedName("radius_km") val radiusKm: Int,
    @SerializedName("by_condition") val byCondition: Map<String, List<ListingSummary>>
)
data class ListingSummary(
    @SerializedName("listing_seq") val listingSeq: Long,
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("model_id") val modelId: Long,
    @SerializedName("region_id") val regionId: Long,
    @SerializedName("region_name") val regionName: String?,
    @SerializedName("price") val price: Long?,
    @SerializedName("is_bundle") val isBundle: Boolean?,
    @SerializedName("condition") val condition: String,
    @SerializedName("post_url") val postUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("boosted_at") val boostedAt: String?,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class RegionSearchResult(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double
)

// 연관 제품 추천 API 요청
data class RelatedProductsRequest(
    val mode: String = "FILTER", // FILTER, BODY_TO_LENS, LENS_TO_BODY, BUNDLE
    @SerializedName("conditions") val conditions: List<String>? = null, // 멀티 선택 지원
    @SerializedName("minPrice") val minPrice: Long? = null,
    @SerializedName("maxPrice") val maxPrice: Long? = null,
    @SerializedName("brands") val brands: List<String>? = null, // 멀티 선택 지원
    @SerializedName("unitType") val unitType: String, // BODY, LENS - 필수
    @SerializedName("cameraTypes") val cameraTypes: List<String>? = null, // 멀티 선택 지원
    @SerializedName("mounts") val mounts: List<String>? = null, // 멀티 선택 지원
    @SerializedName("sensorFormats") val sensorFormats: List<String>? = null, // 멀티 선택 지원
    @SerializedName("bodyModelId") val bodyModelId: Long? = null,
    @SerializedName("lensModelId") val lensModelId: Long? = null,
    @SerializedName("presetLensBrand") val presetLensBrand: String? = null,
    val page: Int = 0,
    val size: Int = 20
)

// 연관 제품 추천 API 응답
data class RelatedProductsResponse(
    val items: List<RelatedProductItem>,
    val page: Int,
    val size: Int,
    val total: Int
)

data class RelatedProductItem(
    @SerializedName("listingItemId") val listingItemId: Long,
    @SerializedName("modelId") val modelId: Long,
    @SerializedName("modelName") val modelName: String,
    val brand: String,
    @SerializedName("unitType") val unitType: String,
    @SerializedName("cameraType") val cameraType: String?,
    val mount: String?,
    // [추가] 이 줄을 추가해야 합니다!
    @SerializedName("priceType") // JSON 키 이름과 정확히 매칭 (Gson 사용 시)
    val priceType: String? = null,
    @SerializedName("sensorFormat") val sensorFormat: String?,
    val price: Long?,
    val condition: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("postUrl") val postUrl: String,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @SerializedName("salesCount") val salesCount: Int
)
