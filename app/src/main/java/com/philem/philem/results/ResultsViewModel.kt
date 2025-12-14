package com.philem.philem.results

import androidx.lifecycle.ViewModel
import com.philem.philem.data.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.lifecycle.viewModelScope
import com.philem.philem.data.repository.PricingRepository
import com.philem.philem.domain.pricing.dto.BundleCompareItem
import com.philem.philem.domain.pricing.dto.ModelPriceSnapshot
import com.philem.philem.domain.pricing.dto.ProductSet
import com.philem.philem.domain.pricing.dto.AnalyzeUrlResponse
import com.philem.philem.domain.pricing.dto.ListingItem
import com.philem.philem.domain.pricing.dto.ListingSummary
import com.philem.philem.domain.pricing.dto.RegionSearchResult
import kotlinx.coroutines.launch
import android.util.Log


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

    private val _userRegionName = MutableStateFlow("역삼동")
    val userRegionName: StateFlow<String> = _userRegionName.asStateFlow()

    private val _userRegionId = MutableStateFlow(6035L)

    private val _targetUrl = MutableStateFlow("")
    val targetUrl: StateFlow<String> = _targetUrl.asStateFlow()

    private val _analyzeResult = MutableStateFlow<AnalyzeUrlResponse?>(null)
    val analyzeResult: StateFlow<AnalyzeUrlResponse?> = _analyzeResult.asStateFlow()

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

    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _recommendations = MutableStateFlow<Map<String, List<ListingSummary>>>(emptyMap())
    val recommendations: StateFlow<Map<String, List<ListingSummary>>> = _recommendations.asStateFlow()

    private val _recommendationsLoading = MutableStateFlow(false)
    val recommendationsLoading: StateFlow<Boolean> = _recommendationsLoading.asStateFlow()

    private val _recommendationsError = MutableStateFlow<String?>(null)
    val recommendationsError: StateFlow<String?> = _recommendationsError.asStateFlow()

    private val _selectedRecommendationGrade = MutableStateFlow("B")
    val selectedRecommendationGrade: StateFlow<String> = _selectedRecommendationGrade.asStateFlow()

    private val _regionSearchQuery = MutableStateFlow("")
    val regionSearchQuery: StateFlow<String> = _regionSearchQuery.asStateFlow()

    private val _regionSearchResults = MutableStateFlow<List<RegionSearchResult>>(emptyList())
    val regionSearchResults: StateFlow<List<RegionSearchResult>> = _regionSearchResults.asStateFlow()

    private val _regionSearchLoading = MutableStateFlow(false)
    val regionSearchLoading: StateFlow<Boolean> = _regionSearchLoading.asStateFlow()

    private val _regionSearchError = MutableStateFlow<String?>(null)
    val regionSearchError: StateFlow<String?> = _regionSearchError.asStateFlow()

    private var lastRecommendationModelId: Long? = null
    private var lastRecommendationPreferredGrade: String = "B"

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

            // 1. 스냅샷 데이터 조회
            val snapshotsResult = repository.getSnapshots(modelId, months = 24)
            snapshotsResult.onSuccess { snapshots ->
                _priceSnapshots.value = snapshots
            }.onFailure { e ->
                _error.value = "스냅샷 로드 실패: ${e.message}"
                _priceSnapshots.value = emptyList()
            }

            // 2. 가격 비교 API 호출
            val compareResult = repository.comparePrice(modelId, condition, price)
            compareResult.onSuccess { compareResponse ->
                _productSet.value = ProductSet(
                    name = _modelName.value.ifBlank { "Unknown Model" },
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
                fetchRecommendationsForModel(modelId, condition)
            }.onFailure { e ->
                _error.value = "가격 비교 실패: ${e.message}"
                _productSet.value = null
            }

            _isLoading.value = false
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

            val bundleItems = items.map { (modelId, condition) ->
                BundleCompareItem(modelId, condition)
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
                _productSet.value = null
            }

            _isLoading.value = false
        }
    }

    fun selectGrade(grade: String) {
        _selectedGrade.value = grade
    }

    fun selectRecommendationGrade(grade: String) {
        _selectedRecommendationGrade.value = grade
    }

    fun updateModelName(name: String) {
        _modelName.value = name
    }

    fun updateUserRegion(regionId: Long, regionName: String) {
        val changed = regionId != _userRegionId.value
        _userRegionId.value = regionId
        _userRegionName.value = regionName
        if (changed) {
            retryRecommendations()
        }
    }

    fun setTargetUrl(url: String) {
        _targetUrl.value = url
    }

    fun retryRecommendations() {
        val modelId = lastRecommendationModelId ?: return
        fetchRecommendationsForModel(modelId, lastRecommendationPreferredGrade)
    }

    fun searchRegion(query: String) {
        _regionSearchQuery.value = query
        if (query.isBlank()) {
            _regionSearchResults.value = emptyList()
            _regionSearchError.value = null
            return
        }

        viewModelScope.launch {
            _regionSearchLoading.value = true
            _regionSearchError.value = null
            Log.d("ResultsViewModel", "[RegionSearch] query='$query'")

            val result = repository.searchRegions(query)
            result
                .onSuccess { list ->
                    Log.d("ResultsViewModel", "[RegionSearch] success size=${list.size}")
                    _regionSearchResults.value = list
                }
                .onFailure { throwable ->
                    Log.e("ResultsViewModel", "[RegionSearch] failure: ${throwable.message}", throwable)
                    _regionSearchResults.value = emptyList()
                    _regionSearchError.value = throwable.message
                }

            _regionSearchLoading.value = false
        }
    }

    fun selectRegion(region: RegionSearchResult) {
        updateUserRegion(region.id, region.name)
        _regionSearchQuery.value = region.name
        _regionSearchResults.value = emptyList()
        _regionSearchError.value = null
    }

    fun beginRegionSearch() {
        val seed = _userRegionName.value.takeIf { it.isNotBlank() && it != "내 동네" } ?: ""
        _regionSearchQuery.value = seed
        searchRegion(seed)
    }

    /**
     * URL 분석 후 가격 데이터 로드 (통합 플로우)
     */
    fun analyzeAndLoadPriceData(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _targetUrl.value = url

            Log.d("ResultsViewModel", "========== API 호출 시작 ==========")
            Log.d("ResultsViewModel", "1. URL 분석 요청: $url")

            // 1단계: URL 분석
            val analyzeResult = repository.analyzeUrl(url)
            analyzeResult
                .onSuccess { response ->
                    _analyzeResult.value = response

                    Log.d("ResultsViewModel", "1. URL 분석 성공:")
                    Log.d("ResultsViewModel", "   - listingId: ${response.listingId}")
                    Log.d("ResultsViewModel", "   - isBundle: ${response.isBundle}")
                    Log.d("ResultsViewModel", "   - bundleTotalPrice: ${response.bundleTotalPrice}")
                    Log.d("ResultsViewModel", "   - items 개수: ${response.items.size}")
                    response.items.forEachIndexed { index, item ->
                        Log.d("ResultsViewModel", "   - Item[$index]: modelId=${item.modelId}, name=${item.modelName}, role=${item.role}, condition=${item.condition}, price=${item.price}")
                    }

                    if (response.items.isEmpty()) {
                        _error.value = "URL 분석 결과에 상품이 없습니다."
                        _isLoading.value = false
                        return@onSuccess
                    }

                    if (response.isBundle) {
                        Log.d("ResultsViewModel", "번들 상품 처리 시작")
                        handleBundleProduct(response)
                    } else {
                        Log.d("ResultsViewModel", "단일 상품 처리 시작")
                        handleSingleProduct(response.items[0])
                    }
                }
                .onFailure { throwable ->
                    Log.e("ResultsViewModel", "1. URL 분석 실패: ${throwable.message}", throwable)
                    _error.value = "URL 분석 실패: ${throwable.message}"
                    _analyzeResult.value = null
                    _isLoading.value = false
                }
        }
    }

    private suspend fun handleSingleProduct(item: ListingItem) {
        _modelName.value = item.modelName

        Log.d("ResultsViewModel", "2. 단일 상품 스냅샷 요청:")
        Log.d("ResultsViewModel", "   - modelId: ${item.modelId}")
        Log.d("ResultsViewModel", "   - months: 24")

        // 단일 상품 스냅샷 조회
        val snapshotsResult = repository.getSnapshots(item.modelId, months = 24)
        snapshotsResult
            .onSuccess { snapshots ->
                Log.d("ResultsViewModel", "2. 스냅샷 조회 성공:")
                Log.d("ResultsViewModel", "   - 총 ${snapshots.size}건")

                // componentType별로 그룹화해서 로깅
                val grouped = snapshots.groupBy { it.componentType }
                grouped.forEach { (type, list) ->
                    Log.d("ResultsViewModel", "   - componentType='$type': ${list.size}건")
                    list.take(2).forEach { snap ->
                        Log.d("ResultsViewModel", "     ${snap.condition}급 ${snap.sold_year}.${snap.sold_month} avg=${snap.avg_price}")
                    }
                }

                // null 체크용 상세 로그
                if (snapshots.any { it.componentType == "body" }) {
                    Log.d("ResultsViewModel", "   ✅ 'body' 타입 데이터 존재")
                }
                if (snapshots.any { it.componentType == "combined" }) {
                    Log.d("ResultsViewModel", "   ✅ 'combined' 타입 데이터 존재")
                }

                _priceSnapshots.value = snapshots

                if (snapshots.isNotEmpty()) {
                    Log.d("ResultsViewModel", "3. 가격 비교 요청:")
                    Log.d("ResultsViewModel", "   - modelId: ${item.modelId}")
                    Log.d("ResultsViewModel", "   - condition: ${item.condition}")
                    Log.d("ResultsViewModel", "   - price: ${item.price}")

                    // 비교 API 호출
                    val compareResult = repository.comparePrice(
                        item.modelId,
                        item.condition,
                        item.price
                    )
                    compareResult
                        .onSuccess { compareResponse ->
                            Log.d("ResultsViewModel", "3. 가격 비교 성공:")
                            Log.d("ResultsViewModel", "   - available: ${compareResponse.available}")
                            Log.d("ResultsViewModel", "   - direction: ${compareResponse.direction}")
                            Log.d("ResultsViewModel", "   - percentVsRef: ${compareResponse.percentVsRef}")

                            _productSet.value = if (compareResponse.available) {
                                ProductSet.fromCompareResponse(
                                    modelName = item.modelName,
                                    condition = item.condition,
                                    response = compareResponse,
                                    componentSnapshots = snapshots
                                )
                            } else {
                                Log.d("ResultsViewModel", "   비교 데이터 없음, 기본 ProductSet 생성")
                                ProductSet(
                                    name = item.modelName,
                                    combinedPrice = item.price,
                                    bodyPrice = item.price,
                                    lensPrice = 0L,
                                    grade = item.condition,
                                    hasBody = item.role == "BODY",
                                    hasLens = item.role == "LENS"
                                )
                            }
                            fetchRecommendationsForModel(item.modelId, item.condition)
                        }
                        .onFailure { throwable ->
                            Log.e("ResultsViewModel", "3. 가격 비교 실패: ${throwable.message}", throwable)
                            _productSet.value = ProductSet(
                                name = item.modelName,
                                combinedPrice = item.price,
                                bodyPrice = item.price,
                                lensPrice = 0L,
                                grade = item.condition,
                                hasBody = item.role == "BODY",
                                hasLens = item.role == "LENS"
                            )
                            fetchRecommendationsForModel(item.modelId, item.condition)
                        }
                } else {
                    Log.e("ResultsViewModel", "2. 스냅샷 데이터 없음!")
                    _error.value = "해당 모델(ID: ${item.modelId})의 시세 데이터가 DB에 없습니다."
                }
            }
            .onFailure { throwable ->
                Log.e("ResultsViewModel", "2. 스냅샷 로드 실패: ${throwable.message}", throwable)
                _error.value = "스냅샷 로드 실패: ${throwable.message}"
                _priceSnapshots.value = emptyList()
            }

        _isLoading.value = false
        Log.d("ResultsViewModel", "========== API 호출 완료 ==========")
    }

    private suspend fun handleBundleProduct(response: AnalyzeUrlResponse) {
        val bodyItem = response.items.find { it.role == "BODY" }
        val lensItem = response.items.find { it.role == "LENS" }

        if (bodyItem == null || lensItem == null) {
            Log.e("ResultsViewModel", "번들 상품이지만 바디 또는 렌즈 정보 없음")
            Log.e("ResultsViewModel", "   - bodyItem: $bodyItem")
            Log.e("ResultsViewModel", "   - lensItem: $lensItem")
            _error.value = "번들 상품이지만 바디 또는 렌즈 정보가 없습니다."
            _isLoading.value = false
            return
        }

        _modelName.value = "${bodyItem.modelName} + ${lensItem.modelName}"

        Log.d("ResultsViewModel", "2. 바디 스냅샷 요청: modelId=${bodyItem.modelId}")
        Log.d("ResultsViewModel", "3. 렌즈 스냅샷 요청: modelId=${lensItem.modelId}")

        // 바디와 렌즈 스냅샷을 각각 조회
        val bodySnapshotsResult = repository.getSnapshots(bodyItem.modelId, months = 24)
        val lensSnapshotsResult = repository.getSnapshots(lensItem.modelId, months = 24)

        val bodySnapshots = bodySnapshotsResult.getOrNull() ?: emptyList()
        val lensSnapshots = lensSnapshotsResult.getOrNull() ?: emptyList()

        Log.d("ResultsViewModel", "2. 바디 스냅샷 결과: ${bodySnapshots.size}건")
        bodySnapshots.groupBy { it.componentType }.forEach { (type, list) ->
            Log.d("ResultsViewModel", "   - $type: ${list.size}건")
        }

        Log.d("ResultsViewModel", "3. 렌즈 스냅샷 결과: ${lensSnapshots.size}건")
        lensSnapshots.groupBy { it.componentType }.forEach { (type, list) ->
            Log.d("ResultsViewModel", "   - $type: ${list.size}건")
        }

        if (bodySnapshots.isEmpty() && lensSnapshots.isEmpty()) {
            Log.e("ResultsViewModel", "바디와 렌즈 모두 스냅샷 없음!")
            _error.value = "바디와 렌즈 모두 시세 데이터가 없습니다."
            _isLoading.value = false
            return
        }

        // 합본 스냅샷 생성 (바디 + 렌즈 가격 합산)
        val combinedSnapshots = createCombinedSnapshots(bodySnapshots, lensSnapshots, bodyItem.condition)

        Log.d("ResultsViewModel", "4. 합본 스냅샷 생성: ${combinedSnapshots.size}건")

        // 모든 스냅샷 병합 (합본 + 바디 + 렌즈)
        _priceSnapshots.value = combinedSnapshots + bodySnapshots + lensSnapshots

        Log.d("ResultsViewModel", "   전체 스냅샷: ${_priceSnapshots.value.size}건")
        _priceSnapshots.value.groupBy { it.componentType }.forEach { (type, list) ->
            Log.d("ResultsViewModel", "   - $type: ${list.size}건")
        }

        Log.d("ResultsViewModel", "5. 번들 비교 요청:")
        Log.d("ResultsViewModel", "   - bundlePrice: ${response.bundleTotalPrice}")
        Log.d("ResultsViewModel", "   - body: ${bodyItem.modelId}, ${bodyItem.condition}")
        Log.d("ResultsViewModel", "   - lens: ${lensItem.modelId}, ${lensItem.condition}")

        // 번들 비교 API 호출
        val bundleCompareResult = repository.compareBundlePrice(
            response.bundleTotalPrice,
            listOf(
                BundleCompareItem(bodyItem.modelId, bodyItem.condition),
                BundleCompareItem(lensItem.modelId, lensItem.condition)
            )
        )

        bundleCompareResult
            .onSuccess { compareResponse ->
                Log.d("ResultsViewModel", "5. 번들 비교 성공:")
                Log.d("ResultsViewModel", "   - available: ${compareResponse.available}")
                Log.d("ResultsViewModel", "   - direction: ${compareResponse.direction}")
                Log.d("ResultsViewModel", "   - percentVsRef: ${compareResponse.percentVsRef}")

                _productSet.value = ProductSet.fromBundleResponse(
                    name = _modelName.value,
                    bundlePrice = response.bundleTotalPrice,
                    response = compareResponse,
                    hasBody = true,
                    hasLens = true
                )
                fetchRecommendationsForModel(bodyItem.modelId, bodyItem.condition)
            }
            .onFailure { throwable ->
                Log.e("ResultsViewModel", "5. 번들 비교 실패: ${throwable.message}", throwable)
                // 번들 비교 실패해도 스냅샷으로 그래프는 표시
                _productSet.value = ProductSet(
                    name = _modelName.value,
                    combinedPrice = response.bundleTotalPrice,
                    bodyPrice = bodyItem.price,
                    lensPrice = lensItem.price,
                    grade = bodyItem.condition,
                    hasBody = true,
                    hasLens = true
                )
                fetchRecommendationsForModel(bodyItem.modelId, bodyItem.condition)
            }

        _isLoading.value = false
        Log.d("ResultsViewModel", "========== API 호출 완료 ==========")
    }

    /**
     * 바디와 렌즈 스냅샷을 합쳐 합본 스냅샷 생성
     */
    private fun createCombinedSnapshots(
        bodySnapshots: List<ModelPriceSnapshot>,
        lensSnapshots: List<ModelPriceSnapshot>,
        condition: String
    ): List<ModelPriceSnapshot> {
        val bodyMap = bodySnapshots
            .filter { it.condition == condition }
            .associateBy { "${it.sold_year}-${it.sold_month}" }
        val lensMap = lensSnapshots
            .filter { it.condition == condition }
            .associateBy { "${it.sold_year}-${it.sold_month}" }

        val allKeys = (bodyMap.keys + lensMap.keys).distinct()

        return allKeys.mapNotNull { key ->
            val body = bodyMap[key]
            val lens = lensMap[key]

            if (body != null && lens != null) {
                ModelPriceSnapshot(
                    condition = condition,
                    sold_year = body.sold_year,
                    sold_month = body.sold_month,
                    max_price = body.max_price + lens.max_price,
                    min_price = body.min_price + lens.min_price,
                    avg_price = body.avg_price + lens.avg_price,
                    sample_count = minOf(body.sample_count, lens.sample_count),
                    _componentType = "combined"
                )
            } else null
        }.sortedWith(compareBy({ it.sold_year }, { it.sold_month }))
    }

    private fun fetchRecommendationsForModel(modelId: Long, preferredGrade: String) {
        lastRecommendationModelId = modelId
        lastRecommendationPreferredGrade = preferredGrade

        viewModelScope.launch {
            _recommendationsLoading.value = true
            _recommendationsError.value = null

            val regionId = _userRegionId.value
            Log.d("ResultsViewModel", "[Recommendations] requesting model=$modelId region=$regionId grade=$preferredGrade")
            val result = repository.getRecommendations(modelId, regionId)
            result
                .onSuccess { response ->
                     val byCondition = response.byCondition ?: emptyMap()
                     _recommendations.value = byCondition
                     val gradesWithData = byCondition.filterValues { it.isNotEmpty() }.keys

                     val desiredGrade = when {
                         byCondition[_selectedRecommendationGrade.value]?.isNotEmpty() == true -> _selectedRecommendationGrade.value
                         byCondition[preferredGrade]?.isNotEmpty() == true -> preferredGrade
                         gradesWithData.isNotEmpty() -> gradesWithData.first()
                         else -> preferredGrade
                     }
                     _selectedRecommendationGrade.value = desiredGrade
                     if (response.userRegionId != _userRegionId.value) {
                         _userRegionId.value = response.userRegionId
                     }
                    Log.d("ResultsViewModel", "[Recommendations] success grades=${byCondition.keys} selected=$desiredGrade")
                 }
                .onFailure { throwable ->
                    Log.e("ResultsViewModel", "[Recommendations] failure: ${throwable.message}", throwable)
                     _recommendations.value = emptyMap()
                     _recommendationsError.value = throwable.message ?: "추천 불러오기 실패"
                 }

            _recommendationsLoading.value = false
        }
    }
}
