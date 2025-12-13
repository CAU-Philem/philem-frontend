package com.philem.philem.results

import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philem.philem.domain.pricing.dto.ModelPriceSnapshot
import com.philem.philem.domain.pricing.dto.ProductSet
import kotlin.math.roundToInt

@Composable
fun PriceChart(
    snapshots: List<ModelPriceSnapshot>,
    productSet: ProductSet,
    modifier: Modifier = Modifier
) {
    var selectedComponent by remember { mutableStateOf("combined") }
    var touchedIndex by remember { mutableStateOf<Int?>(null) }
    var touchedGrade by remember { mutableStateOf<String?>(null) }

    // 초기 선택 등급을 상품의 등급으로 설정
    var currentSelectedGrade by remember { mutableStateOf(productSet.grade) }

    // 모든 등급의 데이터 준비
    val gradeDataMap = remember(snapshots, selectedComponent) {
        listOf("A", "B", "C").associateWith { grade ->
            val filtered = snapshots
                .filter { it.condition == grade && it.component_type == selectedComponent }
                .sortedWith(compareBy({ it.sold_year }, { it.sold_month }))
            interpolateMissingMonths(filtered)
        }
    }

    val currentGradeData = gradeDataMap[currentSelectedGrade] ?: emptyList()

    if (currentGradeData.isEmpty()) {
        EmptyChartPlaceholder(currentSelectedGrade, selectedComponent, modifier)
        return
    }

    val averagePrice = currentGradeData.map { it.avg_price }.average().toLong()
    val currentPrice = productSet.getPriceFor(selectedComponent)

    // 🆕 백엔드 API 응답 값 사용
    val discountPercent = kotlin.math.abs(productSet.percentVsRef.toInt())
    val discountDirection = productSet.direction


    Column(modifier = modifier.fillMaxWidth()) {
        // 🎨 개선된 상품 정보 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 상품명 & 등급 배지
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = productSet.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 등급 배지 (더 크고 눈에 띄게)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (productSet.grade) {
                                    "A" -> Color(0xFF4CAF50)
                                    "B" -> Color(0xFFFFC107)
                                    "C" -> Color(0xFFFF5722)
                                    else -> Color.Gray
                                },
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "${productSet.grade}급",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (productSet.grade == "B") Color.Black else Color.White
                                )
                            }
                            Text(
                                text = componentName(selectedComponent),
                                fontSize = 14.sp,
                                color = Color(0xFF757575),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 구분선
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color(0xFFEEEEEE),
                    thickness = 1.dp
                )

                // 가격 정보
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "내 상품 가격",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentPrice.formatPrice()}원",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = Color(0xFF1E88E5)
                        )
                    }

                    // 할인율 배지 (한 줄 표시)
                    when (discountDirection) {
                        "LOWER" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF4CAF50).copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "↓",
                                        fontSize = 22.sp,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${kotlin.math.abs(discountPercent)}%",
                                        fontSize = 20.sp,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "저렴",
                                        fontSize = 14.sp,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        "HIGHER" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFEBEE),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFE53935).copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "↑",
                                        fontSize = 22.sp,
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${kotlin.math.abs(discountPercent)}%",
                                        fontSize = 20.sp,
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "비쌈",
                                        fontSize = 14.sp,
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F5F5)
                            ) {
                                Text(
                                    text = "평균가",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 바디/합본/렌즈 선택 버튼
        if (productSet.hasLens) {
            ComponentSelector(
                selectedComponent = selectedComponent,
                onComponentSelected = { selectedComponent = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        MultiGradeChart(
            gradeDataMap = gradeDataMap,
            selectedGrade = currentSelectedGrade,
            productGrade = productSet.grade,
            productPrice = currentPrice,
            onTouchIndexChanged = { index, grade ->
                touchedIndex = index
                touchedGrade = grade
            },
            onGradeSelected = { grade ->
                currentSelectedGrade = grade
                touchedIndex = null
                touchedGrade = null
            },
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )

        touchedIndex?.let { index ->
            touchedGrade?.let { grade ->
                gradeDataMap[grade]?.getOrNull(index)?.let { snapshot ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${grade}급 - ${snapshot.sold_year}년 ${snapshot.sold_month}월",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "거래량: ${snapshot.sample_count}건",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun EmptyChartPlaceholder(
    selectedGrade: String,
    selectedComponent: String,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${selectedGrade}급 ${componentName(selectedComponent)} 시세 데이터가 없습니다",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}
@Composable
private fun ChartHeader(
    productGrade: String,  // 상품의 실제 등급
    currentSelectedGrade: String,
    productSet: ProductSet,
    selectedComponent: String,
    touchedIndex: Int?,
    interpolatedData: List<ModelPriceSnapshot>,
    discountPercent: Int,
    gradeColor: Color,
    touchedGrade: String?,
    gradeDataMap: Map<String, List<ModelPriceSnapshot>>
) {
    // 선택한 구성의 현재가 가져오기
    val currentPrice = productSet.getPriceFor(selectedComponent)

    // 세트 가격 계산 (바디 + 렌즈)
    val setPrice = if (productSet.hasBody && productSet.hasLens) {
        productSet.bodyPrice + productSet.lensPrice
    } else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "${productGrade}급 시세 분석",  // 항상 상품 등급 표시
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${productSet.name} - ${componentName(selectedComponent)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                // 터치 시 해당 등급의 가격 표시, 아니면 상품 가격 표시
                val displayPrice = if (touchedIndex != null && touchedGrade != null) {
                    gradeDataMap[touchedGrade]?.getOrNull(touchedIndex)?.avg_price
                } else {
                    currentPrice
                }

                Text(
                    text = "${displayPrice?.formatPrice() ?: "0"}원",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = gradeColor
                )

                // 저렴함 표시: 터치 시 해당 월 기준, 기본은 평균 기준
                // 상품 등급과 동일할 때만 표시
                if (touchedIndex != null && touchedGrade != null) {
                    // 드래그 중: 해당 월 기준으로 계산
                    if (touchedGrade == productGrade) {
                        gradeDataMap[touchedGrade]?.getOrNull(touchedIndex)?.let { snapshot ->
                            val monthlyDiscountPercent = ((snapshot.avg_price - currentPrice) * 100.0 / snapshot.avg_price).roundToInt()
                            if (monthlyDiscountPercent > 0) {
                                Text(
                                    text = "해당 월 대비 $monthlyDiscountPercent% 저렴",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (monthlyDiscountPercent < 0) {
                                Text(
                                    text = "해당 월 대비 ${-monthlyDiscountPercent}% 비쌈",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6F00),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // 기본 상태: 평균 기준으로 계산 (상품 등급과 현재 선택 등급이 동일할 때만)
                    if (currentSelectedGrade == productGrade) {
                        if (discountPercent > 0) {
                            Text(
                                text = "평균 대비 $discountPercent% 저렴",
                                fontSize = 12.sp,
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Medium
                            )
                        } else if (discountPercent < 0) {
                            Text(
                                text = "평균 대비 ${-discountPercent}% 비쌈",
                                fontSize = 12.sp,
                                color = Color(0xFFFF6F00),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 세트 가격 정보 (바디 + 렌즈가 있는 경우에만 표시)
        setPrice?.let { totalPrice ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "세트 가격",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "${productSet.bodyPrice.formatPrice()}원 + ${productSet.lensPrice.formatPrice()}원",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${totalPrice.formatPrice()}원",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )

                        // 세트 가격의 시세 대비 비교
                        val averageSetPrice = interpolatedData
                            .filter { it.component_type == "combined" }
                            .map { it.avg_price }
                            .takeIf { it.isNotEmpty() }
                            ?.average()

                        averageSetPrice?.let { avgPrice ->
                            val setPriceDiscountPercent = ((avgPrice - totalPrice) * 100.0 / avgPrice).roundToInt()
                            if (setPriceDiscountPercent > 0) {
                                Text(
                                    text = "시세 대비 $setPriceDiscountPercent% 저렴",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (setPriceDiscountPercent < 0) {
                                Text(
                                    text = "시세 대비 ${-setPriceDiscountPercent}% 비쌌",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF6F00),
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "시세와 동일",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ComponentSelector(
    selectedComponent: String,
    onComponentSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ComponentChip(
            text = "합본",
            selected = selectedComponent == "combined",
            onClick = { onComponentSelected("combined") }
        )
        ComponentChip(
            text = "바디",
            selected = selectedComponent == "body",
            onClick = { onComponentSelected("body") }
        )
        ComponentChip(
            text = "렌즈",
            selected = selectedComponent == "lens",
            onClick = { onComponentSelected("lens") }
        )
    }
}

@Composable
private fun ComponentChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF1976D2),
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun ChartFooter(snapshot: ModelPriceSnapshot, grade: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${grade}급 - ${snapshot.sold_year}년 ${snapshot.sold_month}월",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = "거래량: ${snapshot.sample_count}건",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
@Composable
private fun InteractivePriceChart(
    data: List<ModelPriceSnapshot>,
    averagePrice: Long,
    gradeColor: Color,
    onTouchIndexChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var touchedX by remember { mutableStateOf<Float?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchedX = offset.x
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchedX = change.position.x
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(2000)
                            touchedX = null
                            onTouchIndexChanged(null)
                        }
                    },
                    onDragCancel = {
                        touchedX = null
                        onTouchIndexChanged(null)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchedX = offset.x
                    coroutineScope.launch {
                        delay(1500)
                        touchedX = null
                        onTouchIndexChanged(null)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 60f
        val paddingRight = 20f
        val paddingTop = 20f
        val paddingBottom = 50f

        val minPrice = data.minOf { it.min_price }
        val maxPrice = data.maxOf { it.max_price }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1)

        fun priceToY(price: Long): Float {
            return height - paddingBottom - ((price - minPrice).toFloat() / priceRange * (height - paddingTop - paddingBottom))
        }

        fun xToIndex(x: Float): Int? {
            val step = (width - paddingLeft - paddingRight) / (data.size - 1).coerceAtLeast(1)
            val index = ((x - paddingLeft) / step).roundToInt()
            return index.takeIf { it in data.indices }
        }

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val yAxisTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        // Y축 가격 레이블 (3~4개 정도)
        val yLabelCount = 4
        for (i in 0..yLabelCount) {
            val price = minPrice + (priceRange * i / yLabelCount)
            val y = priceToY(price)
            val priceText = "${(price / 10000).toInt()}만"

            drawContext.canvas.nativeCanvas.drawText(
                priceText,
                paddingLeft - 10f,
                y + 8f,
                yAxisTextPaint
            )

            // 격자선
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        // X축 월 레이블 (일부만 표시)
        val xLabelStep = (data.size / 6).coerceAtLeast(1)
        data.forEachIndexed { index, snapshot ->
            if (index % xLabelStep == 0 || index == data.size - 1) {
                val x = paddingLeft + (width - paddingLeft - paddingRight) * index / (data.size - 1).coerceAtLeast(1)
                val monthText = "${snapshot.sold_month}월"

                drawContext.canvas.nativeCanvas.drawText(
                    monthText,
                    x,
                    height - 10f,
                    textPaint
                )
            }
        }

        // 평균가 기준선
        val avgY = priceToY(averagePrice)
        drawLine(
            color = Color(0xFFE53935),
            start = Offset(paddingLeft, avgY),
            end = Offset(width - paddingRight, avgY),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )

        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "평균 ${(averagePrice / 10000).toInt()}만원",
                width - paddingRight - 100f,
                avgY - 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 28f
                    isAntiAlias = true
                }
            )
        }

        // 가격 라인
        val path = Path()
        data.forEachIndexed { index, snapshot ->
            val x = paddingLeft + (width - paddingLeft - paddingRight) * index / (data.size - 1).coerceAtLeast(1)
            val y = priceToY(snapshot.avg_price)

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // 그라데이션
        val gradientPath = Path().apply {
            addPath(path)
            lineTo(width - paddingRight, height - paddingBottom)
            lineTo(paddingLeft, height - paddingBottom)
            close()
        }

        drawPath(
            path = gradientPath,
            brush = Brush.verticalGradient(
                listOf(gradeColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        drawPath(path = path, color = gradeColor, style = Stroke(width = 4f))

        // 터치 인터랙션
        touchedX?.let { x ->
            xToIndex(x)?.let { index ->
                onTouchIndexChanged(index)
                val touchX = paddingLeft + (width - paddingLeft - paddingRight) * index / (data.size - 1).coerceAtLeast(1)
                val touchY = priceToY(data[index].avg_price)

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(touchX, paddingTop),
                    end = Offset(touchX, height - paddingBottom),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )

                drawCircle(color = gradeColor, radius = 8f, center = Offset(touchX, touchY))
                drawCircle(color = Color.White, radius = 4f, center = Offset(touchX, touchY))
            }
        }
    }
}

@Composable
private fun MultiGradeChart(
    gradeDataMap: Map<String, List<ModelPriceSnapshot>>,
    selectedGrade: String,
    productGrade: String,
    productPrice: Long,
    onTouchIndexChanged: (Int?, String?) -> Unit,
    onGradeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var touchedX by remember { mutableStateOf<Float?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 상품 등급의 색상만 사용
    val productGradeColor = when (productGrade) {
        "A" -> Color(0xFF4CAF50)
        "B" -> Color(0xFFFFC107)
        "C" -> Color(0xFFFF5722)
        else -> Color.Gray
    }

    val gradeColors = mapOf(
        "A" to Color(0xFF4CAF50),
        "B" to Color(0xFFFFC107),
        "C" to Color(0xFFFF5722)
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchedX = offset.x
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchedX = change.position.x
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(2000)
                            touchedX = null
                            onTouchIndexChanged(null, null)
                        }
                    },
                    onDragCancel = {
                        touchedX = null
                        onTouchIndexChanged(null, null)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchedX = offset.x
                    coroutineScope.launch {
                        delay(1500)
                        touchedX = null
                        onTouchIndexChanged(null, null)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 80f
        val paddingRight = 80f
        val paddingTop = 60f
        val paddingBottom = 50f

        // 모든 등급의 min/max 가격 계산
        val allPrices = gradeDataMap.values.flatten()
        if (allPrices.isEmpty()) return@Canvas

        // 상품 등급의 평균가 계산
        val productGradeData = gradeDataMap[productGrade] ?: emptyList()
        val averagePrice = if (productGradeData.isNotEmpty()) {
            productGradeData.map { it.avg_price }.average().toLong()
        } else {
            allPrices.map { it.avg_price }.average().toLong()
        }

        val dataMinPrice = allPrices.minOf { it.min_price }
        val dataMaxPrice = allPrices.maxOf { it.max_price }

        // Y축 척도를 평균가 기준으로 조정 (평균가가 중간에 오도록)
        val priceRangeFromAvg = kotlin.math.max(
            kotlin.math.abs(dataMaxPrice - averagePrice),
            kotlin.math.abs(averagePrice - dataMinPrice)
        )
        val minPrice = (averagePrice - priceRangeFromAvg * 1.2).toLong().coerceAtLeast(0)
        val maxPrice = (averagePrice + priceRangeFromAvg * 1.2).toLong()
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1)

        fun priceToY(price: Long): Float {
            return height - paddingBottom - ((price - minPrice).toFloat() / priceRange * (height - paddingTop - paddingBottom))
        }

        fun xToIndex(x: Float, dataSize: Int): Int? {
            val step = (width - paddingLeft - paddingRight) / (dataSize - 1).coerceAtLeast(1)
            val index = ((x - paddingLeft) / step).roundToInt()
            return index.takeIf { it in 0 until dataSize }
        }

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val yAxisTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        // Y축 가격 레이블
        val yLabelCount = 5
        for (i in 0..yLabelCount) {
            val price = minPrice + (priceRange * i / yLabelCount)
            val y = priceToY(price)
            val priceText = "${(price / 10000).toInt()}만"

            drawContext.canvas.nativeCanvas.drawText(
                priceText,
                paddingLeft - 10f,
                y + 8f,
                yAxisTextPaint
            )

            // 격자선
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        // X축 월 레이블
        val representativeData = gradeDataMap[selectedGrade] ?: gradeDataMap.values.firstOrNull() ?: emptyList()

        if (representativeData.isNotEmpty()) {
            val xLabelStep = (representativeData.size / 6).coerceAtLeast(1)
            representativeData.forEachIndexed { index, snapshot ->
                if (index % xLabelStep == 0 || index == representativeData.size - 1) {
                    val x = paddingLeft + (width - paddingLeft - paddingRight) * index / (representativeData.size - 1).coerceAtLeast(1)
                    val monthText = "${snapshot.sold_month}월"

                    drawContext.canvas.nativeCanvas.drawText(
                        monthText,
                        x,
                        height - 10f,
                        textPaint
                    )
                }
            }
        }

        // 각 등급별 라인 그리기 (상품 등급 색상만 사용)
        listOf("A", "B", "C").forEach { grade ->
            val data = gradeDataMap[grade] ?: return@forEach
            if (data.isEmpty()) return@forEach

            val gradeColor = gradeColors[grade] ?: Color.Gray
            val isProductGrade = grade == productGrade

            // 상품 등급만 진하게, 나머지는 회색 반투명
            val lineColor = if (isProductGrade) gradeColor else Color.Gray.copy(alpha = 0.3f)
            val lineWidth = if (isProductGrade) 4f else 2f

            val path = Path()
            data.forEachIndexed { index, snapshot ->
                val x = paddingLeft + (width - paddingLeft - paddingRight) * index / (data.size - 1).coerceAtLeast(1)
                val y = priceToY(snapshot.avg_price)

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // 그라데이션 (상품 등급만)
            if (isProductGrade) {
                val gradientPath = Path().apply {
                    addPath(path)
                    lineTo(width - paddingRight, height - paddingBottom)
                    lineTo(paddingLeft, height - paddingBottom)
                    close()
                }

                drawPath(
                    path = gradientPath,
                    brush = Brush.verticalGradient(
                        listOf(productGradeColor.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
            }

            drawPath(path = path, color = lineColor, style = Stroke(width = lineWidth))
        }

        // 평균가 기준선 (상품 등급 기준)
        val avgY = priceToY(averagePrice)
        drawLine(
            color = Color(0xFFE53935).copy(alpha = 0.7f),
            start = Offset(paddingLeft, avgY),
            end = Offset(width - paddingRight, avgY),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )

        // 평균가 레이블
        drawContext.canvas.nativeCanvas.apply {
            val avgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(229, 57, 53)
                textSize = 22f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawText(
                "평균 ${(averagePrice / 10000).toInt()}만원",
                width - paddingRight - 10f,
                avgY - 8f,
                avgPaint
            )
        }

        // 내 상품 가격 라인 (초록색)
        val myPriceY = priceToY(productPrice)
        drawLine(
            color = Color(0xFF4CAF50).copy(alpha = 0.8f),
            start = Offset(paddingLeft, myPriceY),
            end = Offset(width - paddingRight, myPriceY),
            strokeWidth = 3f
        )

        // 내 상품 가격 레이블
        drawContext.canvas.nativeCanvas.apply {
            val myPricePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(76, 175, 80)
                textSize = 22f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
                isFakeBoldText = true
            }
            drawText(
                "내 가격 ${(productPrice / 10000).toInt()}만원",
                width - paddingRight - 10f,
                myPriceY + 25f,
                myPricePaint
            )
        }

        // 범례 그리기 (우측 상단)
        var legendY = paddingTop
        listOf("A", "B", "C").forEach { grade ->
            val gradeColor = gradeColors[grade] ?: Color.Gray
            val isProductGrade = grade == productGrade
            val alpha = if (isProductGrade) 1f else 0.3f

            val legendX = width - paddingRight + 10f

            // 라인
            drawLine(
                color = if (isProductGrade) gradeColor else Color.Gray.copy(alpha = alpha),
                start = Offset(legendX, legendY),
                end = Offset(legendX + 30f, legendY),
                strokeWidth = 3f
            )

            // 텍스트
            drawContext.canvas.nativeCanvas.drawText(
                "${grade}급",
                legendX + 40f,
                legendY + 5f,
                android.graphics.Paint().apply {
                    color = if (isProductGrade) {
                        android.graphics.Color.argb(
                            255,
                            android.graphics.Color.red(gradeColor.hashCode()),
                            android.graphics.Color.green(gradeColor.hashCode()),
                            android.graphics.Color.blue(gradeColor.hashCode())
                        )
                    } else {
                        android.graphics.Color.GRAY
                    }
                    this.alpha = (alpha * 255).toInt()
                    textSize = 24f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )

            legendY += 35f
        }

        // 터치 인터랙션
        touchedX?.let { x ->
            // 터치한 위치에서 가장 가까운 인덱스 찾기
            var closestIndex: Int? = null
            var closestDistance = Float.MAX_VALUE

            // 대표 데이터로 인덱스 찾기
            val representativeData = gradeDataMap[productGrade] ?: gradeDataMap.values.firstOrNull() ?: emptyList()

            xToIndex(x, representativeData.size)?.let { index ->
                val touchX = paddingLeft + (width - paddingLeft - paddingRight) * index / (representativeData.size - 1).coerceAtLeast(1)
                val distance = kotlin.math.abs(x - touchX)

                if (distance < closestDistance) {
                    closestDistance = distance
                    closestIndex = index
                }
            }

            closestIndex?.let { index ->
                onTouchIndexChanged(index, productGrade)

                val touchX = paddingLeft + (width - paddingLeft - paddingRight) * index / (representativeData.size - 1).coerceAtLeast(1)

                // 수직선
                drawLine(
                    color = productGradeColor.copy(alpha = 0.5f),
                    start = Offset(touchX, paddingTop),
                    end = Offset(touchX, height - paddingBottom),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )

                // 상품 등급 교차점만 표시
                gradeDataMap[productGrade]?.getOrNull(index)?.let { snapshot ->
                    val pointY = priceToY(snapshot.avg_price)

                    drawCircle(
                        color = productGradeColor,
                        radius = 10f,
                        center = Offset(touchX, pointY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = Offset(touchX, pointY)
                    )
                }

                // 모든 등급의 가격을 담은 통합 박스 (주식 차트 스타일)
                val boxPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(60, 0, 0, 0))
                }

                val borderPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(255, 200, 200, 200)
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 2f
                }

                // 박스 내용 준비
                val gradeInfoList = mutableListOf<Triple<String, Long, Boolean>>()
                listOf("A", "B", "C").forEach { grade ->
                    gradeDataMap[grade]?.getOrNull(index)?.let { snapshot ->
                        gradeInfoList.add(Triple(grade, snapshot.avg_price, grade == productGrade))
                    }
                }

                if (gradeInfoList.isNotEmpty()) {
                    // 텍스트 페인트
                    val labelPaint = android.graphics.Paint().apply {
                        textSize = 33f  // 22f * 1.5
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.LEFT
                    }

                    // 박스 크기 계산
                    val lineHeight = 52f  // 35f * 1.5
                    val boxPadding = 24f  // 16f * 1.5

                    var maxWidth = 0f
                    gradeInfoList.forEach { (grade, price, isProductGrade) ->
                        val text = "${grade}급 ${price.formatPrice()}원"
                        val bounds = android.graphics.Rect()
                        labelPaint.getTextBounds(text, 0, text.length, bounds)
                        maxWidth = kotlin.math.max(maxWidth, bounds.width().toFloat())
                    }

                    val boxWidth = maxWidth + boxPadding * 2 + 30f  // 20f -> 30f
                    val boxHeight = gradeInfoList.size * lineHeight + boxPadding * 2

                    // 박스 위치 계산 (오른쪽 우선, 넘치면 왼쪽)
                    val boxOffsetX = 25f
                    val boxX = if (touchX + boxOffsetX + boxWidth < width - paddingRight) {
                        touchX + boxOffsetX
                    } else {
                        touchX - boxOffsetX - boxWidth
                    }

                    // 박스를 수직선 중간에 배치
                    val centerY = (paddingTop + height - paddingBottom) / 2
                    val boxY = centerY - boxHeight / 2

                    // 박스 배경 그리기
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        boxX,
                        boxY,
                        boxX + boxWidth,
                        boxY + boxHeight,
                        12f,
                        12f,
                        boxPaint
                    )

                    // 박스 테두리
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        boxX,
                        boxY,
                        boxX + boxWidth,
                        boxY + boxHeight,
                        12f,
                        12f,
                        borderPaint
                    )

                    // 각 등급 정보 그리기
                    var currentY = boxY + boxPadding + 30f  // 20f -> 30f
                    gradeInfoList.forEach { (grade, price, isProductGrade) ->
                        val gradeColorForDisplay = gradeColors[grade] ?: Color.Gray

                        // 등급 색상 인디케이터 (작은 원)
                        val indicatorPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                255,
                                android.graphics.Color.red(gradeColorForDisplay.hashCode()),
                                android.graphics.Color.green(gradeColorForDisplay.hashCode()),
                                android.graphics.Color.blue(gradeColorForDisplay.hashCode())
                            )
                            isAntiAlias = true
                        }

                        drawContext.canvas.nativeCanvas.drawCircle(
                            boxX + boxPadding + 9f,  // 6f -> 9f
                            currentY - 9f,  // 6f -> 9f
                            9f,  // 6f -> 9f (인디케이터 원 크기)
                            indicatorPaint
                        )

                        // 텍스트
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = if (isProductGrade) 36f else 33f  // 24f/22f * 1.5
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.LEFT
                            isFakeBoldText = isProductGrade
                        }

                        val text = "${grade}급 ${price.formatPrice()}원"
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            boxX + boxPadding + 30f,  // 20f -> 30f
                            currentY,
                            textPaint
                        )

                        currentY += lineHeight
                    }
                }

                // 상품 등급의 할인율 표시 (맨 위에)
                // 드래그 시: 해당 월 시세 대비 계산 - 블록 화살표로 표현 (가로 배치)
                gradeDataMap[productGrade]?.getOrNull(index)?.let { snapshot ->
                    val monthlyPrice = snapshot.avg_price
                    val discountPercent = ((monthlyPrice - productPrice) * 100.0 / monthlyPrice).roundToInt()

                    if (discountPercent != 0) {
                        val isLower = discountPercent > 0
                        val percentValue = kotlin.math.abs(discountPercent)

                        // 화살표 + 퍼센트를 가로로 배치
                        val arrowIcon = if (isLower) "▼" else "▲"
                        val displayText = "$arrowIcon $percentValue%"

                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 42f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }

                        // 배경 박스 크기 계산
                        val textBounds = android.graphics.Rect()
                        textPaint.getTextBounds(displayText, 0, displayText.length, textBounds)

                        val boxPadding = 18f
                        val boxWidth = textBounds.width() + boxPadding * 2
                        val boxHeight = textBounds.height() + boxPadding * 2

                        val discountBgPaint = android.graphics.Paint().apply {
                            color = if (isLower) {
                                android.graphics.Color.rgb(129, 199, 132)  // 연한 초록 (#81C784)
                            } else {
                                android.graphics.Color.rgb(239, 154, 154)  // 연한 빨강 (#EF9A9A)
                            }
                            isAntiAlias = true
                            setShadowLayer(6f, 0f, 3f, android.graphics.Color.argb(60, 0, 0, 0))
                        }

                        val discountY = paddingTop + 10f

                        // 둥근 배경 박스
                        drawContext.canvas.nativeCanvas.drawRoundRect(
                            touchX - boxWidth / 2,
                            discountY,
                            touchX + boxWidth / 2,
                            discountY + boxHeight,
                            12f,
                            12f,
                            discountBgPaint
                        )

                        // 화살표 + 퍼센트 텍스트 (한 줄)
                        drawContext.canvas.nativeCanvas.drawText(
                            displayText,
                            touchX,
                            discountY + boxHeight / 2 + textBounds.height() / 2,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}


// 빈 달 보간 함수
private fun interpolateMissingMonths(snapshots: List<ModelPriceSnapshot>): List<ModelPriceSnapshot> {
    if (snapshots.size < 2) return snapshots

    val result = mutableListOf<ModelPriceSnapshot>()
    val sorted = snapshots.sortedWith(compareBy({ it.sold_year }, { it.sold_month }))

    for (i in 0 until sorted.size - 1) {
        val current = sorted[i]
        val next = sorted[i + 1]
        result.add(current)

        val monthDiff = (next.sold_year - current.sold_year) * 12 + (next.sold_month - current.sold_month)

        if (monthDiff > 1) {
            for (j in 1 until monthDiff) {
                val ratio = j.toFloat() / monthDiff
                val interpolatedPrice = (current.avg_price + (next.avg_price - current.avg_price) * ratio).toLong()

                var year = current.sold_year
                var month = current.sold_month + j
                while (month > 12) {
                    year++
                    month -= 12
                }

                result.add(
                    ModelPriceSnapshot(
                        condition = current.condition,
                        sold_year = year,
                        sold_month = month,
                        max_price = interpolatedPrice,
                        min_price = interpolatedPrice,
                        avg_price = interpolatedPrice,
                        sample_count = 0,
                        component_type = current.component_type
                    )
                )
            }
        }
    }

    result.add(sorted.last())
    return result
}

private fun componentName(type: String): String = when (type) {
    "combined" -> "합본"
    "body" -> "바디"
    "lens" -> "렌즈"
    else -> ""
}

private fun Long.formatPrice(): String = "%,d".format(this)
