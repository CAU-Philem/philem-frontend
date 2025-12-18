package com.philem.philem.results

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.philem.philem.data.model.ProductItem
import com.philem.philem.data.repository.PricingRepository
import com.philem.philem.domain.pricing.dto.AnalyzeUrlResponse
import com.philem.philem.domain.pricing.dto.BundleCompareItem
import com.philem.philem.domain.pricing.dto.ListingItem
import com.philem.philem.domain.pricing.dto.ListingSummary
import com.philem.philem.domain.pricing.dto.ModelPriceSnapshot
import com.philem.philem.domain.pricing.dto.ProductSet
import com.philem.philem.domain.pricing.dto.RegionSearchResult
import com.philem.philem.domain.pricing.dto.RelatedProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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
    // 2. 선택 가능한 옵션 리스트 정의 (클래스 멤버 변수로 추가)
    val availableUnitTypes = listOf("BODY", "LENS")

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

    val availableMounts = listOf(
        "Sony E",
        "Canon RF",
        "Canon EF",
        "Nikon Z",
        "Nikon F",
        "Fuji X",
        "L-Mount",
        "Micro Four Thirds"
    )

    // [추가] 번들 구성품의 이름을 저장할 변수
    private var bundleBodyName: String = ""
    private var bundleLensName: String = ""

    // [추가] 번들 상품일 때, 구성품들의 ID를 저장해둘 변수
    private var bundleBodyId: Long? = null
    private var bundleLensId: Long? = null
    private var bundleBodyCondition: String = "A"
    private var bundleLensCondition: String = "A"

    // [추가] 현재 선택된 컴포넌트 (combined, body, lens) - UI가 구독할 상태
    private val _selectedComponent = MutableStateFlow("combined")
    val selectedComponent: StateFlow<String> = _selectedComponent.asStateFlow()

    private var lastRecommendationModelId: Long? = null
    private var lastRecommendationPreferredGrade: String = "B"
    private var lastRecommendationIsBundle: Boolean = false

    private val _selectedItemType = MutableStateFlow("SINGLE")
    val selectedItemType: StateFlow<String> = _selectedItemType.asStateFlow()

    // 연관 제품 추천 상태
    private val _relatedProducts = MutableStateFlow<List<RelatedProductItem>>(emptyList())
    val relatedProducts: StateFlow<List<RelatedProductItem>> = _relatedProducts.asStateFlow()

    private val _relatedProductsLoading = MutableStateFlow(false)
    val relatedProductsLoading: StateFlow<Boolean> = _relatedProductsLoading.asStateFlow()

    private val _relatedProductsError = MutableStateFlow<String?>(null)
    val relatedProductsError: StateFlow<String?> = _relatedProductsError.asStateFlow()

    // 연관 제품 필터 상태
    data class RelatedProductFilters(
        val unitTypes: Set<String> = emptySet(), // BODY, LENS
        val conditions: Set<String> = emptySet(), // A, B, C
        val brands: Set<String> = emptySet(),
        val cameraTypes: Set<String> = emptySet(), // MIRRORLESS, DSLR
        val mounts: Set<String> = emptySet(),
        val sensorFormats: Set<String> = emptySet(), // FULL_FRAME, APS_C
        val minPrice: Long? = null,
        val maxPrice: Long? = null
    )

    private val _relatedProductFilters = MutableStateFlow(RelatedProductFilters())
    val relatedProductFilters: StateFlow<RelatedProductFilters> = _relatedProductFilters.asStateFlow()

    private var baseModelId: Long? = null
    private var baseUnitType: String? = null

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
        Log.d("ResultsViewModel", "[UpdateRegion] regionId=$regionId name='$regionName' current=${_userRegionId.value}")
        val changed = regionId != _userRegionId.value
        _userRegionId.value = regionId
        _userRegionName.value = regionName
        if (changed) {
            Log.d("ResultsViewModel", "[UpdateRegion] Region changed, retrying recommendations")
            retryRecommendations()
        } else {
            Log.d("ResultsViewModel", "[UpdateRegion] Region not changed, skipping retry")
        }
    }

    fun setTargetUrl(url: String) {
        _targetUrl.value = url
    }

    fun retryRecommendations() {
        val modelId = lastRecommendationModelId ?: return
        Log.d("ResultsViewModel", "[RetryRecommendations] modelId=$modelId grade=$lastRecommendationPreferredGrade")
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
        Log.d("ResultsViewModel", "[SelectRegion] selected id=${region.id} name='${region.name}'")
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

        Log.d("ResultsViewModel", "2. 단일 상품 스냅샷 요청: modelId=${item.modelId}, role=${item.role}")

        // 1. [핵심 수정] 역할에 따라 가격을 제자리에 배치합니다.
        // 기존 코드에서는 무조건 bodyPrice = item.price, lensPrice = 0L 이라서 렌즈 가격이 0원이었습니다.
        val inputBodyPrice = if (item.role == "BODY") item.price else 0L
        val inputLensPrice = if (item.role == "LENS") item.price else 0L

        // 2. 스냅샷 조회
        val snapshotsResult = repository.getSnapshots(item.modelId, months = 24)

        snapshotsResult.onSuccess { rawSnapshots ->
            Log.d("ResultsViewModel", "2. 스냅샷 조회 성공: ${rawSnapshots.size}건")

            // 단일 상품의 타입 결정 (API의 "BODY" -> "body", "LENS" -> "lens")
            val targetComponentType = if (item.role == "BODY") "body" else "lens"

            // 차트가 이 타입을 바라보도록 상태 업데이트
            _selectedComponent.value = targetComponentType

            // 번들 관련 변수 초기화
            bundleBodyId = null
            bundleLensId = null

            // 데이터 태깅
            val taggedSnapshots = rawSnapshots.map { it.copy(_componentType = targetComponentType) }
            _priceSnapshots.value = taggedSnapshots

            if (taggedSnapshots.isNotEmpty()) {
                val compareResult = repository.comparePrice(
                    item.modelId,
                    item.condition,
                    item.price
                )
                compareResult
                    .onSuccess { compareResponse ->
                        // [핵심 수정] .copy()를 사용하여 정확한 bodyPrice/lensPrice를 주입합니다.
                        val baseSet = if (compareResponse.available) {
                            ProductSet.fromCompareResponse(
                                modelName = item.modelName,
                                condition = item.condition,
                                response = compareResponse,
                                componentSnapshots = taggedSnapshots
                            )
                        } else {
                            ProductSet(
                                name = item.modelName,
                                combinedPrice = item.price,
                                bodyPrice = inputBodyPrice,
                                lensPrice = inputLensPrice,
                                grade = item.condition,
                                hasBody = item.role == "BODY",
                                hasLens = item.role == "LENS"
                            )
                        }

                        // 명시적으로 가격 덮어쓰기 (팩토리 메서드가 0으로 초기화했을 수 있으므로)
                        _productSet.value = baseSet.copy(
                            bodyPrice = inputBodyPrice,
                            lensPrice = inputLensPrice,
                            hasBody = item.role == "BODY",
                            hasLens = item.role == "LENS"
                        )

                        lastRecommendationIsBundle = false
                        fetchRecommendationsForModel(item.modelId, item.condition)
                        initializeRelatedProducts(item.modelId, item.role)
                    }
                    .onFailure { throwable ->
                        Log.e("ResultsViewModel", "3. 가격 비교 실패", throwable)
                        _productSet.value = ProductSet(
                            name = item.modelName,
                            combinedPrice = item.price,
                            // [핵심] 실패 시에도 올바른 변수를 사용
                            bodyPrice = inputBodyPrice,
                            lensPrice = inputLensPrice,
                            grade = item.condition,
                            hasBody = item.role == "BODY",
                            hasLens = item.role == "LENS"
                        )
                        lastRecommendationIsBundle = false
                        fetchRecommendationsForModel(item.modelId, item.condition)
                        initializeRelatedProducts(item.modelId, item.role)
                    }
            } else {
                Log.e("ResultsViewModel", "2. 스냅샷 데이터 없음!")
                _error.value = "해당 모델의 시세 데이터가 없습니다."
            }
        }.onFailure { throwable ->
            Log.e("ResultsViewModel", "2. 스냅샷 로드 실패", throwable)
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
            _error.value = "번들 구성품 정보가 부족합니다. (Body/Lens 식별 실패)"
            _isLoading.value = false
            return
        }

        // [수정] ID와 조건뿐만 아니라 '이름'도 저장합니다.
        bundleBodyId = bodyItem.modelId
        bundleLensId = lensItem.modelId
        bundleBodyCondition = bodyItem.condition
        bundleLensCondition = lensItem.condition
        // 추가된 부분
        bundleBodyName = bodyItem.modelName
        bundleLensName = lensItem.modelName


        // [추가] 초기 상태는 'combined' (합본)
        _selectedComponent.value = "combined"

        _modelName.value = "${bodyItem.modelName} + ${lensItem.modelName}"
        Log.d("ResultsViewModel", "[Bundle] 처리 시작: Body(${bodyItem.modelId}) + Lens(${lensItem.modelId})")

        // 1. 병렬로 API 호출 (async 사용)
        val bodyDeferred = viewModelScope.async { repository.getSnapshots(bodyItem.modelId, months = 24) }
        val lensDeferred = viewModelScope.async { repository.getSnapshots(lensItem.modelId, months = 24) }

        val bodyResult = bodyDeferred.await()
        val lensResult = lensDeferred.await()

        // 2. 데이터 가져오기
        val rawBodySnapshots = bodyResult.getOrNull() ?: emptyList()
        val rawLensSnapshots = lensResult.getOrNull() ?: emptyList()

        Log.d("ResultsViewModel", "[Bundle] 원본 스냅샷: Body=${rawBodySnapshots.size}건, Lens=${rawLensSnapshots.size}건")

        // 3. [핵심 수정] UI를 위해 componentType을 강제로 "body", "lens"로 통일 (태깅)
        // copy() 함수를 사용하려면 ModelPriceSnapshot이 data class여야 합니다. (이미 그렇습니다)
        val taggedBodySnapshots = rawBodySnapshots.map { it.copy(_componentType = "body") }
        val taggedLensSnapshots = rawLensSnapshots.map { it.copy(_componentType = "lens") }

        // 4. 합산 스냅샷 생성 ("combined" 태그는 createCombinedSnapshots 안에서 생성됨)
        val combinedSnapshots = createCombinedSnapshots(rawBodySnapshots, rawLensSnapshots, bodyItem.condition)
        Log.d("ResultsViewModel", "[Bundle] 합산 스냅샷: ${combinedSnapshots.size}건")

        // 5. [핵심 수정] 이제 태그가 명확하므로 3개를 다 합쳐서 보냅니다.
        // PriceChart.kt가 componentType으로 필터링하므로 이제 섞이지 않습니다.
        _priceSnapshots.value = combinedSnapshots + taggedBodySnapshots + taggedLensSnapshots

        // 6. 데이터 유효성 경고 로그
        if (taggedBodySnapshots.isEmpty()) Log.e("ResultsViewModel", "⚠️ 바디 시세 데이터 없음")
        if (taggedLensSnapshots.isEmpty()) Log.e("ResultsViewModel", "⚠️ 렌즈 시세 데이터 없음")

        if (taggedBodySnapshots.isEmpty() && taggedLensSnapshots.isEmpty()) {
            _error.value = "시세 데이터가 없습니다."
            _isLoading.value = false
            return
        }

        // 7. 번들 가격 비교 API 호출
        val bundleCompareResult = repository.compareBundlePrice(
            response.bundleTotalPrice,
            listOf(
                BundleCompareItem(bodyItem.modelId, bodyItem.condition),
                BundleCompareItem(lensItem.modelId, lensItem.condition)
            )
        )

        bundleCompareResult
            .onSuccess { compareResponse ->
                val baseSet = ProductSet.fromBundleResponse(
                    name = _modelName.value,
                    bundlePrice = response.bundleTotalPrice,
                    response = compareResponse,
                    hasBody = true,
                    hasLens = true
                )
                // 2. [핵심 수정] 여기에 URL 분석에서 나온 '진짜 내 가격'을 덮어씌웁니다.
                // 이렇게 해야 그래프 상단에 평균가가 아닌 "내 상품 가격"이 뜹니다.
                _productSet.value = baseSet.copy(
                    bodyPrice = bodyItem.price, // URL 분석 결과의 바디 가격
                    lensPrice = lensItem.price  // URL 분석 결과의 렌즈 가격
                )

                lastRecommendationIsBundle = true
                fetchRecommendationsForModel(bodyItem.modelId, bodyItem.condition)
            }
            .onFailure {
                // 실패 시 기본 정보 표시
                _productSet.value = ProductSet(
                    name = _modelName.value,
                    combinedPrice = response.bundleTotalPrice,
                    bodyPrice = bodyItem.price,
                    lensPrice = lensItem.price,
                    grade = bodyItem.condition,
                    hasBody = true,
                    hasLens = true
                )
                lastRecommendationIsBundle = true
                fetchRecommendationsForModel(bodyItem.modelId, bodyItem.condition)
            }

        // 마지막에 한 번 호출 (기본값: 바디 기준)
        // 사용자가 "번들 옵션 -> 바디 조회만"을 원하셨으므로, combined일 때도 바디 ID로 조회합니다.
        fetchRecommendationsForModel(bodyItem.modelId, bodyItem.condition)
        initializeRelatedProducts(bodyItem.modelId, "BODY") // 초기 연관 상품: 바디

        _isLoading.value = false
    }

    /**
     * [신규] UI에서 탭(합본/바디/렌즈)을 눌렀을 때 호출하는 함수
     */
    fun selectComponentTab(componentType: String) {
        _selectedComponent.value = componentType

        // 번들 모드가 아니면(단일 상품이면) 아무것도 안 함
        if (bundleBodyId == null || bundleLensId == null) return

        val targetModelId: Long
        val targetCondition: String
        val targetUnitType: String

        // 사용자 요청 로직 반영:
        // 1. 번들(Combined) -> 바디 ID 사용 (기본)
        // 2. 바디(Body) -> 바디 ID 사용
        // 3. 렌즈(Lens) -> 렌즈 ID 사용
        when (componentType) {
            "lens" -> {
                targetModelId = bundleLensId!!
                targetCondition = bundleLensCondition
                targetUnitType = "LENS"
                // [추가] 화면 이름을 렌즈 이름으로 변경
                _modelName.value = bundleLensName
            }
            "body" -> {
                targetModelId = bundleBodyId!!
                targetCondition = bundleBodyCondition
                targetUnitType = "BODY"
                // [추가] 화면 이름을 바디 이름으로 변경
                _modelName.value = bundleBodyName
            }
            else -> { // combined
                targetModelId = bundleBodyId!!
                targetCondition = bundleBodyCondition
                targetUnitType = "BODY"
                // [추가] 화면 이름을 다시 번들 전체 이름으로 복구
                _modelName.value = "$bundleBodyName + $bundleLensName"
            }
        }

        Log.d("ResultsViewModel", "[TabChange] $componentType 선택됨 -> ModelID: $targetModelId 조회 시작")

        // 1. 추천 매물 새로고침 (근처 시세)
        fetchRecommendationsForModel(targetModelId, targetCondition)

        // 2. 연관 상품 새로고침
        // 기존 필터를 유지할지, 초기화할지는 선택 사항입니다. 여기서는 깔끔하게 초기화합니다.
        initializeRelatedProducts(targetModelId, targetUnitType)
    }

    private fun createCombinedSnapshots(
        bodySnapshots: List<ModelPriceSnapshot>,
        lensSnapshots: List<ModelPriceSnapshot>,
        condition: String
    ): List<ModelPriceSnapshot> {
        // 날짜 키 생성 (예: "2024-12")
        val bodyMap = bodySnapshots
            .filter { it.condition == condition }
            .associateBy { "${it.sold_year}-${it.sold_month}" }

        val lensMap = lensSnapshots
            .filter { it.condition == condition }
            .associateBy { "${it.sold_year}-${it.sold_month}" }

        // 두 데이터 중 하나라도 있는 날짜들을 모두 모음
        // (교집합만 하려면 bodyMap.keys.intersect(lensMap.keys) 사용)
        val allKeys = (bodyMap.keys + lensMap.keys).distinct()

        val result = allKeys.mapNotNull { key ->
            val body = bodyMap[key]
            val lens = lensMap[key]

            // [중요] 바디와 렌즈가 모두 있는 달만 합산합니다.
            // 하나만 있는 달도 그래프에 표시하고 싶다면 로직을 수정해야 합니다. (아래 주석 참조)
            if (body != null && lens != null) {
                ModelPriceSnapshot(
                    condition = condition,
                    sold_year = body.sold_year,
                    sold_month = body.sold_month,
                    max_price = body.max_price + lens.max_price,
                    min_price = body.min_price + lens.min_price,
                    avg_price = body.avg_price + lens.avg_price,
                    sample_count = minOf(body.sample_count, lens.sample_count),
                    _componentType = "combined" // UI에서 구분할 수 있게 태그
                )
            } else {
                // 데이터가 한쪽만 있는 경우 버림 (그래프 왜곡 방지)
                // 만약 한쪽만 있어도 보여주려면 여기서 body?.avg_price ?: 0 처럼 처리 가능하지만 추천하지 않음.
                null
            }
        }.sortedWith(compareBy({ it.sold_year }, { it.sold_month }))

        return result
    }

    fun toggleItemType() {
        _selectedItemType.value = when (_selectedItemType.value) {
            "SINGLE" -> "BUNDLE"
            "BUNDLE" -> "SINGLE"
            else -> "SINGLE"
        }
        retryRecommendations()
    }

    fun toggleRelatedFilter(filterType: String, value: String) {
        val current = _relatedProductFilters.value
        _relatedProductFilters.value = when (filterType) {
            "unitType" -> current.copy(
                unitTypes = if (value in current.unitTypes) current.unitTypes - value else current.unitTypes + value
            )
            "condition" -> current.copy(
                conditions = if (value in current.conditions) current.conditions - value else current.conditions + value
            )
            "brand" -> current.copy(
                brands = if (value in current.brands) current.brands - value else current.brands + value
            )
            "cameraType" -> current.copy(
                cameraTypes = if (value in current.cameraTypes) current.cameraTypes - value else current.cameraTypes + value
            )
            "mount" -> current.copy(
                mounts = if (value in current.mounts) current.mounts - value else current.mounts + value
            )
            "sensorFormat" -> current.copy(
                sensorFormats = if (value in current.sensorFormats) current.sensorFormats - value else current.sensorFormats + value
            )
            "minPrice" -> current.copy(
                minPrice = value.toLongOrNull()
            )
            "maxPrice" -> current.copy(
                maxPrice = value.toLongOrNull()
            )
            else -> current
        }
    }

    fun clearRelatedFilters() {
        _relatedProductFilters.value = RelatedProductFilters()
    }

    fun applyFilters() {
        fetchRelatedProducts()
    }

    fun initializeRelatedProducts(modelId: Long, unitType: String) {
        baseModelId = modelId
        baseUnitType = unitType

        // 필터 초기화
        _relatedProductFilters.value = RelatedProductFilters()

        Log.d("ResultsViewModel", "[RelatedProducts] 초기화: modelId=$modelId, unitType=$unitType")
        fetchRelatedProducts()
    }

    private fun fetchRelatedProducts() {
        val modelId = baseModelId ?: return
        val currentUnitType = baseUnitType ?: return

        viewModelScope.launch {
            _relatedProductsLoading.value = true
            _relatedProductsError.value = null

            val filters = _relatedProductFilters.value

            val selectedUnitType = filters.unitTypes.firstOrNull()
            val targetUnitType = selectedUnitType ?: (if (currentUnitType == "BODY") "LENS" else "BODY")

            val hasFilters = filters.unitTypes.isNotEmpty() ||
                    filters.conditions.isNotEmpty() ||
                    filters.brands.isNotEmpty() ||
                    filters.cameraTypes.isNotEmpty() ||
                    filters.mounts.isNotEmpty() ||
                    filters.sensorFormats.isNotEmpty() ||
                    filters.minPrice != null ||
                    filters.maxPrice != null

            val mode = if (hasFilters) {
                "FILTER"
            } else {
                if (targetUnitType == "LENS") "BODY_TO_LENS" else "LENS_TO_BODY"
            }

            // FILTER 모드일 때는 ID를 보내지 않음 (null 처리)
            val reqBodyModelId: Long?
            val reqLensModelId: Long?

            if (mode == "FILTER") {
                reqBodyModelId = null
                reqLensModelId = null
            } else {
                reqBodyModelId = if (currentUnitType == "BODY" && targetUnitType == "LENS") modelId else null
                reqLensModelId = if (currentUnitType == "LENS" && targetUnitType == "BODY") modelId else null
            }

            // 값 매핑 (UI String -> API 규격)
            // 브랜드, 카메라 타입: 대소문자 그대로 (Sony, Mirrorless)
            // 마운트: 약어 매핑 (Sony E -> E)
            val apiBrands = filters.brands.map { mapToApiValue("brand", it) }.ifEmpty { null }
            val apiCameraTypes = filters.cameraTypes.map { mapToApiValue("cameraType", it) }.ifEmpty { null }
            val apiMounts = filters.mounts.map { mapToApiValue("mount", it) }.ifEmpty { null }
            val apiSensorFormats = filters.sensorFormats.map { mapToApiValue("sensorFormat", it) }.ifEmpty { null }
            val apiConditions = filters.conditions.toList().ifEmpty { null }

            Log.d("ResultsViewModel", "[RelatedProducts] API 요청 준비: mode=$mode, target=$targetUnitType")
            Log.d("ResultsViewModel", "  -> Brands: $apiBrands")
            Log.d("ResultsViewModel", "  -> Mounts: $apiMounts")

            val request = com.philem.philem.domain.pricing.dto.RelatedProductsRequest(
                mode = mode,
                conditions = apiConditions,
                minPrice = filters.minPrice,
                maxPrice = filters.maxPrice,
                brands = apiBrands,
                unitType = targetUnitType,
                cameraTypes = apiCameraTypes,
                mounts = apiMounts,
                sensorFormats = apiSensorFormats,
                bodyModelId = reqBodyModelId,
                lensModelId = reqLensModelId,
                page = 0,
                size = 20
            )

            val result = repository.getRelatedProducts(request)
            result
                .onSuccess { response ->
                    Log.d("ResultsViewModel", "[RelatedProducts] ✅ API 호출 성공: ${response.total}건")
                    // 주의: RelatedProductItem 데이터 클래스에 priceType 필드가 있어야 합니다.
                    _relatedProducts.value = response.items.filter { it.priceType == "PER_ITEM" }

                    _relatedProductsError.value = null
                }
                .onFailure { throwable ->
                    Log.e("ResultsViewModel", "[RelatedProducts] ❌ API 호출 실패", throwable)
                    _relatedProducts.value = emptyList()
                    _relatedProductsError.value = "연관 제품 추천 실패: ${throwable.message}"
                }

            _relatedProductsLoading.value = false
        }
    }

    private fun fetchRecommendationsForModel(modelId: Long, preferredGrade: String) {
        lastRecommendationModelId = modelId
        lastRecommendationPreferredGrade = preferredGrade

        viewModelScope.launch {
            _recommendationsLoading.value = true
            _recommendationsError.value = null

            val regionId = _userRegionId.value
            val itemType = _selectedItemType.value

            val result = repository.getRecommendations(
                modelId = modelId,
                userRegionId = regionId,
                radiusKm = 10,
                limit = 20,
                condition = null,
                itemType = itemType
            )
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
                }
                .onFailure { throwable ->
                    Log.e("ResultsViewModel", "[Recommendations] failure: ${throwable.message}", throwable)
                    _recommendations.value = emptyMap()
                    _recommendationsError.value = throwable.message ?: "추천 불러오기 실패"
                }

            _recommendationsLoading.value = false
        }
    }

    /**
     * UI 값을 서버 API 규격으로 변환
     */
    private fun mapToApiValue(category: String, uiValue: String): String {
        return when (category) {
            "brand" -> uiValue // Sony -> Sony (그대로 유지)
            "cameraType" -> uiValue // Mirrorless -> Mirrorless (그대로 유지)
            "sensorFormat" -> uiValue.replace(" ", "_").uppercase() // Full Frame -> FULL_FRAME
            "mount" -> {
                when (uiValue) {
                    "Sony E" -> "E"
                    "Canon RF" -> "RF"
                    "Canon EF" -> "EF"
                    "Nikon Z" -> "Z"
                    "Nikon F" -> "F"
                    "Fuji X" -> "X"
                    "L-Mount" -> "L"
                    "Micro Four Thirds" -> "MFT"
                    else -> uiValue.uppercase().replace(" ", "_")
                }
            }
            else -> uiValue
        }
    }
}