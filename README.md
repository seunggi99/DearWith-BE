# Dearwith

**Event & Community Platform for Idol Fans**

DearWith는 실제 서비스 운영을 목표로  
백엔드 아키텍처, 인증·보안, 이미지 처리, 성능 최적화까지  
**운영 관점에서 설계·구현한 프로젝트**입니다.

### Links
배포 URL : [바로가기](https://www.dearwith.kr/)  
API Docs : [바로가기](https://api.dearwith.kr/swagger-ui/index.html#/)  

## Overview

- 아이돌 생일 카페·이벤트 정보를 등록·탐색·공유하는 팬 커뮤니티 플랫폼
- 트래픽 증가와 운영 이슈를 전제로 한 백엔드 중심 설계
- 단순 기능 구현보다 **구조적 문제 해결**에 초점

## System Architecture
동적 API 트래픽과 이미지 CDN 트래픽을 분리해
백엔드 부하와 외부 I/O를 최소화한 서비스 아키텍처

## ERD Diagram
운영과 확장을 고려해 상태와 관계를 명확히 분리한 도메인 모델

## CI / CD 
빌드·배포 과정을 자동화해 일관성과 재현성을 확보한 배포 파이프라인

## Key Engineering Decisions

Dearwith에서 발생한 실제 문제를 기반으로
설계와 구조를 개선한 주요 기술 기록입니다.

### 🔥 Hot Event & Recommendation System
Redis ZSET 기반 랭킹 집계와 ID 기반 2단계 조회로  
메인 페이지 성능과 추천 신뢰도를 동시에 확보했습니다.  
→ [문서 보기](./docs/hot-ranking-system.md)

### 🖼 Image Upload Pipeline
Presigned URL과 S3 상태 전이(tmp → inline → trash)를 도입해  
대용량 이미지 업로드의 정합성과 운영 안정성을 설계했습니다.  
→ [문서 보기](./docs/image-upload-pipeline.md)

### 🔔 Push Notification Architecture
FCM 멀티캐스트 전송과 토큰 라이프사이클 관리로  
대규모 푸시 전송을 운영 가능한 시스템으로 구성했습니다.  
→ [문서 보기](./docs/push-notification-architecture.md)

### 🔐 WebView / Safari Authentication Issue
iOS WebView 환경에서 발생한 쿠키 기반 인증 불일치를 분석하고  
플랫폼별 인증 전달 방식 분리로 문제를 해결했습니다.  
→ [문서 보기](./docs/auth-webview-cookie-issue.md)

### 💥 High-Resolution Image OOM Troubleshooting
고해상도 이미지 처리 중 발생한 JVM OOM의 원인을 디코딩 단계에서 찾아  
서브샘플링 기반 이미지 파이프라인으로 안정성을 확보했습니다.  
→ [문서 보기](./docs/image-decoding-oom-troubleshooting.md)

### ⚙️ Asynchronous Image Processing
이미지 variant 생성을 비동기로 분리해  
응답 시간을 단축하고 트랜잭션 경계를 명확히 했습니다.  
→ [문서 보기](./docs/image-processing-async-troubleshooting.md)

### 🚀 Main Page API Performance Optimization
N+1 제거를 넘어 조회 구조를 재설계하고  
k6 부하 테스트로 성능 개선을 정량 검증했습니다.  
→ [문서 보기](./docs/main-api-performance.md)

## Tech Stack

- Backend: Java 17, Spring Boot, JPA, QueryDSL
- Database: MySQL (RDS), Redis
- Infra: AWS EC2, S3, CloudFront, Nginx (HTTPS, Reverse Proxy)
- Container: Docker, Docker Compose
- CI/CD: Jenkins