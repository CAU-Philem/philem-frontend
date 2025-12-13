package com.philem.philem.ui.theme

import androidx.compose.ui.graphics.Color

object PhilemColors {
    // Primary Colors
    val Primary = Color(0xFF1E88E5)
    val PrimaryDark = Color(0xFF1565C0)
    val PrimaryLight = Color(0xFF42A5F5)

    // Grade Colors
    val GradeA = Color(0xFF4CAF50)      // 초록 - 최상
    val GradeB = Color(0xFFFFC107)      // 노랑 - 상
    val GradeC = Color(0xFFFF5722)      // 빨강 - 중

    // Price Colors
    val PriceLower = Color(0xFF4CAF50)  // 저렴 - 초록
    val PriceHigher = Color(0xFFE53935) // 비쌈 - 빨강
    val PriceEqual = Color(0xFF9E9E9E)  // 동일 - 회색
    val MyPrice = Color(0xFF00BFA5)     // 내 가격 - 청록
    val AvgPrice = Color(0xFFE53935)    // 평균가 - 빨강

    // Background Colors
    val Background = Color(0xFFF8F9FA)
    val Surface = Color.White
    val CardBackground = Color.White

    // Text Colors
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
    val TextHint = Color(0xFFBDBDBD)

    // Accent Colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFE53935)
    val Info = Color(0xFF2196F3)

    // Chart Colors
    val ChartGrid = Color(0xFFEEEEEE)
    val ChartLine = Color(0xFF1E88E5)
    val ChartGradient = Color(0xFF1E88E5).copy(alpha = 0.1f)
}

