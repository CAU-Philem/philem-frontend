package com.philem.philem.data.mock

import com.philem.philem.domain.pricing.dto.ModelPriceSnapshot
import com.philem.philem.domain.pricing.dto.ProductSet

object MockPriceData {

    fun getProductSet(): ProductSet {
        return ProductSet(
            name = "A7M3 + 렌즈 A",
            combinedPrice = 1_500_000,  // 합본 현재가
            bodyPrice = 1_300_000,       // 바디 현재가
            lensPrice = 250_000,         // 렌즈 현재가
            grade = "B",                 // 상품 등급

            // compare API 응답 필드 (Mock 데이터)
            percentVsRef = -9.52,        // 평균 대비 9.52% 저렴
            direction = "LOWER",         // 저렴함
            refAvgPrice = 1_430_000,     // 기준 평균가 (B급 최근 월)
            refYear = 2025,
            refMonth = 1,
            diffPrice = -130_000,        // 13만원 저렴

            hasBody = true,
            hasLens = true
        )
    }

    fun getSonyA7M3Snapshots(): List<ModelPriceSnapshot> {
        return listOf(
            // 합본 (A급)
            ModelPriceSnapshot("A", 2025, 1, 1_600_000, 1_400_000, 1_500_000, 5, "combined"),
            ModelPriceSnapshot("A", 2024, 12, 1_580_000, 1_380_000, 1_480_000, 7, "combined"),
            ModelPriceSnapshot("A", 2024, 10, 1_520_000, 1_320_000, 1_420_000, 8, "combined"),
            ModelPriceSnapshot("A", 2024, 8, 1_480_000, 1_280_000, 1_380_000, 10, "combined"),
            ModelPriceSnapshot("A", 2024, 6, 1_430_000, 1_230_000, 1_330_000, 9, "combined"),
            ModelPriceSnapshot("A", 2024, 4, 1_380_000, 1_180_000, 1_280_000, 6, "combined"),
            ModelPriceSnapshot("A", 2024, 2, 1_350_000, 1_150_000, 1_250_000, 5, "combined"),

            // 바디만 (A급)
            ModelPriceSnapshot("A", 2025, 1, 1_350_000, 1_250_000, 1_300_000, 5, "body"),
            ModelPriceSnapshot("A", 2024, 12, 1_330_000, 1_230_000, 1_280_000, 7, "body"),
            ModelPriceSnapshot("A", 2024, 11, 1_300_000, 1_200_000, 1_250_000, 6, "body"),
            ModelPriceSnapshot("A", 2024, 9, 1_250_000, 1_150_000, 1_200_000, 12, "body"),
            ModelPriceSnapshot("A", 2024, 7, 1_200_000, 1_100_000, 1_150_000, 11, "body"),
            ModelPriceSnapshot("A", 2024, 5, 1_150_000, 1_050_000, 1_100_000, 7, "body"),
            ModelPriceSnapshot("A", 2024, 3, 1_120_000, 1_020_000, 1_070_000, 8, "body"),

            // 렌즈 A (A급)
            ModelPriceSnapshot("A", 2025, 1, 280_000, 220_000, 250_000, 4, "lens"),
            ModelPriceSnapshot("A", 2024, 12, 270_000, 210_000, 240_000, 5, "lens"),
            ModelPriceSnapshot("A", 2024, 10, 260_000, 200_000, 230_000, 6, "lens"),
            ModelPriceSnapshot("A", 2024, 8, 250_000, 190_000, 220_000, 7, "lens"),
            ModelPriceSnapshot("A", 2024, 6, 240_000, 180_000, 210_000, 5, "lens"),
            ModelPriceSnapshot("A", 2024, 4, 230_000, 170_000, 200_000, 4, "lens"),
            ModelPriceSnapshot("A", 2024, 2, 220_000, 160_000, 190_000, 3, "lens"),

            // 합본 (B급)
            ModelPriceSnapshot("B", 2025, 1, 1_350_000, 1_250_000, 1_300_000, 4, "combined"),
            ModelPriceSnapshot("B", 2024, 12, 1_330_000, 1_230_000, 1_280_000, 6, "combined"),
            ModelPriceSnapshot("B", 2024, 10, 1_280_000, 1_180_000, 1_220_000, 5, "combined"),
            ModelPriceSnapshot("B", 2024, 8, 1_230_000, 1_130_000, 1_180_000, 8, "combined"),
            ModelPriceSnapshot("B", 2024, 6, 1_180_000, 1_080_000, 1_120_000, 7, "combined"),
            ModelPriceSnapshot("B", 2024, 4, 1_130_000, 1_030_000, 1_090_000, 5, "combined"),
            ModelPriceSnapshot("B", 2024, 2, 1_100_000, 1_000_000, 1_050_000, 4, "combined"),

            // 바디만 (B급)
            ModelPriceSnapshot("B", 2025, 1, 1_150_000, 1_050_000, 1_100_000, 4, "body"),
            ModelPriceSnapshot("B", 2024, 12, 1_130_000, 1_030_000, 1_080_000, 6, "body"),
            ModelPriceSnapshot("B", 2024, 11, 1_100_000, 1_000_000, 1_050_000, 5, "body"),
            ModelPriceSnapshot("B", 2024, 9, 1_050_000, 950_000, 1_000_000, 10, "body"),
            ModelPriceSnapshot("B", 2024, 7, 1_000_000, 900_000, 950_000, 9, "body"),
            ModelPriceSnapshot("B", 2024, 5, 950_000, 850_000, 900_000, 8, "body"),
            ModelPriceSnapshot("B", 2024, 3, 920_000, 820_000, 870_000, 6, "body"),

            // 렌즈 A (B급)
            ModelPriceSnapshot("B", 2025, 1, 230_000, 170_000, 200_000, 3, "lens"),
            ModelPriceSnapshot("B", 2024, 12, 220_000, 160_000, 190_000, 4, "lens"),
            ModelPriceSnapshot("B", 2024, 10, 210_000, 150_000, 180_000, 5, "lens"),
            ModelPriceSnapshot("B", 2024, 8, 200_000, 140_000, 170_000, 6, "lens"),
            ModelPriceSnapshot("B", 2024, 6, 190_000, 130_000, 160_000, 4, "lens"),
            ModelPriceSnapshot("B", 2024, 4, 180_000, 120_000, 150_000, 3, "lens"),
            ModelPriceSnapshot("B", 2024, 2, 170_000, 110_000, 140_000, 2, "lens"),

            // 합본 (C급)
            ModelPriceSnapshot("C", 2025, 1, 1_150_000, 1_050_000, 1_100_000, 3, "combined"),
            ModelPriceSnapshot("C", 2024, 12, 1_130_000, 1_030_000, 1_080_000, 5, "combined"),
            ModelPriceSnapshot("C", 2024, 10, 1_080_000, 980_000, 1_020_000, 4, "combined"),
            ModelPriceSnapshot("C", 2024, 8, 1_030_000, 930_000, 980_000, 6, "combined"),
            ModelPriceSnapshot("C", 2024, 6, 980_000, 880_000, 920_000, 5, "combined"),
            ModelPriceSnapshot("C", 2024, 4, 930_000, 830_000, 890_000, 4, "combined"),
            ModelPriceSnapshot("C", 2024, 2, 900_000, 800_000, 850_000, 3, "combined"),

            // 바디만 (C급)
            ModelPriceSnapshot("C", 2025, 1, 950_000, 850_000, 900_000, 3, "body"),
            ModelPriceSnapshot("C", 2024, 12, 930_000, 830_000, 880_000, 5, "body"),
            ModelPriceSnapshot("C", 2024, 11, 900_000, 800_000, 850_000, 4, "body"),
            ModelPriceSnapshot("C", 2024, 9, 850_000, 750_000, 800_000, 8, "body"),
            ModelPriceSnapshot("C", 2024, 7, 800_000, 700_000, 750_000, 7, "body"),
            ModelPriceSnapshot("C", 2024, 5, 750_000, 650_000, 700_000, 6, "body"),
            ModelPriceSnapshot("C", 2024, 3, 720_000, 620_000, 670_000, 5, "body"),

            // 렌즈 A (C급)
            ModelPriceSnapshot("C", 2025, 1, 200_000, 140_000, 170_000, 2, "lens"),
            ModelPriceSnapshot("C", 2024, 12, 190_000, 130_000, 160_000, 3, "lens"),
            ModelPriceSnapshot("C", 2024, 10, 180_000, 120_000, 150_000, 4, "lens"),
            ModelPriceSnapshot("C", 2024, 8, 170_000, 110_000, 140_000, 5, "lens"),
            ModelPriceSnapshot("C", 2024, 6, 160_000, 100_000, 130_000, 3, "lens"),
            ModelPriceSnapshot("C", 2024, 4, 150_000, 90_000, 120_000, 2, "lens"),
            ModelPriceSnapshot("C", 2024, 2, 140_000, 80_000, 110_000, 1, "lens")
        )
    }
}
