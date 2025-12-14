package com.philem.philem.domain.pricing.dto

import com.google.gson.annotations.SerializedName

data class ModelPriceSnapshot(
    val condition: String,
    val sold_year: Int,
    val sold_month: Int,
    val max_price: Long,
    val min_price: Long,
    val avg_price: Long,
    val sample_count: Int,
    @SerializedName("component_type") private val _componentType: String? = null
) {
    // API에서 component_type이 없으면 "body" 또는 "combined"로 추론
    val componentType: String
        get() = _componentType ?: "body"  // 기본값을 body로 설정 (단일 상품이 많으므로)
}

fun List<ModelPriceSnapshot>.forComponent(type: String): List<ModelPriceSnapshot> =
    filter { it.componentType == type }

fun List<ModelPriceSnapshot>.forGrade(grade: String): List<ModelPriceSnapshot> =
    filter { it.condition == grade }

data class ProductSet(
    val name: String,
    val combinedPrice: Long,      // 합본 가격
    val bodyPrice: Long,           // 바디 가격
    val lensPrice: Long,           // 렌즈 가격
    val grade: String = "B",       // 상품 등급 (A, B, C)

    // compare API 응답 필드
    val percentVsRef: Double = 0.0,    // 평균 대비 할인율 (%)
    val direction: String = "SAME",     // LOWER/HIGHER/SAME
    val refAvgPrice: Long = 0L,         // 기준 평균가
    val refYear: Int = 0,               // 기준 연도
    val refMonth: Int = 0,              // 기준 월
    val diffPrice: Long = 0L,           // 가격 차이 (input_price - ref_avg_price)

    val hasBody: Boolean = true,
    val hasLens: Boolean = false
) {
    // 선택한 구성에 따라 가격 반환
    fun getPriceFor(componentType: String): Long = when (componentType) {
        "body" -> bodyPrice
        "lens" -> lensPrice
        else -> combinedPrice
    }

    companion object {
        fun fromCompareResponse(
            modelName: String,
            condition: String,
            response: CompareResponse,
            componentSnapshots: List<ModelPriceSnapshot>
        ): ProductSet {
            return ProductSet(
                name = modelName,
                combinedPrice = response.inputPrice,
                bodyPrice = response.inputPrice,
                lensPrice = 0L,
                grade = condition,
                percentVsRef = response.percentVsRef,
                direction = response.direction,
                refAvgPrice = response.refAvgPrice,
                refYear = response.refYear,
                refMonth = response.refMonth,
                diffPrice = response.diffPrice,
                hasBody = true,
                hasLens = false
            )
        }

        fun fromBundleResponse(
            name: String,
            bundlePrice: Long,
            response: BundleCompareResponse,
            hasBody: Boolean,
            hasLens: Boolean
        ): ProductSet {
            val bodyItem = response.items.find { it.refAvgPrice > 0 }
            val lensItem = response.items.getOrNull(1)

            return ProductSet(
                name = name,
                combinedPrice = bundlePrice,
                bodyPrice = bodyItem?.refAvgPrice ?: 0L,
                lensPrice = lensItem?.refAvgPrice ?: 0L,
                grade = bodyItem?.condition ?: "B",
                percentVsRef = response.percentVsRef,
                direction = response.direction,
                refAvgPrice = response.refPrice,
                refYear = response.refYear,
                refMonth = response.refMonth,
                diffPrice = response.diffPrice,
                hasBody = hasBody,
                hasLens = hasLens
            )
        }
    }
}
