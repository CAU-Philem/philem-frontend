package com.philem.philem.results

import androidx.lifecycle.ViewModel
import com.philem.philem.data.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import androidx.lifecycle.viewModelScope
import com.philem.philem.data.mock.MockPriceData
import com.philem.philem.data.repository.PricingRepository
import com.philem.philem.domain.pricing.dto.ModelPriceSnapshot
import com.philem.philem.domain.pricing.dto.ProductSet
import kotlinx.coroutines.launch


/**
 * ResultsActivity를 위한 UI 상태
 */
data class ResultsUiState(
    val products: List<ProductItem> = emptyList(),
    val selectedGrade: String = "All",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ResultsActivity의 로직을 담당하는 ViewModel
 */
class ResultsViewModel : ViewModel() {

    private val repository = PricingRepository()

    private val _priceSnapshots = MutableStateFlow<List<ModelPriceSnapshot>>(emptyList())
    val priceSnapshots: StateFlow<List<ModelPriceSnapshot>> = _priceSnapshots.asStateFlow()

    private val _productSet = MutableStateFlow<ProductSet?>(null)
    val productSet: StateFlow<ProductSet?> = _productSet.asStateFlow()

    private val _selectedGrade = MutableStateFlow("A")
    val selectedGrade: StateFlow<String> = _selectedGrade.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 가격 데이터 로드 (실제 API 사용)
     * @param modelId 모델 ID
     * @param condition 상품 등급 (A, B, C)
     * @param price 사용자 입력 가격
     */
    fun loadPriceData(modelId: Long, condition: String = "B", price: Long = 950000L) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. 스냅샷 데이터 조회
                val snapshotsResult = repository.getSnapshots(modelId, months = 36)
                snapshotsResult.onSuccess { snapshots ->
                    _priceSnapshots.value = snapshots
                }.onFailure { e ->
                    _error.value = "스냅샷 로드 실패: ${e.message}"
                    // 실패 시 Mock 데이터 사용
                    _priceSnapshots.value = MockPriceData.getSonyA7M3Snapshots()
                }

                // 2. 가격 비교 API 호출
                val compareResult = repository.comparePrice(modelId, condition, price)
                compareResult.onSuccess { compareResponse ->
                    _productSet.value = ProductSet(
                        name = "Sony A7M3", // TODO: 실제 모델명으로 변경
                        combinedPrice = price,
                        bodyPrice = price,
                        lensPrice = 0L,
                        grade = condition,
                        percentVsRef = compareResponse.percentVsRef,
                        direction = compareResponse.direction,
                        refAvgPrice = compareResponse.refAvgPrice,
                        refYear = compareResponse.refYear,
                        refMonth = compareResponse.refMonth,
                        diffPrice = compareResponse.diffPrice,
                        hasBody = true,
                        hasLens = false
                    )
                }.onFailure { e ->
                    _error.value = "가격 비교 실패: ${e.message}"
                    // 실패 시 Mock 데이터 사용
                    _productSet.value = MockPriceData.getProductSet()
                }

            } catch (e: Exception) {
                _error.value = "데이터 로드 중 오류: ${e.message}"
                // 실패 시 Mock 데이터 사용
                _priceSnapshots.value = MockPriceData.getSonyA7M3Snapshots()
                _productSet.value = MockPriceData.getProductSet()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 번들 상품 가격 비교
     */
    fun loadBundlePriceData(
        bundlePrice: Long,
        items: List<Pair<Long, String>> // (modelId, condition) 리스트
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val bundleItems = items.map { (modelId, condition) ->
                    com.philem.philem.domain.pricing.dto.BundleCompareItem(modelId, condition)
                }

                val result = repository.compareBundlePrice(bundlePrice, bundleItems)
                result.onSuccess { bundleResponse ->
                    _productSet.value = ProductSet(
                        name = "Bundle Product",
                        combinedPrice = bundlePrice,
                        bodyPrice = bundleResponse.items.firstOrNull()?.refAvgPrice ?: 0L,
                        lensPrice = bundleResponse.items.getOrNull(1)?.refAvgPrice ?: 0L,
                        grade = "B",
                        percentVsRef = bundleResponse.percentVsRef,
                        direction = bundleResponse.direction,
                        refAvgPrice = bundleResponse.refPrice,
                        refYear = bundleResponse.refYear,
                        refMonth = bundleResponse.refMonth,
                        diffPrice = bundleResponse.diffPrice,
                        hasBody = true,
                        hasLens = true
                    )
                }.onFailure { e ->
                    _error.value = "번들 가격 비교 실패: ${e.message}"
                }

            } catch (e: Exception) {
                _error.value = "번들 데이터 로드 중 오류: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectGrade(grade: String) {
        _selectedGrade.value = grade
    }
}
//class ResultsViewModel : ViewModel() {
//
//    // 1. 모든 상품의 '원본' 목록 (이 목록은 변하지 않음)
//    private var allProducts: List<ProductItem> = emptyList()
//
//    // 2. UI 상태를 담는 변수
//    private val _uiState = MutableStateFlow(ResultsUiState())
//    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()
//
//    /**
//     * UI(Activity)가 처음 검색을 요청할 때 호출
//     */
//    fun searchProducts(query: String) {
//        // TODO: 나중에는 'query'로 서버에서 allProducts를 가져옴
//
//        // 지금은 그냥 '전체' 목록을 생성하고 UI 상태에 반영
//        allProducts = listOf(
//            ProductItem(1, "소니 A7M3 풀프레임 미...", "1,100,000원", "A"),
//            ProductItem(2, "소니 미러리스 카메라 A...", "1,000,000원", "A"),
//            ProductItem(3, "소니 A7C 카메라", "1,200,000원", "B"),
//            ProductItem(4, "SONY ZV-E10M2", "1,150,000원", "B"),
//            ProductItem(5, "소니 A7 (구형)", "450,000원", "C") // C등급 아이템 추가
//        )
//
//        _uiState.value = ResultsUiState(
//            products = allProducts, // 처음엔 필터링 안 된 전체 목록
//            selectedGrade = "All"
//        )
//    }
//
//    /**
//     * UI(Activity)가 'A', 'B', 'C' 버튼을 누를 때 호출할 함수
//     */
//    fun setGradeFilter(grade: String) {
//
//        // 현재 선택된 등급과 같은 버튼을 또 누르면 -> 필터 해제 ("All")
//        val newFilter = if (_uiState.value.selectedGrade == grade) "All" else grade
//
//        // 1. '전체' 목록에서 필터링
//        val filteredList = if (newFilter == "All") {
//            allProducts // "All"이면 전체 목록 반환
//        } else {
//            allProducts.filter { it.grade == newFilter } // A, B, C에 맞는 것만 필터링
//        }
//
//        // 2. UI 상태 업데이트
//        // .update { ... } 를 사용하면 기존 값을 안전하게 복사하며 변경 가능
//        _uiState.update { currentState ->
//            currentState.copy(
//                products = filteredList, // '필터링된 목록'으로 교체
//                selectedGrade = newFilter  // '선택된 등급' 상태 저장
//            )
//        }
//    }
//}