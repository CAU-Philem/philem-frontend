package com.philem.philem.results

import androidx.lifecycle.ViewModel
import com.philem.philem.data.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ResultsActivity를 위한 UI 상태
 */
data class ResultsUiState(
    val products: List<ProductItem> = emptyList(), // 화면에 '보여질' 상품 목록 (필터링됨)
    val selectedGrade: String = "All" // 현재 선택된 등급 필터 (기본값 "All")
)

/**
 * ResultsActivity의 로직을 담당하는 ViewModel
 */
class ResultsViewModel : ViewModel() {

    // 1. 모든 상품의 '원본' 목록 (이 목록은 변하지 않음)
    private var allProducts: List<ProductItem> = emptyList()

    // 2. UI 상태를 담는 변수
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    /**
     * UI(Activity)가 처음 검색을 요청할 때 호출
     */
    fun searchProducts(query: String) {
        // TODO: 나중에는 'query'로 서버에서 allProducts를 가져옴

        // 지금은 그냥 '전체' 목록을 생성하고 UI 상태에 반영
        allProducts = listOf(
            ProductItem(1, "소니 A7M3 풀프레임 미...", "1,100,000원", "A"),
            ProductItem(2, "소니 미러리스 카메라 A...", "1,000,000원", "A"),
            ProductItem(3, "소니 A7C 카메라", "1,200,000원", "B"),
            ProductItem(4, "SONY ZV-E10M2", "1,150,000원", "B"),
            ProductItem(5, "소니 A7 (구형)", "450,000원", "C") // C등급 아이템 추가
        )

        _uiState.value = ResultsUiState(
            products = allProducts, // 처음엔 필터링 안 된 전체 목록
            selectedGrade = "All"
        )
    }

    /**
     * UI(Activity)가 'A', 'B', 'C' 버튼을 누를 때 호출할 함수
     */
    fun setGradeFilter(grade: String) {

        // 현재 선택된 등급과 같은 버튼을 또 누르면 -> 필터 해제 ("All")
        val newFilter = if (_uiState.value.selectedGrade == grade) "All" else grade

        // 1. '전체' 목록에서 필터링
        val filteredList = if (newFilter == "All") {
            allProducts // "All"이면 전체 목록 반환
        } else {
            allProducts.filter { it.grade == newFilter } // A, B, C에 맞는 것만 필터링
        }

        // 2. UI 상태 업데이트
        // .update { ... } 를 사용하면 기존 값을 안전하게 복사하며 변경 가능
        _uiState.update { currentState ->
            currentState.copy(
                products = filteredList, // '필터링된 목록'으로 교체
                selectedGrade = newFilter  // '선택된 등급' 상태 저장
            )
        }
    }
}