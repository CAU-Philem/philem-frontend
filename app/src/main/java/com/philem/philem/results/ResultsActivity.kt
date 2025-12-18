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
import androidx.core.net.toUri
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn

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
            val selectedItemType by viewModel.selectedItemType.collectAsState()
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
                                            },
                                            selectedItemType = selectedItemType,
                                            onToggleItemType = viewModel::toggleItemType,
                                            referencePrice = set.combinedPrice
                                        )

                                        Spacer(modifier = Modifier.height(32.dp))

                                        RelatedProductsSection(
                                            products = viewModel.relatedProducts.collectAsState().value,
                                            filters = viewModel.relatedProductFilters.collectAsState().value,
                                            isLoading = viewModel.relatedProductsLoading.collectAsState().value,
                                            error = viewModel.relatedProductsError.collectAsState().value,
                                            onFilterToggle = viewModel::toggleRelatedFilter,
                                            onClearFilters = viewModel::clearRelatedFilters,
                                            onApplyFilters = viewModel::applyFilters // ✅ 이 줄을 추가하세요!
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
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(
                                                0xFFFFF9C4
                                            )
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                "🔍 진단 정보",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (targetUrl.isNotBlank()) {
                                                Text(
                                                    "입력 URL:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(targetUrl, fontSize = 11.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            analyzeResult?.let { result ->
                                                Text(
                                                    "✅ URL 분석 성공",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF4CAF50)
                                                )
                                                Text(
                                                    "번들 여부: ${if (result.isBundle) "번들" else "단일"}",
                                                    fontSize = 12.sp
                                                )
                                                if (result.items.isNotEmpty()) {
                                                    result.items.forEachIndexed { index, item ->
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            "상품 ${index + 1}:",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            "  모델: ${item.modelName}",
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            "  모델ID: ${item.modelId}",
                                                            fontSize = 11.sp
                                                        )
                                                        Text("  역할: ${item.role}", fontSize = 11.sp)
                                                        Text(
                                                            "  등급: ${item.condition}",
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            "  가격: ${item.price}원",
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            } ?: run {
                                                Text(
                                                    "❌ URL 분석 실패",
                                                    fontSize = 12.sp,
                                                    color = Color.Red
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                "스냅샷 현황:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (snapshots.isEmpty()) {
                                                Text(
                                                    "❌ 스냅샷 데이터 없음",
                                                    fontSize = 12.sp,
                                                    color = Color.Red,
                                                    fontWeight = FontWeight.Bold
                                                )

                                            } else {
                                                val grouped: Map<String, List<ModelPriceSnapshot>> =
                                                    snapshots.groupBy { it.componentType }
                                                grouped.forEach { (type: String, list: List<ModelPriceSnapshot>) ->
                                                    Text("  $type: ${list.size}건", fontSize = 11.sp)
                                                    val byGrade: Map<String, List<ModelPriceSnapshot>> =
                                                        list.groupBy { it.condition }
                                                    byGrade.forEach { (grade: String, items: List<ModelPriceSnapshot>) ->
                                                        Text(
                                                            "    ${grade}급: ${items.size}건",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }

                                            if (error != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "에러:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    error ?: "",
                                                    fontSize = 11.sp,
                                                    color = Color.Red
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "💡 로그캣에서 'ResultsViewModel' 태그로 상세 로그를 확인하세요",
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
        // 여기에 삽입하세요!
        override fun onNewIntent(intent: Intent) {
            super.onNewIntent(intent)
            setIntent(intent) // 액티비티가 새로 생성되지 않으므로, 전달받은 새 인텐트로 교체해줍니다.

            val newUrl = intent.getStringExtra("target_url") ?: ""
            android.util.Log.d("ResultsActivity", "onNewIntent 호출됨 - 새 URL: '$newUrl'")

            if (newUrl.isNotBlank()) {
                // 새 URL이 들어오면 ViewModel을 통해 분석 프로세스를 다시 시작합니다.
                viewModel.analyzeAndLoadPriceData(newUrl)
            }
        }
// ResultsActivity.kt 내부에 추가

        internal fun showSelectionDialog(url: String) {
            val options = arrayOf("이 상품 분석하기", "당근에서 상품 확인하기")

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("원하는 동작을 선택해주세요")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // 분석하기
                            val intent = Intent(this, ResultsActivity::class.java).apply {
                                putExtra("target_url", url)
                                // 새 액티비티를 스택 상단에 올리거나, 기존 액티비티를 재사용하도록 설정 가능
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(intent)
                            // 현재 액티비티를 종료하고 새로 열고 싶다면 finish() 호출
                        }

                        1 -> { // 당근에서 상품 확인하기
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            }
                        }
                    }
                }
                .show()
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
    onRegionClick: () -> Unit,
    selectedItemType: String,
    onToggleItemType: () -> Unit,
    referencePrice: Long // [추가]
) {
    val selectedList = recommendations[selectedGrade].orEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // 양 끝 정렬
            verticalAlignment = Alignment.Top // 위쪽 기준 정렬
        ) {
            // 왼쪽: 텍스트 영역
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "${regionName} 근처", // 조금 줄여서 심플하게
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF191F28)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${productName} 시세 확인",
                    fontSize = 14.sp,
                    color = Color(0xFF4E5968)
                )
            }

            // 오른쪽: 지역 변경 버튼 (둥근 스타일)
            Surface(
                onClick = onRegionClick,
                shape = RoundedCornerShape(20.dp), // 둥근 알약 모양
                color = Color(0xFFE3F2FD), // 아주 연한 파란색 배경
                contentColor = Color(0xFF1E88E5) // 진한 파란색 텍스트/아이콘
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn, // 위치 아이콘 추가
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "지역 변경",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("A", "B", "C").forEach { grade ->
                    ModernGradeChip(
                        text = "${grade}급",
                        isSelected = selectedGrade == grade,
                        onClick = { onGradeSelected(grade) }
                    )
                }
            }

            // 2. 우측: 단품/번들 필터 (깔끔한 텍스트 버튼 스타일)
            // 기존 OutlinedButton 대신 더 심플하게 변경
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleItemType() }
                    .background(Color(0xFFF2F4F6)) // 아주 연한 회색 배경
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 필터 아이콘 느낌 (선택사항)
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = Color(0xFF4E5968),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (selectedItemType) {
                            "SINGLE" -> "단품만 보기"
                            "BUNDLE" -> "번들 포함"
                            else -> "전체 보기"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E5968) // 차분한 진회색
                    )
                }
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
                RecommendationList(list = selectedList, referencePrice = referencePrice) // [수정] referencePrice 전달
            }
        }
    }
}

@Composable
private fun RecommendationList(list: List<ListingSummary>, referencePrice: Long) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.listingSeq }) { item ->
            RecommendationCard(item, referencePrice = referencePrice) // [수정] referencePrice 전달
        }
    }
}

@Composable
private fun RecommendationCard(
    item: ListingSummary,
    referencePrice: Long
) {
    val context = LocalContext.current
    val activity = context as? ResultsActivity

    // 가격 차이 계산 로직 (동일)
    val itemPrice = item.price ?: 0L
    val priceDiff = itemPrice - referencePrice
    val percent = if (referencePrice > 0) {
        (kotlin.math.abs(priceDiff).toDouble() / referencePrice * 100).toInt()
    } else 0

    // 색상 팔레트 정의 (세련된 컬러)
    val priceColor = Color(0xFF191F28) // 진한 검정 (토스/당근 스타일)
    val subTextColor = Color(0xFF8B95A1) // 연한 회색
    val badgeGreen = Color(0xFFE8F5E9) // 아주 연한 초록 배경
    val badgeGreenText = Color(0xFF2E7D32) // 진한 초록 텍스트
    val badgeRed = Color(0xFFFFEBEE)   // 아주 연한 빨강 배경
    val badgeRedText = Color(0xFFC62828)   // 진한 빨강 텍스트

    val (badgeText, badgeBg, badgeTxtColor) = when {
        referencePrice <= 0 || itemPrice <= 0 -> Triple("", Color.Transparent, Color.Transparent)
        priceDiff < 0 -> Triple("▼ $percent%", badgeGreen, badgeGreenText)
        priceDiff > 0 -> Triple("▲ $percent%", badgeRed, badgeRedText)
        else -> Triple("-", Color(0xFFF5F5F5), Color(0xFF757575))
    }

    Card(
        modifier = Modifier
            .width(200.dp) // 너비를 살짝 줄여서 오밀조밀하게
            .padding(end = 12.dp) // 카드 간 간격
            .clickable { activity?.showSelectionDialog(item.postUrl) },
        shape = RoundedCornerShape(16.dp), // 둥글기 증가
        colors = CardDefaults.cardColors(containerColor = Color.White), // [핵심] 배경 흰색
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // [핵심] 살짝 그림자
    ) {
        Column {
            // 1. 이미지 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl ?: item.postUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 등급 뱃지 (심플하게 우측 상단)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${item.condition}급",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // 2. 텍스트 정보 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp) // 내부 여백 넉넉하게
            ) {
                // 가격 비교 뱃지 (텍스트 위로 올림)
                if (badgeText.isNotEmpty()) {
                    Text(
                        text = badgeText,
                        color = badgeTxtColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 가격 (가장 강조)
                Text(
                    text = "${item.price?.formatAsWon() ?: "가격 미정"}원",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = priceColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 지역 및 시간 (URL 제거하고 유용한 정보만)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.regionName ?: "지역정보 없음",
                        fontSize = 12.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // 2. 날짜 데이터 가공 (없으면 null 반환하도록 로직 처리 필요하지만, 여기선 간단히)
                    val dateText = item.updatedAt?.formatAsDate()

                    // 3. 날짜가 있을 때만 '·' 과 '날짜'를 그림
                    if (!dateText.isNullOrBlank()) {
                        Text(
                            text = " · ",
                            fontSize = 12.sp,
                            color = subTextColor
                        )

                        Text(
                            text = dateText,
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                }
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
                    placeholder = { Text("예: 역삼동, 서초동", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp), // 검색창 둥글게
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.height(300.dp)) {
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
                                            Text(
                                                "ID: ${region.id}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text(
                                            text = "선택",
                                            color = Color(0xFF1E88E5),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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

private fun String.formatAsDate(): String = runCatching {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val dateTime = OffsetDateTime.parse(this)
        dateTime.format(DateTimeFormatter.ofPattern("MM.dd"))
    } else {
        // Fallback for API < 26
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("MM.dd", java.util.Locale.getDefault())
        val date = inputFormat.parse(this.substringBefore('+').substringBefore('Z'))
        date?.let { outputFormat.format(it) } ?: this
    }
}.getOrElse { this }

// [수정] RelatedProductsSection 함수 전체 교체
@Composable
private fun RelatedProductsSection(
    products: List<com.philem.philem.domain.pricing.dto.RelatedProductItem>,
    filters: ResultsViewModel.RelatedProductFilters,
    isLoading: Boolean,
    error: String?,
    onFilterToggle: (String, String) -> Unit,
    onClearFilters: () -> Unit,
    onApplyFilters: () -> Unit // [추가] 매개변수
) {
    val totalFilterCount = filters.conditions.size +
            filters.brands.size +
            filters.cameraTypes.size +
            filters.mounts.size +
            filters.sensorFormats.size

    // 필터 패널 열림/닫힘 상태 관리
    var isFilterOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "연관 상품 추천",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                if (totalFilterCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($totalFilterCount)",
                        fontSize = 14.sp,
                        color = Color(0xFF1E88E5),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // [수정] 멀티 필터 지원 텍스트 -> 필터 토글 버튼
                OutlinedButton(
                    onClick = { isFilterOpen = !isFilterOpen },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isFilterOpen) Color(0xFFE3F2FD) else Color.Transparent,
                        contentColor = if (isFilterOpen) Color(0xFF1E88E5) else Color(0xFF616161)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isFilterOpen) Color(0xFF1E88E5) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = if (isFilterOpen) "필터 닫기" else "필터",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 초기화 버튼 (필요 시 유지, API 호출은 안 함)
                if (totalFilterCount > 0) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "초기화",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 고정 높이 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
        ) {
            // [수정] 왼쪽: 필터 메뉴 (isFilterOpen일 때만 보임)
            if (isFilterOpen) {
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp)
                ) {
                    // 필터 리스트 (스크롤 가능)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterSection(
                            title = "등급",
                            options = listOf("A", "B", "C"),
                            selected = filters.conditions,
                            onToggle = { value -> onFilterToggle("condition", value) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        FilterSection(
                            title = "브랜드",
                            options = listOf("Sony", "Nikon", "Canon", "Fujifilm", "Tamron", "Samyang", "Viltrox"),
                            selected = filters.brands,
                            onToggle = { value -> onFilterToggle("brand", value) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        PriceFilterSection(
                            minPrice = filters.minPrice,
                            maxPrice = filters.maxPrice,
                            onMinPriceChange = { value -> onFilterToggle("minPrice", value) },
                            onMaxPriceChange = { value -> onFilterToggle("maxPrice", value) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        FilterSection(
                            title = "카메라 타입",
                            options = listOf("Mirrorless", "DSLR"),
                            selected = filters.cameraTypes,
                            onToggle = { value -> onFilterToggle("cameraType", value) },
                            displayNames = mapOf("Mirrorless" to "미러리스", "DSLR" to "DSLR")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        FilterSection(
                            title = "센서 크기",
                            options = listOf("FULL_FRAME", "APS_C"),
                            selected = filters.sensorFormats,
                            onToggle = { value -> onFilterToggle("sensorFormat", value) },
                            displayNames = mapOf("FULL_FRAME" to "풀프레임", "APS_C" to "APS-C")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // [추가] 적용 버튼
                    Button(
                        onClick = {
                            onApplyFilters()     // API 요청
                            isFilterOpen = false // 필터 닫기
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5)
                        )
                    ) {
                        Text("적용", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
            }

            // 오른쪽: 상품 리스트
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("오류 발생", fontWeight = FontWeight.Bold)
                                Text(error, fontSize = 12.sp)
                            }
                        }
                    }

                    products.isEmpty() -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                            Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("상품이 없습니다.", color = Color.Gray)
                            }
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            products.forEach { product ->
                                RelatedProductCard(product)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    displayNames: Map<String, String> = emptyMap()
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF616161)
            )
            if (selected.isNotEmpty()) {
                Text(
                    text = "${selected.size}",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E88E5))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(option) } // 👈 여기 983라인 근처: onToggle 호출 확인
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option in selected,
                    onCheckedChange = { onToggle(option) }, // 👈 여기도 확인
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayNames[option] ?: option,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PriceFilterSection(
    minPrice: Long?,
    maxPrice: Long?,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit
) {
    var minPriceText by remember(minPrice) { mutableStateOf(minPrice?.toString() ?: "") }
    var maxPriceText by remember(maxPrice) { mutableStateOf(maxPrice?.toString() ?: "") }

    Column {
        Text(
            text = "가격 범위",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF616161)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 최소 가격
        OutlinedTextField(
            value = minPriceText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    minPriceText = newValue
                    onMinPriceChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("최소 가격", fontSize = 11.sp) },
            placeholder = { Text("예: 500000", fontSize = 11.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFBDBDBD)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 최대 가격
        OutlinedTextField(
            value = maxPriceText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    maxPriceText = newValue
                    onMaxPriceChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("최대 가격", fontSize = 11.sp) },
            placeholder = { Text("예: 2000000", fontSize = 11.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFBDBDBD)
            )
        )
    }
}

@Composable
private fun RelatedProductCard(product: com.philem.philem.domain.pricing.dto.RelatedProductItem) {
    val context = LocalContext.current
    val activity = context as? ResultsActivity
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 수정된 부분: 이제 activity 객체를 통해 dialog 호출이 가능합니다.
                activity?.showSelectionDialog(product.postUrl)
            },
//                runCatching {
//                    val intent = Intent(Intent.ACTION_VIEW, product.postUrl.toUri())
//                    context.startActivity(intent)
//                }
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFF2F4F6))
            ) {
                // 상품 썸네일 이미지 표시
                AsyncImage(
                    model = product.thumbnailUrl ?: product.postUrl,
                    contentDescription = product.modelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.img_main_camera),
                    placeholder = painterResource(id = R.drawable.img_main_camera)
                )

                Text(
                    text = "${product.condition}급",
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
                    text = product.modelName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF191F28)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${product.price?.formatAsWon() ?: "가격 미정"}원",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF191F28)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    product.cameraType?.let {
                        Text(
                            text = when(it) {
                                "MIRRORLESS" -> "미러리스"
                                "DSLR" -> "DSLR"
                                else -> it
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF8B95A1)
                        )
                    }

                    product.sensorFormat?.let {
                        Text(
                            text = when(it) {
                                "FULL_FRAME" -> "FF"
                                "APS_C" -> "APS-C"
                                else -> it
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF8B95A1)
                        )
                    }
                }
            }
        }
    }
}
    @Composable
    fun ModernGradeChip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        // 색상 정의
        val backgroundColor = if (isSelected) Color(0xFF191F28) else Color.White
        val contentColor = if (isSelected) Color.White else Color(0xFF8B95A1)
        val borderColor = if (isSelected) Color.Transparent else Color(0xFFE1E2E4)

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp), // 완전한 타원형 (알약 모양)
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor), // 선택 안됐을 때만 얇은 테두리
            modifier = Modifier.height(32.dp) // 높이 고정으로 통일감
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 16.dp) // 좌우 여백 넉넉히
            ) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }