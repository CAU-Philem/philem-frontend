package com.philem.philem.results

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.philem.philem.R
import com.philem.philem.data.model.ProductItem
import com.philem.philem.ui.theme.PhilemTheme

class ResultsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val searchQuery = intent.getStringExtra("query")?.replace("\n", "") ?: "검색어 없음"

        enableEdgeToEdge()
        setContent {
            PhilemTheme {
                val viewModel: ResultsViewModel = viewModel()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(key1 = searchQuery) {
                    viewModel.searchProducts(searchQuery)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ResultsScreen(
                        modifier = Modifier.padding(innerPadding),
                        searchQuery = searchQuery,
                        state = state,
                        onGradeSelected = { grade -> viewModel.setGradeFilter(grade) },
                        onBackClick = { finish() } // 뒤로가기 클릭 시 액티비티 종료
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    modifier: Modifier = Modifier,
    searchQuery: String,
    state: ResultsUiState,
    onGradeSelected: (String) -> Unit,
    onBackClick: () -> Unit // 뒤로가기 클릭 이벤트 핸들러
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
                            .clickable { onBackClick() } // 클릭 시 onBackClick 호출
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
                                .menuAnchor()
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
