# MCI 마스킹 시스템 변경사항

## 개요
본 문서는 MCI 마스킹 시스템의 개선 작업 내용을 정리한 문서입니다.

## 변경 일자
2025-11-01

---

## 주요 변경사항

### 1. List 타입 데이터 구조 지원 (중첩 List 포함)

#### 변경 전
- jsonPath 경로 탐색 중 List 타입을 만나면 처리하지 못하고 무시됨
- Map 타입만 재귀적으로 탐색 가능

#### 변경 후
- List 타입 내부의 각 요소(Map)에 대해 재귀적으로 마스킹 적용
- 중첩된 List 구조(List 안의 List)도 정상 처리
- 빈 List에 대해서도 예외 없이 안전하게 처리

#### 영향받는 파일
- `MaskingService.java` (src/main/java/com/example/playground/config/MaskingService.java:78-86)

#### 코드 변경
```java
// 4. List 타입인 경우 처리
else if (value instanceof List) {
    List<?> list = (List<?>) value;
    // List 내부의 각 요소에 대해 재귀적으로 마스킹 적용
    for (Object item : list) {
        if (item instanceof Map) {
            maskRecursive((Map<String, Object>) item, keys, index + 1, maskingType);
        }
    }
}
```

#### 사용 예시
```yaml
# mciMasking.yml
masking:
  mappings:
    "NCDP_CUSAFT10A0":
      - jsonPath: "CUSAFT_IN.CUSAFT_IN_SUB.acno"  # CUSAFT_IN_SUB가 List인 경우에도 동작
        maskingType: "type_account"
```

데이터 구조:
```json
{
  "CUSAFT_IN": {
    "CUSAFT_IN_SUB": [
      {"acno": "1234567890", "name": "홍길동"},
      {"acno": "0987654321", "name": "김철수"}
    ]
  }
}
```

마스킹 후:
```json
{
  "CUSAFT_IN": {
    "CUSAFT_IN_SUB": [
      {"acno": "<Protected_Mci_Data>", "name": "홍길동"},
      {"acno": "<Protected_Mci_Data>", "name": "김철수"}
    ]
  }
}
```

---

### 2. Common 마스킹 룰 자동 적용

#### 변경 전
- mciId별 룰만 적용
- 공통 필드(예: pfmidata.rlno, pfminpt.rlno)를 각 mciId마다 중복 정의 필요

#### 변경 후
- `common` 키 하위에 정의된 룰이 모든 mciId에 자동으로 적용됨
- MaskingManager에서 룰 조회 시 common + mciId별 룰을 자동으로 병합하여 반환

#### 영향받는 파일
- `MaskingManager.java` (src/main/java/com/example/playground/config/MaskingManager.java:27-40)

#### 코드 변경
```java
public List<MaskingProperties.PathRule> getMaskingRules(String mciId) {
    List<MaskingProperties.PathRule> result = new ArrayList<>();

    // 1. common 룰 추가 (모든 mciId에 공통 적용)
    List<MaskingProperties.PathRule> commonRules =
        maskingProperties.getMappings().getOrDefault(COMMON_KEY, Collections.emptyList());
    result.addAll(commonRules);

    // 2. mciId별 특정 룰 추가
    List<MaskingProperties.PathRule> specificRules =
        maskingProperties.getMappings().getOrDefault(mciId, Collections.emptyList());
    result.addAll(specificRules);

    return result;
}
```

#### 설계 결정 이유
**선택한 방안:** MaskingManager에서 common 룰 자동 병합

**이유:**
1. **관심사의 분리**: MaskingService는 "어떻게 마스킹할지"만 담당, MaskingManager는 "어떤 룰을 적용할지" 담당
2. **투명성**: applyMasking 호출부에서 common 존재 여부를 몰라도 됨
3. **유지보수성**: common 처리 로직이 한 곳(MaskingManager)에 집중됨
4. **성능**: 한 번의 데이터 순회로 모든 룰(common + mciId별) 적용
5. **확장성**: 나중에 다른 공통 카테고리 추가 시 Manager만 수정하면 됨

#### 사용 예시
```yaml
# mciMasking.yml
masking:
  mappings:
    "common":  # 모든 mciId에 공통 적용
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
      - jsonPath: "pfminpt.rlno"
        maskingType: "type_rlno"

    "NCDP_MIMEIN10A0":  # 이 mciId에만 적용
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.custNm"
        maskingType: "type_name"
```

`NCDP_MIMEIN10A0`로 마스킹 호출 시 자동으로 3개의 룰이 적용됨:
- `pfmidata.rlno` (common)
- `pfminpt.rlno` (common)
- `MIMEIN_IN_MST.MIMEIN_IN.custNm` (mciId별)

---

## 추가된 테스트 케이스

### List 타입 관련 테스트
1. **`applyMasking_withListType()`**
   - 단일 레벨 List 내부의 Map에 대한 마스킹 검증
   - 경로: `CUSAFT_IN.CUSAFT_IN_SUB.acno`

2. **`applyMasking_withEmptyList()`**
   - 빈 List에 대한 예외 처리 검증

3. **`applyMasking_withNestedListStructure()`**
   - 2단계 중첩 List 구조 마스킹 검증
   - 경로: `MIMEIN10A0.svcidata.sub01.subsub01.rlno`

4. **`applyMasking_withDeeplyNestedListStructure()`**
   - 3단계 이상 중첩 List 구조 마스킹 검증
   - 경로: `root.level1.level2.level3.rlno`

### Common 룰 관련 테스트
1. **`applyMasking_withCommonAndSpecificRules()`**
   - common 룰과 mciId별 룰이 모두 적용되는지 검증

2. **`applyMasking_withOnlyCommonRules()`**
   - common 룰만 있고 mciId별 룰이 없어도 정상 동작하는지 검증

---

## 시간 복잡도 분석 (Big-O)

### 현재 방식: JsonPath 기반 매핑 방식

#### 시간 복잡도: **O(R × D)**

**변수 정의:**
- `R`: 마스킹 룰의 개수
- `D`: 각 jsonPath의 평균 깊이 (경로 길이)
- `N`: 전체 데이터 노드 수 (Map 키-값 쌍 + List 요소)
- `L`: List 요소의 평균 개수

#### 복잡도 상세 분석:

각 룰마다:
1. jsonPath를 "." 기준으로 split: **O(D)**
2. 재귀적 탐색:
   - Map을 만날 때마다 키로 직접 접근: **O(1)**
   - List를 만날 때마다 각 요소 순회: **O(L)**
   - 경로 깊이만큼 반복: **O(D)**
   - List가 포함된 경로의 경우: **O(D × L)**

**전체 시간 복잡도:**
- List가 없는 경우: **O(R × D)**
- List가 포함된 경로의 경우: **O(R × D × L)**

**공간 복잡도:** **O(D)** (재귀 호출 스택)

#### 장점:
1. **효율성**: 필요한 경로만 정확히 탐색
2. **예측 가능성**: 룰 개수와 경로 깊이에 비례하여 성능 예측 가능
3. **확장성**: 데이터 크기가 커져도 룰이 증가하지 않으면 성능 영향 적음
4. **유지보수성**: 명시적인 경로로 어떤 필드가 마스킹되는지 명확함

---

### 대안: 필드명만으로 마스킹하는 방식

#### 시간 복잡도: **O(N)**

**변수 정의:**
- `N`: 전체 데이터 노드 수
- `F`: 마스킹 대상 필드명 Set 크기

#### 복잡도 상세 분석:

1. 전체 데이터 구조를 DFS/BFS로 순회: **O(N)**
2. 각 노드마다 필드명이 마스킹 대상인지 확인: **O(1)** (HashSet 사용 시)

**전체 시간 복잡도:** **O(N)**

**공간 복잡도:** **O(N)** (재귀 호출 스택 또는 큐)

#### 단점:
1. **오버 마스킹 위험**: 같은 이름의 필드가 다른 위치에 있어도 모두 마스킹됨
   - 예: `user.rlno`는 마스킹하고 싶지만 `metadata.rlno`는 유지하고 싶은 경우 구분 불가
2. **불필요한 순회**: 마스킹 대상이 아닌 깊은 구조도 모두 탐색해야 함
3. **예측 불가능성**: 데이터 크기에 따라 성능이 크게 변함
4. **유지보수성 저하**: 어떤 위치의 필드가 마스킹되는지 불명확

---

### 성능 비교 예시

#### 시나리오 1: 작은 데이터, 많은 룰
- 데이터 노드 수 (N): 100개
- 마스킹 룰 수 (R): 20개
- 평균 경로 깊이 (D): 4

**JsonPath 방식:** O(20 × 4) = **80 연산**
**필드명 방식:** O(100) = **100 연산**
→ **JsonPath 방식이 유리**

#### 시나리오 2: 큰 데이터, 적은 룰
- 데이터 노드 수 (N): 10,000개
- 마스킹 룰 수 (R): 5개
- 평균 경로 깊이 (D): 3

**JsonPath 방식:** O(5 × 3) = **15 연산**
**필드명 방식:** O(10,000) = **10,000 연산**
→ **JsonPath 방식이 압도적으로 유리**

#### 시나리오 3: List가 포함된 경우
- 마스킹 룰 수 (R): 3개
- 평균 경로 깊이 (D): 4
- List 평균 요소 수 (L): 50개

**JsonPath 방식:** O(3 × 4 × 50) = **600 연산**
**필드명 방식:** O(N) - 전체 데이터 크기에 의존

→ **List가 크더라도 JsonPath 방식은 해당 경로만 탐색하므로 여전히 효율적**

---

### 결론

**JsonPath 기반 매핑 방식이 우수한 이유:**

1. **정확성**: 경로를 명시하여 의도하지 않은 마스킹 방지
2. **효율성**: 필요한 경로만 탐색하여 대부분의 경우 더 빠름
3. **확장성**: 데이터가 커져도 룰이 증가하지 않으면 성능 유지
4. **유연성**:
   - 같은 필드명이라도 위치에 따라 다르게 처리 가능
   - common 룰로 공통 처리와 mciId별 특수 처리를 조합 가능
5. **가독성**: 설정 파일만 보고도 어떤 필드가 마스킹되는지 명확히 파악

**현재 구현의 장점:**
- O(R × D) 복잡도로 대부분의 실무 환경에서 최적의 성능
- 중첩된 List 구조도 효율적으로 처리
- common 룰로 코드 중복 제거 및 유지보수성 향상

---

## 하위 호환성

### 기존 코드 영향도
- **영향 없음**: 기존에 작성된 mciMasking.yml 파일은 수정 없이 그대로 동작
- **선택적 적용**: common 룰은 필요한 경우에만 추가하면 됨
- **API 변경 없음**: `MaskingService.applyMasking()` 호출 방법은 동일

### 마이그레이션 가이드
기존 설정을 그대로 사용하거나, 공통 필드가 있다면 common으로 추출할 수 있습니다:

**마이그레이션 전:**
```yaml
masking:
  mappings:
    "NCDP_MCI_001":
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
      - jsonPath: "data.name"
        maskingType: "type_name"
    "NCDP_MCI_002":
      - jsonPath: "pfmidata.rlno"  # 중복
        maskingType: "type_rlno"
      - jsonPath: "info.account"
        maskingType: "type_account"
```

**마이그레이션 후:**
```yaml
masking:
  mappings:
    "common":
      - jsonPath: "pfmidata.rlno"  # 공통으로 추출
        maskingType: "type_rlno"
    "NCDP_MCI_001":
      - jsonPath: "data.name"
        maskingType: "type_name"
    "NCDP_MCI_002":
      - jsonPath: "info.account"
        maskingType: "type_account"
```

---

## 향후 개선 방향

1. **마스킹 타입별 구체적인 로직 구현**
   - 현재는 모든 타입이 `<Protected_Mci_Data>`로 처리됨
   - MaskingService.java:94-130의 주석 처리된 switch문 활성화 예정

2. **로깅 추가**
   - 마스킹 실패 시 상세 로그 (MaskingService.java:44)
   - 성능 모니터링을 위한 메트릭 수집

3. **캐싱 최적화**
   - 자주 사용되는 룰에 대한 캐싱
   - jsonPath split 결과 캐싱

4. **에러 핸들링 강화**
   - 잘못된 경로에 대한 명확한 에러 메시지
   - 순환 참조 감지 및 처리

---

## 관련 파일

### 수정된 파일
- `src/main/java/com/example/playground/config/MaskingService.java`
- `src/main/java/com/example/playground/config/MaskingManager.java`
- `src/test/java/com/example/playground/service/MaksingServiceTest.java`

### 설정 파일
- `src/main/resources/mciMasking.yml`

### 문서
- `docs/MASKING_CHANGELOG.md` (본 문서)
- `docs/MASKING_USER_GUIDE.md` (응용개발자 가이드)

---

## 문의사항
개선사항이나 버그 발견 시 개발팀에 문의해주시기 바랍니다.
