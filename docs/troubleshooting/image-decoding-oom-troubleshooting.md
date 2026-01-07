# **고해상도 이미지 처리 OOM 트러블슈팅 정리**

*ImageIO 디코딩 단계 메모리 사용 최적화*

---

## **1. 문제 배경**

Dearwith 서비스에서는 모바일 환경에서 촬영된 이미지를 업로드 받아

여러 해상도의 variant(WebP)를 생성한다.

하지만 **고해상도 이미지(12~24MP 이상)** 업로드 시,

variant 생성 과정에서 **JVM OutOfMemoryError(OOM)** 가 발생했다.

문제는 단순히 이미지 개수가 아니라, **이미지 처리 파이프라인의 디코딩 단계**에 있었다.

→ 이미지 개수와 무관하게 단일 요청만으로도 장애가 발생할 수 있는 구조였다.

---

## **2. 문제 현상**

- 고해상도 JPEG 업로드 시 ImageIO.read() 단계에서 OOM 발생
- JVM 힙 사이즈: -Xmx512m (EC2 Free Tier 기준)
- variant 생성이 시작되기도 전에 프로세스 종료
- 비동기/동기 여부와 무관하게 **단일 고해상도 이미지 처리만으로도 OOM 발생**

---

## **3. 원인 분석**

### **3.1 기존 처리 방식의 문제점**

기존 구현에서는 다음과 같은 흐름으로 이미지를 처리했다.

```text
byte[] originalBytes
   ↓
ImageIO.read(originalBytes)   ← 전체 해상도 디코딩
   ↓
BufferedImage (full resolution)
   ↓
리사이징 / 포맷 변환
```
이 방식의 문제는:

- ImageIO.read()가 **원본 이미지를 전체 해상도로 한 번에 디코딩**
- 24MP JPEG → 수십 MB 크기의 픽셀 버퍼가 힙에 적재
- 이후 리사이징 과정에서 **추가 메모리 사용**
- 제한된 힙 환경에서 OOM 발생

👉 즉, **리사이징 이전에 이미 메모리를 초과**하고 있었다.  
→ 이후 최적화가 아무 의미 없을 정도로, 병목이 가장 앞단에 존재했다.
---

## **4. 해결 전략**

### **핵심 목표**

> 디코딩 단계에서부터 메모리 사용량을 제한한다
>

리사이징 이전에 이미 메모리를 다 써버리는 구조를 바꾸기 위해,

**이미지를 “작게 읽는 것” 자체를 목표**로 했다.

---

## **5. 해결 방법**

### **5.1 디코딩 단계 서브샘플링 도입**

ImageIO.read() 대신,

ImageReader + ImageReadParam#setSourceSubsampling()을 사용해

**디코딩 단계에서부터 해상도를 제한**했다.
- 모바일/웹에서 품질 저하 없이 처리 가능한 최대 해상도로 판단되는 **긴 변 2048px** 기준 적용
```java
// 긴 변 기준 최대 2048px로 제한
BufferedImage src =
    ImageDownscaler.readWithMaxLongEdge(originalBytes, 2048);
```
- 원본 해상도 기준으로 ratio 계산
- 디코딩 시 불필요한 픽셀 자체를 읽지 않음
- 힙 메모리에 올라가는 이미지 크기 대폭 감소
---

### **5.2 Thumbnailator를 이용한 2차 안전 축소**

서브샘플링 이후에도 긴 변이 기준을 초과하는 경우를 대비해

Thumbnailator로 한 번 더 안전하게 축소했다.
```java
Thumbnails.of(src)
    .size(targetW, targetH)
    .keepAspectRatio(true)
    .asBufferedImage();
```
---

### **5.3 EXIF Orientation 처리 분리**

디코딩 이후에만 **EXIF 방향 보정**을 수행하도록 분리했다.
```java
src = ImageOrientationNormalizer.normalize(originalBytes, src);
```
- 불필요한 회전 연산 최소화
- 디코딩 + 회전이 동시에 일어나지 않도록 단계 분리

---

### **5.4 GC 부담 완화**

대용량 byte 배열을 더 이상 사용하지 않도록

참조를 명시적으로 해제했다.
```java
originalBytes = null;
```
- 장시간 variant 생성 시 GC 압박 완화
- 힙 회수 타이밍 개선

---

## **6. 결과**

- 12~24MP 고해상도 이미지 처리 시 **OOM 재현 불가**
- JVM 힙 512MB 환경에서도 안정적으로 variant 생성 가능
- 이미지 처리 실패로 인한 서비스 중단 제거
- 비동기 처리 여부와 무관하게 **기본 이미지 파이프라인 안정성 확보**
