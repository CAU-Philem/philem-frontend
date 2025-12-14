# 연관 제품 추천 시스템 - 유즈케이스 구현 현황

**작성일:** 2025-12-15  
**최종 업데이트:** 2025-12-15 (UC-01 완성)  
**현재 구현 상태:** UC-01 완성 ✅ (100%)

---

## 📋 유즈케이스 요구사항 vs 구현 현황

### ✅ UC-01: 필터로 매물 검색하기 (FILTER) - **100% 완료** 🎉

#### 요구사항
- **목적:** 사용자가 조건/가격/브랜드/마운트로 매물 리스트 조회
- **입력:** 
  - `mode: "FILTER"`
  - `unitType` (BODY/LENS)
  - 옵션: `brand`, `mount`, `sensorFormat`, `condition`, `minPrice`, `maxPrice`
  - `page`, `size`
- **출력:** 매물 목록 + 페이징 정보
- **주의사항:**
  - enum 문자열은 서버와 동일 (대문자/언더스코어)
  - size는 1 이상

#### 구현 현황

**✅ 완료:**
- API 인터페이스 정의 (`POST /listings/related`)
- DTO 정의 (`RelatedProductsRequest`, `RelatedProductsResponse`)
  - **멀티 필터 지원:** `conditions`, `brands`, `cameraTypes`, `mounts`, `sensorFormats` 모두 `List<String>?`로 변경
- Repository 메서드 (`getRelatedProducts`)
- ViewModel 상태 관리 및 필터 토글 로직
  - **멀티 선택 지원:** 여러 조건 동시 선택 시 모두 API로 전송
  - **필터 초기화 기능:** `clearRelatedFilters()` 추가
- UI: 체크박스 필터 사이드바 (등급, 브랜드, 카메라 타입, 센서 크기)
  - **선택 개수 표시:** 각 필터 섹션에 선택된 개수 배지 표시
  - **전체 필터 개수 표시:** 제목 옆에 총 선택 개수 표시
  - **초기화 버튼:** 모든 필터를 한 번에 초기화
- UI: 가로 스크롤 상품 리스트
- 페이징 파라미터 (`page`, `size`)
- enum 문자열 대문자 사용 (MIRRORLESS, DSLR, FULL_FRAME, APS_C)

**✅ 최근 개선 사항 (2025-12-15):**
1. **멀티 필터 지원 완성**
   ```kotlin
   // Before: 첫 번째만 전송
   condition = filters.conditions.firstOrNull()
   
   // After: 모든 선택값 전송
   conditions = filters.conditions.takeIf { it.isNotEmpty() }?.toList()
   ```

2. **UX 개선**
   - 선택된 필터 개수 실시간 표시 (예: "등급 (2)")
   - 전체 선택 개수 배지 표시
   - 빠른 초기화 버튼 추가

3. **로깅 개선**
   - 선택된 필터와 전송된 필터 구분하여 로깅
   - 디버깅 용이성 향상

**구현 위치:**
- DTO: `app/src/main/java/com/philem/philem/domain/pricing/dto/ApiResponses.kt` (Line 105-120)
- API: `app/src/main/java/com/philem/philem/data/api/PricingApiService.kt`
- Repository: `app/src/main/java/com/philem/philem/data/repository/PricingRepository.kt`
- ViewModel: `app/src/main/java/com/philem/philem/results/ResultsViewModel.kt` (Line 450-600)
- UI: `app/src/main/java/com/philem/philem/results/ResultsActivity.kt` (Line 855-1095)

**남은 작업 (선택 사항):**
- [ ] 페이징 UI ("더 보기" 버튼 또는 무한 스크롤)
- [ ] 가격 범위 슬라이더 (현재는 minPrice/maxPrice만 지원)
- [ ] 필터 디바운싱 (500ms 딜레이로 API 호출 최적화)

---

### ❌ UC-02: 바디 → 렌즈 추천 (BODY_TO_LENS) - **0% 완료**

#### 요구사항
- **목적:** 바디 모델 선택 시 호환 렌즈 자동 추천
- **입력:**
  - `mode: "BODY_TO_LENS"`
  - `bodyModelId` (필수)
  - 옵션: `presetLensBrand`, `condition`, `minPrice`, `maxPrice`
  - `page`, `size`
- **출력:** 호환 렌즈 목록
- **주의:** `presetLensBrand`는 더미값("string") 금지

#### 구현 현황
- **미구현:** 현재 `mode`는 "FILTER"로 고정
- **필요 작업:**
  1. ViewModel에 `mode` 선택 로직 추가
  2. UI에 "호환 렌즈 보기" 버튼 추가
  3. `bodyModelId` 기반 API 호출 구현

---

### ❌ UC-03: 렌즈 → 바디 추천 (LENS_TO_BODY) - **0% 완료**

#### 요구사항
- **목적:** 렌즈 선택 시 호환 바디 추천
- **입력:**
  - `mode: "LENS_TO_BODY"`
  - `lensModelId` (필수)
  - 옵션: 바디 필터
  - `page`, `size`
- **출력:** 호환 바디 목록

#### 구현 현황
- **미구현**

---

### ❌ UC-04: 바디 + 렌즈 세트 추천 (BUNDLE) - **0% 완료**

#### 요구사항
- **목적:** 바디 + 렌즈 조합 추천
- **입력:**
  - `mode: "BUNDLE"`
  - `bodyModelId` (필수)
  - 렌즈 필터 (초점거리, 조리개, 브랜드 등)
  - `page`, `size`
- **출력:** 바디 1개 + 매칭 렌즈 N개

#### 구현 현황
- **미구현**
- **참고:** 기존에 번들 가격 비교 API는 있음 (`/bundles/compare`)

---

### ❌ UC-05: 필터 옵션 목록 제공 (FACETS) - **0% 완료**

#### 요구사항
- **목적:** 드롭다운/필터 UI용 enum 값 제공
- **입력:** 컨텍스트 (선택)
- **출력:** `BrandType`, `CameraType`, `Mount`, `SensorFormat`, `ConditionType` enum 리스트
- **효과:** 하드코딩된 "Sony", "Mirrorless" 같은 값 제거

#### 구현 현황
- **현재:** 하드코딩된 옵션 사용
```kotlin
// ResultsActivity.kt Line 880-913
FilterSection(
    title = "브랜드",
    options = listOf("SONY", "CANON", "NIKON"), // 하드코딩
    ...
)
```
- **필요 작업:** `/api/filters/meta` 같은 엔드포인트 추가

---

### ❌ UC-06: 모델 상세 조회 (MODEL_DETAIL) - **0% 완료**

#### 요구사항
- **목적:** 모델 클릭 시 스펙/속성/호환 정보 조회
- **입력:** `modelId`
- **출력:** 브랜드, 마운트, 센서포맷, 타입 등

#### 구현 현황
- **미구현**
- **현재:** 클릭 시 당근마켓 URL로만 이동
```kotlin
// ResultsActivity.kt Line 1002-1009
Card(
    modifier = Modifier.clickable {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.postUrl))
        context.startActivity(intent)
    }
)
```

---

## 🎯 우선순위별 개발 로드맵

### ✅ Phase 1: UC-01 완성 - **완료** (2025-12-15)
- [x] 멀티 필터 지원 (List<String> 형식으로 DTO 변경)
- [x] 필터 초기화 버튼 추가
- [x] 선택 개수 실시간 표시
- [ ] 페이징 UI 추가 (선택 사항 - 현재는 page 0만 호출)

### Phase 2: UC-02, UC-03 구현 (예상 3-5일)
- [ ] `mode` 선택 UI 추가 (라디오 버튼: FILTER / BODY_TO_LENS / LENS_TO_BODY)
- [ ] 호환성 기반 추천 로직 구현
- [ ] `presetLensBrand` 유효성 검증
- [ ] "호환 렌즈 보기" / "호환 바디 보기" 버튼 추가

### Phase 3: UC-04 구현 (예상 2-3일)
- [ ] 번들 추천 UI
- [ ] 렌즈 상세 필터 (초점거리, 조리개)

### Phase 4: UC-05, UC-06 구현 (예상 2-3일)
- [ ] 필터 메타데이터 API 연동
- [ ] 모델 상세 페이지 구현

---

## 📌 해결된 이슈 (2025-12-15)

### ~~1. 단일 필터만 전송~~ ✅ **해결됨**
```kotlin
// Before: A, B 둘 다 선택해도 A만 전송
condition = filters.conditions.firstOrNull()

// After: 모든 선택값 리스트로 전송
conditions = filters.conditions.takeIf { it.isNotEmpty() }?.toList()
```

**해결 방법:**
- DTO를 `List<String>?` 형식으로 변경
- ViewModel에서 `takeIf { it.isNotEmpty() }?.toList()` 사용

### 2. 필터 옵션이 하드코딩 ⚠️ **부분 해결**
```kotlin
// ResultsActivity.kt Line 888
options = listOf("SONY", "CANON", "NIKON")  // 여전히 하드코딩
```

**해결 방법:** UC-05 구현 후 동적으로 로드 (다음 Phase)

### 3. 페이징 미구현 ⚠️ **선택 사항**
- 현재 `page: 0`, `size: 20`으로 고정
- "더 보기" 버튼 또는 무한 스크롤 추가 가능 (우선순위 낮음)

---

## 🔍 코드 위치 참조

| 항목 | 파일 경로 | 라인 |
|------|----------|------|
| RelatedProductsRequest DTO | `domain/pricing/dto/ApiResponses.kt` | 105-118 |
| API 인터페이스 | `data/api/PricingApiService.kt` | 62-66 |
| Repository 메서드 | `data/repository/PricingRepository.kt` | 76-84 |
| ViewModel 필터 로직 | `results/ResultsViewModel.kt` | 450-580 |
| UI - 필터 섹션 | `results/ResultsActivity.kt` | 855-1095 |

---

## ✨ 추가 권장사항

1. **에러 핸들링 강화**
   - 네트워크 오류 시 재시도 버튼
   - 빈 결과일 때 추천 필터 제안

2. **UX 개선**
   - 로딩 스켈레톤 UI
   - 필터 적용 시 애니메이션
   - 선택된 필터 개수 표시 (예: "등급 (2)")

3. **성능 최적화**
   - 필터 변경 시 디바운싱 (500ms)
   - 이미지 lazy loading

4. **테스트 코드 작성**
   - ViewModel 단위 테스트
   - API 통합 테스트

