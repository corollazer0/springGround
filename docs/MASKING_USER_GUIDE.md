# MCI 마스킹 설정 가이드 (응용개발자용)

## 개요
본 문서는 `mciMasking.yml` 파일만 수정하여 마스킹 룰을 설정하는 응용개발자를 위한 가이드입니다.

**코드 수정 없이 설정 파일(`mciMasking.yml`)만 편집하면 됩니다.**

---

## 목차
1. [기본 개념](#기본-개념)
2. [설정 파일 구조](#설정-파일-구조)
3. [사용 예시](#사용-예시)
4. [고급 기능](#고급-기능)
5. [문제 해결](#문제-해결)
6. [베스트 프랙티스](#베스트-프랙티스)

---

## 기본 개념

### 마스킹이란?
개인정보나 민감정보를 보호하기 위해 원본 데이터를 특정 패턴으로 변환하는 것입니다.

**예시:**
- 주민번호: `9007021234567` → `900702*******`
- 이름: `홍길동` → `홍*동`
- 계좌번호: `1234567890` → `<Protected_Mci_Data>`

### JsonPath란?
JSON 데이터 구조에서 특정 필드의 위치를 표현하는 경로입니다.

**데이터 구조:**
```json
{
  "user": {
    "info": {
      "name": "홍길동"
    }
  }
}
```

**JsonPath:** `user.info.name`

---

## 설정 파일 구조

### 파일 위치
```
src/main/resources/mciMasking.yml
```

### 기본 구조
```yaml
masking:
  mappings:
    "common":                    # 모든 MCI에 공통 적용되는 룰
      - jsonPath: "경로"
        maskingType: "타입"

    "NCDP_MCI_ID":              # 특정 MCI ID에만 적용되는 룰
      - jsonPath: "경로"
        maskingType: "타입"
```

### 필드 설명

#### 1. `mappings`
- 모든 마스킹 룰을 담는 최상위 컨테이너
- 변경 불가

#### 2. 룰 그룹 키
- `"common"`: 모든 MCI ID에 자동으로 적용
- `"NCDP_XXX"`: 해당 MCI ID로 호출될 때만 적용

#### 3. `jsonPath`
- 마스킹할 필드의 경로
- "."으로 구분된 경로 문자열
- 대소문자 구분

#### 4. `maskingType`
- 마스킹 방식을 지정하는 타입
- 현재 버전에서는 모든 타입이 `<Protected_Mci_Data>`로 변환됨
- 향후 타입별 다른 마스킹 패턴 적용 예정

**사용 가능한 타입:**
- `type_rlno`: 주민등록번호
- `type_name`: 이름
- `type_account`: 계좌번호
- `type_email`: 이메일
- `type_address`: 주소

---

## 사용 예시

### 예시 1: 단순 경로
가장 기본적인 형태의 마스킹 설정입니다.

#### 데이터 구조
```json
{
  "pfmidata": {
    "rlno": "9007021234567"
  }
}
```

#### 설정
```yaml
masking:
  mappings:
    "NCDP_EXAMPLE01":
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
```

#### 마스킹 후
```json
{
  "pfmidata": {
    "rlno": "<Protected_Mci_Data>"
  }
}
```

---

### 예시 2: 깊은 경로
여러 단계로 중첩된 구조의 필드를 마스킹합니다.

#### 데이터 구조
```json
{
  "MIMEIN_IN_MST": {
    "MIMEIN_IN": {
      "rlno": "9007021234567",
      "custNm": "홍길동"
    }
  }
}
```

#### 설정
```yaml
masking:
  mappings:
    "NCDP_MIMEIN10A0":
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.rlno"
        maskingType: "type_rlno"
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.custNm"
        maskingType: "type_name"
```

#### 마스킹 후
```json
{
  "MIMEIN_IN_MST": {
    "MIMEIN_IN": {
      "rlno": "<Protected_Mci_Data>",
      "custNm": "<Protected_Mci_Data>"
    }
  }
}
```

---

### 예시 3: 배열(List) 데이터
배열 내부의 각 요소에 동일한 마스킹이 적용됩니다.

#### 데이터 구조
```json
{
  "CUSAFT_IN": {
    "CUSAFT_IN_SUB": [
      {
        "acno": "1234567890",
        "name": "홍길동"
      },
      {
        "acno": "0987654321",
        "name": "김철수"
      }
    ]
  }
}
```

#### 설정
```yaml
masking:
  mappings:
    "NCDP_CUSAFT10A0":
      - jsonPath: "CUSAFT_IN.CUSAFT_IN_SUB.acno"
        maskingType: "type_account"
```

**중요:** 배열 인덱스(`[0]`, `[1]`)를 명시하지 않습니다. 시스템이 자동으로 모든 요소를 처리합니다.

#### 마스킹 후
```json
{
  "CUSAFT_IN": {
    "CUSAFT_IN_SUB": [
      {
        "acno": "<Protected_Mci_Data>",
        "name": "홍길동"
      },
      {
        "acno": "<Protected_Mci_Data>",
        "name": "김철수"
      }
    ]
  }
}
```

---

### 예시 4: 중첩된 배열
배열 안에 배열이 있는 복잡한 구조도 처리할 수 있습니다.

#### 데이터 구조
```json
{
  "MIMEIN10A0": {
    "svcidata": {
      "sub01": [
        {
          "subsub01": [
            {
              "rlno": "1111111111111",
              "name": "홍길동"
            },
            {
              "rlno": "2222222222222",
              "name": "김철수"
            }
          ]
        },
        {
          "subsub01": [
            {
              "rlno": "3333333333333",
              "name": "이영희"
            }
          ]
        }
      ]
    }
  }
}
```

#### 설정
```yaml
masking:
  mappings:
    "NCDP_MIMEIN10A0":
      - jsonPath: "MIMEIN10A0.svcidata.sub01.subsub01.rlno"
        maskingType: "type_rlno"
```

#### 마스킹 후
```json
{
  "MIMEIN10A0": {
    "svcidata": {
      "sub01": [
        {
          "subsub01": [
            {
              "rlno": "<Protected_Mci_Data>",
              "name": "홍길동"
            },
            {
              "rlno": "<Protected_Mci_Data>",
              "name": "김철수"
            }
          ]
        },
        {
          "subsub01": [
            {
              "rlno": "<Protected_Mci_Data>",
              "name": "이영희"
            }
          ]
        }
      ]
    }
  }
}
```

**중첩 깊이 제한 없음:** 몇 단계로 중첩되어 있어도 정상 처리됩니다.

---

### 예시 5: Common 룰 활용
여러 MCI ID에서 공통으로 사용하는 필드는 `common`에 정의합니다.

#### 설정
```yaml
masking:
  mappings:
    "common":                              # 모든 MCI ID에 자동 적용
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
      - jsonPath: "pfminpt.rlno"
        maskingType: "type_rlno"

    "NCDP_MIMEIN10A0":                     # 이 MCI ID 전용
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.custNm"
        maskingType: "type_name"

    "NCDP_CUSAFT10A0":                     # 이 MCI ID 전용
      - jsonPath: "CUSAFT_IN.CUSAFT_IN_SUB.acno"
        maskingType: "type_account"
```

#### 동작 방식
- `NCDP_MIMEIN10A0` 호출 시: **3개 룰 적용**
  1. `pfmidata.rlno` (common)
  2. `pfminpt.rlno` (common)
  3. `MIMEIN_IN_MST.MIMEIN_IN.custNm` (MCI ID별)

- `NCDP_CUSAFT10A0` 호출 시: **3개 룰 적용**
  1. `pfmidata.rlno` (common)
  2. `pfminpt.rlno` (common)
  3. `CUSAFT_IN.CUSAFT_IN_SUB.acno` (MCI ID별)

#### 장점
- 중복 제거: 공통 필드를 매번 반복해서 정의하지 않아도 됨
- 유지보수 용이: 공통 룰 변경 시 한 곳만 수정
- 실수 방지: 일부 MCI ID에만 공통 룰을 누락하는 실수 방지

---

## 고급 기능

### 1. 선택적 마스킹
같은 이름의 필드라도 경로가 다르면 선택적으로 마스킹할 수 있습니다.

#### 데이터 구조
```json
{
  "userInfo": {
    "rlno": "9007021234567"    // 마스킹 O
  },
  "metadata": {
    "rlno": "referenceId123"   // 마스킹 X
  }
}
```

#### 설정
```yaml
masking:
  mappings:
    "NCDP_SELECTIVE":
      - jsonPath: "userInfo.rlno"    # 이 경로만 마스킹
        maskingType: "type_rlno"
      # metadata.rlno는 설정하지 않음 → 마스킹 안 됨
```

---

### 2. 복수 MCI ID 설정
하나의 파일에 여러 MCI ID의 룰을 정의할 수 있습니다.

```yaml
masking:
  mappings:
    "common":
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"

    "NCDP_MCI_001":
      - jsonPath: "data.field1"
        maskingType: "type_name"

    "NCDP_MCI_002":
      - jsonPath: "info.field2"
        maskingType: "type_account"

    "NCDP_MCI_003":
      - jsonPath: "user.email"
        maskingType: "type_email"
```

---

### 3. 빈 룰 설정
특정 MCI ID에 마스킹을 적용하지 않으려면 비워두거나 아예 정의하지 않습니다.

```yaml
masking:
  mappings:
    "common":
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"

    "NCDP_NO_MASKING":    # 빈 리스트 또는 정의하지 않음
      # common 룰만 적용됨
```

---

## 문제 해결

### Q1. 마스킹이 적용되지 않아요
**확인사항:**
1. jsonPath가 실제 데이터 구조와 일치하는가?
   - 대소문자 확인
   - 경로 깊이 확인
2. MCI ID가 정확한가?
3. YAML 문법 오류가 없는가?
   - 들여쓰기(2칸 스페이스)
   - 따옴표 누락

**디버깅 방법:**
```java
// MciMaksingController.java의 testMasking 메소드 사용
System.out.println("--- 마스킹 전 원본 데이터 ---");
System.out.println(input);

maskingService.applyMasking("NCDP_MIMEIN10A0", input);

System.out.println("--- 마스킹 후 데이터 ---");
System.out.println(input);
```

---

### Q2. 배열의 일부만 마스킹하고 싶어요
**답변:** 현재는 해당 경로의 모든 배열 요소에 마스킹이 적용됩니다.
특정 인덱스만 마스킹하는 기능은 지원하지 않습니다.

**대안:** 데이터 구조를 변경하여 마스킹 대상과 비대상을 다른 경로에 배치하세요.

---

### Q3. YAML 파일 수정 후 재시작해야 하나요?
**답변:** 네, 애플리케이션을 재시작해야 변경사항이 반영됩니다.
`MaskingProperties`는 애플리케이션 시작 시 한 번만 로드됩니다.

---

### Q4. 여러 필드를 한 번에 마스킹하려면?
**답변:** 각 필드마다 별도의 룰을 정의하세요.

```yaml
masking:
  mappings:
    "NCDP_MULTI":
      - jsonPath: "user.name"
        maskingType: "type_name"
      - jsonPath: "user.rlno"
        maskingType: "type_rlno"
      - jsonPath: "user.account"
        maskingType: "type_account"
```

---

### Q5. 잘못된 경로를 설정하면 에러가 나나요?
**답변:** 아니요, 에러는 발생하지 않습니다.
해당 경로를 찾을 수 없으면 조용히 무시됩니다.

**권장:** 설정 후 반드시 테스트하여 의도한 필드가 마스킹되는지 확인하세요.

---

## 베스트 프랙티스

### 1. Common 룰 적극 활용
모든 MCI ID에 공통으로 적용되는 필드는 `common`에 정의하세요.

**좋은 예:**
```yaml
masking:
  mappings:
    "common":
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
      - jsonPath: "pfminpt.rlno"
        maskingType: "type_rlno"
    "NCDP_MCI_001":
      - jsonPath: "specific.field"
        maskingType: "type_name"
```

**나쁜 예:**
```yaml
masking:
  mappings:
    "NCDP_MCI_001":
      - jsonPath: "pfmidata.rlno"      # 중복
        maskingType: "type_rlno"
      - jsonPath: "specific.field"
        maskingType: "type_name"
    "NCDP_MCI_002":
      - jsonPath: "pfmidata.rlno"      # 중복
        maskingType: "type_rlno"
```

---

### 2. 주석 활용
복잡한 경로나 특이사항은 주석으로 설명하세요.

```yaml
masking:
  mappings:
    "common":
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
      - jsonPath: "pfminpt.rlno"
        maskingType: "type_rlno"

    "NCDP_MIMEIN10A0":
      # 고객 이름 마스킹 (개인정보보호법 준수)
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.custNm"
        maskingType: "type_name"
      # 고객 주민번호 마스킹
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.rlno"
        maskingType: "type_rlno"
```

---

### 3. 일관된 명명 규칙
MCI ID는 일관된 패턴으로 작성하세요.

**권장 패턴:**
- `NCDP_` 접두사 사용
- 대문자 사용
- 의미 있는 이름

**예시:**
- `NCDP_MIMEIN10A0`
- `NCDP_CUSAFT10A0`
- `NCDP_USERINFO01`

---

### 4. 테스트 데이터 준비
새로운 룰을 추가할 때는 반드시 테스트 데이터로 검증하세요.

**테스트 방법:**
1. `/api/maskingTest` 엔드포인트 사용
2. 원본 데이터와 마스킹 후 데이터 비교
3. 의도하지 않은 필드가 마스킹되지 않았는지 확인

---

### 5. 버전 관리
`mciMasking.yml` 파일은 Git 등의 버전 관리 시스템으로 관리하세요.

**이유:**
- 변경 이력 추적
- 문제 발생 시 롤백 가능
- 팀원 간 변경사항 공유

**커밋 메시지 예시:**
```
feat: Add masking rules for NCDP_NEWMCI01

- Add rlno masking for user.info.rlno
- Add name masking for user.info.name
```

---

### 6. 정기적인 검토
분기별로 마스킹 룰을 검토하세요.

**체크리스트:**
- 더 이상 사용하지 않는 MCI ID 정리
- 신규 개인정보 필드 추가 확인
- Common 룰로 통합 가능한 중복 룰 정리

---

## 실전 예제

### 실제 운영 환경 설정 예시
```yaml
masking:
  mappings:
    # ===========================================
    # Common Rules (모든 MCI ID에 자동 적용)
    # ===========================================
    "common":
      # 표준 개인정보 필드
      - jsonPath: "pfmidata.rlno"
        maskingType: "type_rlno"
      - jsonPath: "pfminpt.rlno"
        maskingType: "type_rlno"
      - jsonPath: "pfmidata.custNm"
        maskingType: "type_name"

    # ===========================================
    # MCI별 특화 Rules
    # ===========================================

    # 고객정보 조회 MCI
    "NCDP_MIMEIN10A0":
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.rlno"
        maskingType: "type_rlno"
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.custNm"
        maskingType: "type_name"
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.addr"
        maskingType: "type_address"
      - jsonPath: "MIMEIN_IN_MST.MIMEIN_IN.email"
        maskingType: "type_email"

    # 계좌정보 조회 MCI
    "NCDP_CUSAFT10A0":
      # CUSAFT_IN_SUB는 배열이므로 인덱스 없이 작성
      - jsonPath: "CUSAFT_IN.CUSAFT_IN_SUB.acno"
        maskingType: "type_account"
      - jsonPath: "CUSAFT_IN.CUSAFT_IN_SUB.custNm"
        maskingType: "type_name"

    # 서비스 정보 조회 MCI (중첩 배열 포함)
    "NCDP_SVCIDATA01":
      - jsonPath: "SVCI_MST.svcidata.sub01.subsub01.rlno"
        maskingType: "type_rlno"
      - jsonPath: "SVCI_MST.svcidata.sub01.subsub01.custNm"
        maskingType: "type_name"

    # 테스트용 MCI (마스킹 없음)
    "NCDP_TEST_NOMASK":
      # common 룰만 적용됨
```

---

## 체크리스트

새로운 마스킹 룰을 추가할 때 다음을 확인하세요:

- [ ] 실제 데이터 구조를 확인했는가?
- [ ] jsonPath의 대소문자가 정확한가?
- [ ] 배열 경로에 인덱스를 포함하지 않았는가?
- [ ] YAML 문법 오류가 없는가? (들여쓰기, 따옴표)
- [ ] 공통 필드는 common에 정의했는가?
- [ ] 주석으로 설명을 추가했는가?
- [ ] 테스트를 수행했는가?
- [ ] Git에 커밋했는가?

---

## 참고 문서

- **변경사항 상세:** `docs/MASKING_CHANGELOG.md`
- **소스 코드:** `src/main/java/com/example/playground/config/`
- **테스트 코드:** `src/test/java/com/example/playground/service/MaksingServiceTest.java`

---

## 문의
설정 관련 문의사항은 개발팀에 연락해주세요.

---

## 부록: YAML 문법 요약

### 기본 구조
```yaml
key: value                    # 단순 값

nested:                       # 중첩 객체
  key: value

list:                         # 리스트
  - item1
  - item2

list_of_objects:              # 객체 리스트
  - key1: value1
    key2: value2
  - key1: value3
    key2: value4
```

### 주석
```yaml
# 이것은 주석입니다
key: value    # 라인 끝 주석도 가능
```

### 문자열
```yaml
unquoted: hello world         # 따옴표 없이
single: 'hello world'         # 작은따옴표
double: "hello world"         # 큰따옴표

# 특수문자가 있으면 따옴표 필수
special: "key:value"
```

### 들여쓰기
- **반드시 스페이스 사용** (탭 사용 금지)
- **일관된 들여쓰기** (보통 2칸)

**올바른 예:**
```yaml
masking:
  mappings:
    "common":
      - jsonPath: "pfmidata.rlno"
```

**잘못된 예:**
```yaml
masking:
 mappings:              # 1칸 들여쓰기
   "common":            # 3칸 들여쓰기 (일관성 없음)
      - jsonPath: "pfmidata.rlno"
```

---

**끝**
