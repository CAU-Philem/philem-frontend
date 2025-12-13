<div align="center">

# 🎯 Philem API 연동 가이드

### 중고 카메라 가격 분석 시스템

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2024+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.9.0-48B983?style=for-the-badge)](https://square.github.io/retrofit/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.0-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

</div>

---

## 📌 개요

**Philem**은 중고 카메라 시장의 실시간 가격 데이터를 분석하여 사용자에게 최적의 거래 정보를 제공합니다.

### ✨ 주요 기능

- 📊 **실시간 시세 분석** - 최근 36개월 거래 데이터 기반
- 💰 **가격 비교** - 평균 대비 저렴/비싼 정도 자동 계산
- 📦 **번들 상품 지원** - 바디+렌즈 세트 가격 분석
- 🎯 **등급별 분류** - A/B/C 등급별 시세 추적
- 📍 **지역 기반 추천** - 근처 유사 매물 추천

---

## 🚀 빠른 시작

---

## 🚀 빠른 시작

<details open>
<summary><b>1️⃣ 단일 상품 가격 분석</b></summary>

<br>

카메라 바디 또는 렌즈 하나의 가격을 분석합니다.

```kotlin
class ResultsActivity : ComponentActivity() {
    private val viewModel: ResultsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 📊 단일 상품 가격 분석
        viewModel.loadPriceData(
            modelId = 1L,           // 모델 ID (Sony A7M3 등)
            condition = "B",        // 등급 (A: 최상, B: 상, C: 중)
            price = 950000L         // 사용자 입력 가격
        )
        
        // 🎨 UI 연결
        setContent {
            val snapshots by viewModel.priceSnapshots.collectAsState()
            val productSet by viewModel.productSet.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                error != null -> {
                    ErrorScreen(message = error)
                }
                else -> {
                    productSet?.let { set ->
                        PriceChart(
                            snapshots = snapshots,
                            selectedGrade = "B",
                            productSet = set,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
```

</details>

<details>
<summary><b>2️⃣ 번들 상품 가격 분석</b></summary>

<br>

바디와 렌즈를 함께 구매할 때의 가격을 분석합니다.

```kotlin
// 📦 번들 상품 (바디 + 렌즈)
viewModel.loadBundlePriceData(
    bundlePrice = 1500000L,     // 번들 전체 가격
    items = listOf(
        1L to "B",              // Sony A7M3 바디 - B등급
        12L to "C"              // FE 28-70mm 렌즈 - C등급
    )
)
```

**💡 Tip:** 번들 가격은 개별 아이템의 최신 월 평균가 합계와 비교됩니다.

</details>

---

## 📊 데이터 구조

### ProductSet

상품 정보와 백엔드 분석 결과를 담는 핵심 데이터 클래스입니다.

```kotlin
data class ProductSet(
    // 📷 기본 정보
    val name: String,                    // 상품명 (예: "Sony A7M3")
    val combinedPrice: Long,             // 합본 가격
    val bodyPrice: Long,                 // 바디만 가격
    val lensPrice: Long,                 // 렌즈만 가격
    val grade: String,                   // 등급 (A/B/C)
    
    // 📈 백엔드 API 분석 결과
    val percentVsRef: Double,            // 평균 대비 할인율 (%)
                                         // 예: -9.52 = 9.52% 저렴
    val direction: String,               // LOWER(저렴) / HIGHER(비쌈) / SAME(동일)
    val refAvgPrice: Long,               // 기준 평균가
    val refYear: Int,                    // 기준 연도 (예: 2025)
    val refMonth: Int,                   // 기준 월 (예: 11)
    val diffPrice: Long,                 // 가격 차이 (원)
    
    // 🏷️ 구성 정보
    val hasBody: Boolean,                // 바디 포함 여부
    val hasLens: Boolean                 // 렌즈 포함 여부
)
```

### 필드 설명

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `percentVsRef` | `Double` | 평균 대비 할인율 (음수 = 저렴) | `-9.52` |
| `direction` | `String` | 가격 방향성 | `LOWER` |
| `refAvgPrice` | `Long` | 참조 평균가 | `1050000` |
| `diffPrice` | `Long` | 입력가 - 평균가 | `-100000` |

---

## 🔧 API 엔드포인트

### 📡 전체 API 목록

<table>
<tr>
<th>API</th>
<th>Method</th>
<th>Endpoint</th>
<th>설명</th>
</tr>
<tr>
<td>🔍 URL 분석</td>
<td><code>POST</code></td>
<td><code>/listings/models-from-url</code></td>
<td>당근 URL에서 상품 정보 추출</td>
</tr>
<tr>
<td>📊 시세 조회</td>
<td><code>GET</code></td>
<td><code>/models/{modelId}/snapshots</code></td>
<td>월별 시세 데이터 조회</td>
</tr>
<tr>
<td>💰 단일 비교</td>
<td><code>GET</code></td>
<td><code>/models/{modelId}/compare</code></td>
<td>단일 상품 가격 비교</td>
</tr>
<tr>
<td>📦 번들 비교</td>
<td><code>POST</code></td>
<td><code>/bundles/compare</code></td>
<td>번들 상품 가격 비교</td>
</tr>
<tr>
<td>🎯 추천</td>
<td><code>GET</code></td>
<td><code>/recommendations</code></td>
<td>근처 유사 매물 추천</td>
</tr>
</table>

### 🔍 API 상세 정보

<details>
<summary><b>GET /models/{modelId}/snapshots</b> - 시세 데이터 조회</summary>

<br>

**Parameters:**
- `modelId` (path) - 모델 ID
- `months` (query, optional) - 조회 개월 수 (기본값: 36)

**Response:**
```json
[
  {
    "condition": "B",
    "sold_year": 2025,
    "sold_month": 12,
    "max_price": 1200000,
    "min_price": 950000,
    "avg_price": 1050000,
    "sample_count": 7
  }
]
```

</details>

<details>
<summary><b>GET /models/{modelId}/compare</b> - 단일 상품 비교</summary>

<br>

**Parameters:**
- `modelId` (path) - 모델 ID
- `condition` (query) - 등급 (A/B/C)
- `price` (query) - 사용자 입력 가격

**Response:**
```json
{
  "model_id": 123,
  "condition": "B",
  "input_price": 950000,
  "available": true,
  "percent_vs_ref": -9.52,
  "direction": "LOWER",
  "ref_avg_price": 1050000
}
```

</details>

<details>
<summary><b>POST /bundles/compare</b> - 번들 상품 비교</summary>

<br>

**Request Body:**
```json
{
  "bundle_price": 1500000,
  "items": [
    { "model_id": 1, "condition": "B" },
    { "model_id": 12, "condition": "C" }
  ]
}
```

**Response:**
```json
{
  "bundle_price": 1500000,
  "available": true,
  "percent_vs_ref": -9.09,
  "direction": "LOWER",
  "ref_price": 1650000,
  "items": [
    {
      "model_id": 1,
      "condition": "B",
      "ref_avg_price": 1050000
    }
  ]
}
```

</details>

---

## ⚙️ 프로젝트 설정

### 🔌 Retrofit 설정

`RetrofitClient.kt` 파일에서 서버 URL을 설정합니다:

```kotlin
object RetrofitClient {
    // ⚠️ TODO: 실제 서버 URL로 변경 필요!
    private const val BASE_URL = "https://api.philem.com/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val pricingApi: PricingApiService = retrofit.create(PricingApiService::class.java)
}
```

> 💡 **Tip:** 개발 환경과 프로덕션 환경의 URL을 `BuildConfig`로 분리하는 것을 권장합니다.

### 📦 의존성

`build.gradle.kts`에 다음 라이브러리들이 추가되어 있어야 합니다:

```kotlin
dependencies {
    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
```

---

## 🎨 UI 컴포넌트

### 💰 가격 정보 표시

```kotlin
@Composable
fun PriceInfoCard(productSet: ProductSet) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (productSet.direction) {
                "LOWER" -> Color(0xFFE8F5E9)  // 연한 초록
                "HIGHER" -> Color(0xFFFFEBEE) // 연한 빨강
                else -> Color(0xFFF5F5F5)      // 회색
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 할인율 표시
            val discountText = when (productSet.direction) {
                "LOWER" -> "🎉 시세 대비 ${abs(productSet.percentVsRef.toInt())}% 저렴"
                "HIGHER" -> "⚠️ 시세 대비 ${abs(productSet.percentVsRef.toInt())}% 비쌈"
                else -> "💰 시세와 동일"
            }
            
            Text(
                text = discountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (productSet.direction) {
                    "LOWER" -> Color(0xFF4CAF50)
                    "HIGHER" -> Color(0xFFE53935)
                    else -> Color(0xFF757575)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 기준 정보
            Text(
                text = "기준: ${productSet.refYear}년 ${productSet.refMonth}월 평균가",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Text(
                text = "평균가: ${productSet.refAvgPrice.formatPrice()}원",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### 📊 할인율 배지

```kotlin
@Composable
fun DiscountBadge(productSet: ProductSet) {
    if (productSet.direction != "SAME") {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (productSet.direction) {
                "LOWER" -> Color(0xFF4CAF50)
                else -> Color(0xFFFF5722)
            }
        ) {
            Text(
                text = when (productSet.direction) {
                    "LOWER" -> "↓ ${abs(productSet.percentVsRef.toInt())}% 저렴"
                    else -> "↑ ${abs(productSet.percentVsRef.toInt())}% 비쌈"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

---

## 🐛 에러 처리

### 에러 화면 컴포넌트

```kotlin
@Composable
fun ErrorScreen(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFE53935)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "오류가 발생했습니다",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message ?: "알 수 없는 오류",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("다시 시도")
        }
    }
}
```

### 에러 다이얼로그

```kotlin
@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )
        },
        title = {
            Text(
                text = "연결 오류",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(errorMessage)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onRetry()
                }
            ) {
                Text("재시도")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
```

---

## 🔄 로딩 상태

### 로딩 인디케이터

```kotlin
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "가격 데이터를 불러오는 중...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
```

---

## 🧪 테스트 & Mock 데이터

### Mock 데이터 자동 Fallback

API 연동 실패 시 자동으로 Mock 데이터를 사용하여 앱이 중단되지 않도록 합니다:

```kotlin
// ResultsViewModel.kt 내부
try {
    val snapshotsResult = repository.getSnapshots(modelId, months = 36)
    snapshotsResult.onSuccess { snapshots ->
        _priceSnapshots.value = snapshots
    }.onFailure { e ->
        _error.value = "스냅샷 로드 실패: ${e.message}"
        // ⚠️ Fallback: Mock 데이터 사용
        _priceSnapshots.value = MockPriceData.getSonyA7M3Snapshots()
    }
} catch (e: Exception) {
    // 예외 발생 시에도 Mock 데이터로 대체
    _priceSnapshots.value = MockPriceData.getSonyA7M3Snapshots()
}
```

### Mock 데이터 제공 함수

| 함수 | 설명 | 반환 타입 |
|------|------|-----------|
| `MockPriceData.getSonyA7M3Snapshots()` | Sony A7M3 36개월 시세 데이터 | `List<ModelPriceSnapshot>` |
| `MockPriceData.getProductSet()` | 샘플 상품 정보 (B등급) | `ProductSet` |

---

## 📋 개발 체크리스트

### ✅ 필수 설정

- [x] **Retrofit 의존성** 추가 (`build.gradle.kts`)
- [x] **인터넷 권한** 추가 (`AndroidManifest.xml`)
- [x] **API 서비스 인터페이스** 생성 (`PricingApiService.kt`)
- [x] **Repository 패턴** 구현 (`PricingRepository.kt`)
- [x] **ViewModel** 에서 API 호출
- [x] **UI** 에서 백엔드 응답 사용

### ⚠️ 배포 전 확인사항

- [ ] **BASE_URL 변경** - 실제 프로덕션 서버 URL로 변경
- [ ] **API 키 관리** - 필요 시 `local.properties`에 API 키 추가
- [ ] **ProGuard 규칙** - Retrofit, Gson 난독화 예외 처리
- [ ] **에러 로깅** - Firebase Crashlytics 등 연동
- [ ] **네트워크 상태 확인** - 오프라인 모드 처리

### 🎨 UI/UX 개선

- [ ] **로딩 애니메이션** 추가
- [ ] **에러 화면** 디자인 개선
- [ ] **빈 상태(Empty State)** 처리
- [ ] **Pull-to-Refresh** 기능 추가
- [ ] **오프라인 캐싱** 구현

---

## 🔒 ProGuard 설정

배포 빌드 시 Retrofit과 Gson이 정상 작동하도록 ProGuard 규칙을 추가하세요:

```proguard
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# API Response Models
-keep class com.philem.philem.domain.pricing.dto.** { *; }
```

---

## 📚 추가 리소스

### 📖 참고 문서

- [Retrofit 공식 문서](https://square.github.io/retrofit/)
- [Kotlin Coroutines 가이드](https://kotlinlang.org/docs/coroutines-guide.html)
- [Jetpack Compose 문서](https://developer.android.com/jetpack/compose)
- [Android Architecture Components](https://developer.android.com/topic/architecture)

### 🛠️ 유용한 도구

- [Postman](https://www.postman.com/) - API 테스트
- [Charles Proxy](https://www.charlesproxy.com/) - 네트워크 디버깅
- [JSON Formatter](https://jsonformatter.org/) - JSON 데이터 검증

---

<div align="center">

## 💬 문의 및 지원

문제가 발생하거나 질문이 있으시면 언제든지 문의해주세요!

**Made with ❤️ by Philem Team**

---

*Last Updated: 2025.12.14*

</div>

