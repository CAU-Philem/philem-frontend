package com.philem.philem.results

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philem.philem.R
import com.philem.philem.data.model.ProductItem
import com.philem.philem.domain.pricing.dto.ModelPriceSnapshot
import com.philem.philem.domain.pricing.dto.ListingSummary
import com.philem.philem.domain.pricing.dto.RegionSearchResult
import com.philem.philem.ui.theme.PhilemTheme
import coil.compose.AsyncImage
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ResultsActivity : ComponentActivity() {

    private val viewModel: ResultsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUrl = intent.getStringExtra("target_url") ?: ""

        android.util.Log.d("ResultsActivity", "========== Activity 시작 ==========")
        android.util.Log.d("ResultsActivity", "받은 URL: '$targetUrl'")

        if (targetUrl.isNotBlank()) {
            // URL이 있으면 분석 API를 먼저 호출
            android.util.Log.d("ResultsActivity", "URL 분석 플로우 시작")
            viewModel.analyzeAndLoadPriceData(targetUrl)
        } else {
            // URL이 없으면 기존 방식 (테스트용)
            android.util.Log.d("ResultsActivity", "URL 없음 - 테스트 모드")
            val modelId = intent.getLongExtra("model_id", 1L)
            val modelName = intent.getStringExtra("model_name") ?: "Unknown Model"
            viewModel.updateModelName(modelName)
            viewModel.setTargetUrl(targetUrl)
            viewModel.loadPriceData(modelId = modelId)
        }

        setContent {
            val snapshots by viewModel.priceSnapshots.collectAsState()
            val productSet by viewModel.productSet.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()
            val analyzeResult by viewModel.analyzeResult.collectAsState()
            val targetUrl by viewModel.targetUrl.collectAsState()
            val recommendations by viewModel.recommendations.collectAsState()
            val recommendationsLoading by viewModel.recommendationsLoading.collectAsState()
            val recommendationsError by viewModel.recommendationsError.collectAsState()
            val selectedRecommendationGrade by viewModel.selectedRecommendationGrade.collectAsState()
            val regionName by viewModel.userRegionName.collectAsState()
            val modelName by viewModel.modelName.collectAsState()
            val regionSearchQuery by viewModel.regionSearchQuery.collectAsState()
            val regionSearchResults by viewModel.regionSearchResults.collectAsState()
            val regionSearchLoading by viewModel.regionSearchLoading.collectAsState()
            val regionSearchError by viewModel.regionSearchError.collectAsState()
            var showRegionSearch by remember { mutableStateOf(false) }


            PhilemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator()
                            }

                            productSet != null -> {
                                // 정상: 그래프 표시
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    productSet?.let { set ->
                                        PriceChart(
                                            snapshots = snapshots,
                                            productSet = set,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(32.dp))

                                        RecommendationSection(
                                            regionName = regionName,
                                            productName = set.name.ifBlank { modelName.ifBlank { "이 상품" } },
                                            recommendations = recommendations,
                                            selectedGrade = selectedRecommendationGrade,
                                            onGradeSelected = viewModel::selectRecommendationGrade,
                                            isLoading = recommendationsLoading,
                                            error = recommendationsError,
                                            onRetry = viewModel::retryRecommendations,
                                            onRegionClick = {
                                                viewModel.beginRegionSearch()
                                                showRegionSearch = true
                                            }
                                        )
                                     }
                                 }
                            }

                            else -> {
                                // 데이터 없음: 디버그 정보 표시
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "시세 데이터를 불러오지 못했습니다.",
                                        color = Color.Gray,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("🔍 진단 정보", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (targetUrl.isNotBlank()) {
                                                Text("입력 URL:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(targetUrl, fontSize = 11.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            analyzeResult?.let { result ->
                                                Text("✅ URL 분석 성공", fontSize = 12.sp, color = Color(0xFF4CAF50))
                                                Text("번들 여부: ${if (result.isBundle) "번들" else "단일"}", fontSize = 12.sp)
                                                if (result.items.isNotEmpty()) {
                                                    result.items.forEachIndexed { index, item ->
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("상품 ${index + 1}:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text("  모델: ${item.modelName}", fontSize = 11.sp)
                                                        Text("  모델ID: ${item.modelId}", fontSize = 11.sp)
                                                        Text("  역할: ${item.role}", fontSize = 11.sp)
                                                        Text("  등급: ${item.condition}", fontSize = 11.sp)
                                                        Text("  가격: ${item.price}원", fontSize = 11.sp)
                                                    }
                                                }
                                            } ?: run {
                                                Text("❌ URL 분석 실패", fontSize = 12.sp, color = Color.Red)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text("스냅샷 현황:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (snapshots.isEmpty()) {
                                                Text("❌ 스냅샷 데이터 없음",
                                                    fontSize = 12.sp,
                                                    color = Color.Red,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                val grouped: Map<String, List<ModelPriceSnapshot>> = snapshots.groupBy { it.componentType }
                                                grouped.forEach { (type: String, list: List<ModelPriceSnapshot>) ->
                                                    Text("  $type: ${list.size}건", fontSize = 11.sp)
                                                    val byGrade: Map<String, List<ModelPriceSnapshot>> = list.groupBy { it.condition }
                                                    byGrade.forEach { (grade: String, items: List<ModelPriceSnapshot>) ->
                                                        Text("    ${grade}급: ${items.size}건", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                }
                                            }

                                            if (error != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("에러:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(error ?: "", fontSize = 11.sp, color = Color.Red)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("💡 로그캣에서 'ResultsViewModel' 태그로 상세 로그를 확인하세요",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showRegionSearch) {
                        RegionSearchDialog(
                            query = regionSearchQuery,
                            results = regionSearchResults,
                            isLoading = regionSearchLoading,
                            error = regionSearchError,
                            onDismiss = { showRegionSearch = false },
                            onQueryChange = viewModel::searchRegion,
                            onRegionSelected = {
                                viewModel.selectRegion(it)
                                showRegionSearch = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==============================
// Composable 함수들
// ==============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    modifier: Modifier = Modifier,
    searchQuery: String,
    state: ResultsUiState,
    onGradeSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val regionOptions = listOf("전체 지역", "서울", "경기", "부산", "대구")
    var expanded by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf(regionOptions[0]) }

    val recommendedProducts = listOf(
        ProductItem(10, "캐논 EOS R6", "2,500,000원", "A"),
        ProductItem(11, "니콘 Z6 II", "2,300,000원", "B"),
        ProductItem(12, "파나소닉 루믹스 S5", "1,800,000원", "A"),
        ProductItem(13, "후지필름 X-T4", "1,900,000원", "C")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "뒤로가기",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBackClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = searchQuery,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Search, contentDescription = "검색", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(painterResource(id = R.drawable.ic_menu), contentDescription = "메뉴", modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "시세 그래프", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Image(
                    painter = painterResource(id = R.drawable.img_main_camera),
                    contentDescription = "시세 그래프",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedRegion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("지역 선택") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 14.sp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            regionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 14.sp) },
                                    onClick = {
                                        selectedRegion = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "에서 매물 찾기", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, softWrap = false)
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        GradeButton(
                            text = "A",
                            isSelected = state.selectedGrade == "A",
                            onClick = { onGradeSelected("A") }
                        )
                        GradeButton(
                            text = "B",
                            isSelected = state.selectedGrade == "B",
                            onClick = { onGradeSelected("B") }
                        )
                        GradeButton(
                            text = "C",
                            isSelected = state.selectedGrade == "C",
                            onClick = { onGradeSelected("C") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        items(state.products) { product ->
            ProductItemCard(product = product)
        }

        item(span = { GridItemSpan(2) }) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "이런 상품은 어때요?",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        items(recommendedProducts) { product ->
            ProductItemCard(product = product)
        }
    }
}

@Composable
fun GradeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isSelected) Color.DarkGray else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Black
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProductItemCard(product: ProductItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.img_main_camera),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = product.price, fontWeight = FontWeight.Bold, fontSize = 17.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (product.grade == "A") Color(0xFF4CAF50) else Color(0xFFFFC107)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = product.grade,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenPreview() {
    val fakeState = ResultsUiState(
        products = listOf(
            ProductItem(1, "소니 A7M3 풀프레임 미...", "1,100,000원", "A"),
            ProductItem(2, "소니 미러리스 카메라 A...", "1,000,000원", "A"),
            ProductItem(3, "소니 A7C 카메라", "1,200,000원", "B"),
            ProductItem(4, "SONY ZV-E10M2", "1,150,000원", "B")
        )
    )
    PhilemTheme {
        ResultsScreen(
            searchQuery = "Sony A7M3",
            state = fakeState,
            onGradeSelected = {},
            onBackClick = {}
        )
    }
}

@Composable
private fun RecommendationSection(
    regionName: String,
    productName: String,
    recommendations: Map<String, List<ListingSummary>>,
    selectedGrade: String,
    onGradeSelected: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onRegionClick: () -> Unit
) {
    val gradeOptions = listOf("A", "B", "C")
    val selectedList = recommendations[selectedGrade].orEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRegionClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${regionName}에서 ${productName} 확인하기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "동일 모델 다른 등급 추천 매물",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "지역 변경",
                color = Color(0xFF1E88E5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            gradeOptions.forEach { grade ->
                FilterChip(
                    selected = selectedGrade == grade,
                    onClick = { onGradeSelected(grade) },
                    label = { Text("${grade}급") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            error != null -> {
                RecommendationErrorCard(error = error, onRetry = onRetry)
            }

            selectedList.isEmpty() -> {
                RecommendationEmptyState()
            }

            else -> {
                RecommendationList(list = selectedList)
            }
        }
    }
}

@Composable
private fun RecommendationList(list: List<ListingSummary>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.listingSeq }) { item ->
            RecommendationCard(item)
        }
    }
}

@Composable
private fun RecommendationCard(item: ListingSummary) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.postUrl))
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFE0E0E0))
            ) {
                AsyncImage(
                    model = item.thumbnailUrl ?: item.postUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "${item.condition}급",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xAA000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${item.price?.formatAsWon() ?: "가격 미정"}원",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E88E5)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.postUrl,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "업데이트 ${item.updatedAt.formatAsDayTime()}",
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
private fun RecommendationErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("추천 데이터를 불러오지 못했습니다.", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            Spacer(modifier = Modifier.height(4.dp))
            Text(error, fontSize = 12.sp, color = Color(0xFF5D4037))
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}

@Composable
private fun RecommendationEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("해당 등급의 추천 매물이 없습니다.", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RegionSearchDialog(
    query: String,
    results: List<RegionSearchResult>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRegionSelected: (RegionSearchResult) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(text = "동 검색", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 역삼, 서초") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    error != null -> {
                        Text(text = error, color = Color.Red, fontSize = 12.sp)
                    }

                    results.isEmpty() -> {
                        Text(text = "검색 결과가 없습니다.", color = Color.Gray, fontSize = 12.sp)
                    }

                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(results) { region ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRegionSelected(region) }
                                        .background(Color(0xFFF5F5F5))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(region.name, fontWeight = FontWeight.Bold)
                                        Text("ID: ${region.id}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(text = "선택", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

private fun Long.formatAsWon(): String = String.format(java.util.Locale.getDefault(), "%,d", this)

private fun String.formatAsDayTime(): String = runCatching {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val dateTime = OffsetDateTime.parse(this)
        dateTime.format(DateTimeFormatter.ofPattern("MM.dd HH:mm"))
    } else {
        // Fallback for API < 26
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("MM.dd HH:mm", java.util.Locale.getDefault())
        val date = inputFormat.parse(this.substringBefore('+').substringBefore('Z'))
        date?.let { outputFormat.format(it) } ?: this
    }
}.getOrElse { this }
