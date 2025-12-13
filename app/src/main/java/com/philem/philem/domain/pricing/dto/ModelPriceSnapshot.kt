package com.philem.philem.domain.pricing.dto

data class ModelPriceSnapshot(
    val condition: String,
    val sold_year: Int,
    val sold_month: Int,
    val max_price: Long,
    val min_price: Long,
    val avg_price: Long,
    val sample_count: Int,
    val component_type: String = "combined"
)

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
}
