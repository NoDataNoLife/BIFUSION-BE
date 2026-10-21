# BIFUSION-BE
의료 데이터 부족 문제를 해결하는 **AI 증강·학습·추론 올인원 플랫폼**

<br/>

## 프로젝트 소개

### 배경

생의학·의료 영상은 라벨링 비용이 크고, 희귀질환이나 기관별 데이터는 소량인 경우가 많습니다.
대량 데이터에 의존하는 기존 딥러닝 분류를 그대로 적용하기 어렵다는 근본적인 한계가 있습니다.

### 목표

- 사용자가 업로드한 **소량의 생의학 이미지를 증강**해 유효한 학습 데이터를 생성하고
- 이를 바탕으로 **few-shot 이진 분류 모델**을 도출해, 분류 서비스와 모델 자체를 제공합니다.
- 생성된 데이터셋과 모델을 연구자 간에 **공유·재사용(Fork)** 하고, **전문가가 검수**하며, **팀을 모집·협업**할 수 있는 커뮤니티를 결합합니다.

소량 데이터 환경에서의 진단 모델 구축부터 검증·공유·협업까지 전 과정을 하나의 웹 플랫폼으로 통합해,
비전문가와 소규모 연구진의 의료 AI 진입장벽을 낮추는 것을 지향합니다.

<br/>

## 주요 기능

### 백엔드

| 기능 | 내용 |
| --- | --- |
| 인증/인가 | Google OAuth2 + JWT(HttpOnly 쿠키) 기반 로그인 |
| 프로젝트 협업 | 이메일 기반 프로젝트 초대, 수락·만료 스케줄러 |
| AI 작업(Job) 관리 | 증강·학습·추론 작업 생성/조회/재시도/취소, ML 서버 상태 폴링 스케줄러 |
| 전문가 인증·검수 | 신청 → 시작 → 코멘트 → 승인/거절 → 성과 집계의 다단계 상태 전이 워크플로우 |
| 커뮤니티 | 팀원 모집 · 레시피 Fork · 데이터셋 · Q&A 4개 도메인 CRUD 및 검색 |
| 알림 | 여러 이벤트를 통합한 알림 시스템, 읽음 처리 |
| 마이페이지 | 내 활동 조회(커서 기반 페이지네이션), 활동 공개/비공개, 공개 프로필 |
| 파일 | AWS S3 private 버킷 + Presigned URL 기반 객체 접근 |
| 공통 기반 | 공통 응답 포맷, JPA Auditing, 전역 예외 처리로 전 도메인이 일관되게 확장되는 구조 |

### AI 연동

BE는 ML 서버(FastAPI + Celery 기반, RunPod)를 REST로 호출해 증강 → 학습 → 추론 작업을 위임하고, 작업 상태와 결과를 DB에 관리합니다.
FE는 ML 서버를 직접 호출하지 않고 BE API만 사용합니다.

```
1. FE  → BE      Job 생성 요청
2. BE  → S3      입력 이미지 다운로드 → Base64 변환
3. BE  → ML      실행 요청 (X-API-Key 인증) → ML 작업 ID 수신
4. ML  → S3      결과 파일 업로드 (ml-results/)
5. BE  → ML      폴링 스케줄러가 상태 조회 → 완료 시 결과와 파일 key(bucket/key)를 DB에 반영
6. FE  → BE      Job 상태 · 결과 조회 (DB 값, 다운로드 URL은 조회 시 BE가 presign)
```

- **작업 생성** (`POST /api/v1/projects/{projectId}/jobs`)
  1. 요청 검증 후 `PENDING` Job 생성, 입력 파일을 Job에 연결 (트랜잭션 1)
  2. S3에서 입력 이미지를 내려받아 JPG/PNG 시그니처 검사 · 용량 상한(20MB) 확인 후 Base64로 변환, ML 서버에 실행 요청 (트랜잭션 밖)
  3. 접수되면 ML 작업 ID를 기록하고, 실패하면 실패 사유를 Job에 남김 (트랜잭션 2)
  - 수십 MB 요청과 최대 60초 응답 대기 동안 DB 커넥션을 점유하지 않도록 ML 호출을 트랜잭션 밖에서 수행
  - `Idempotency-Key` 헤더로 중복 클릭·네트워크 재전송 시 같은 작업을 반환
  - GPU 작업 중복을 막기 위해 ML 자동 재시도는 하지 않음
- **상태 폴링** (`MlJobPollingScheduler`)
  - `PENDING`·`RUNNING` Job을 ML 서버에 주기적으로 조회해 상태·진행 단계를 DB에 반영
  - `fixedDelay`로 실행이 겹치지 않고, Job 하나의 예외가 나머지 처리를 막지 않음
  - 실행 요청 전 서버 중단(10분), ML 대기(6시간), 실행 정체(72시간)를 타임아웃으로 실패 처리
- **결과 저장**
  - 증강: 결과 데이터셋·미리보기 파일을 File로 등록 → 학습 입력으로 사용
  - 학습: 학습 완료 모델(TrainedModel)과 지표(정확도·F1·AUROC·손실) 저장 → 추론 입력으로 사용
  - 추론: 이미지별 예측 결과(InferenceResult) 저장
  - ML이 SUCCESS를 보내도 저장할 수 없는 결과(result 누락 등)면 가짜 성공으로 남기지 않고 실패 처리
  - 결과 파일은 ML이 전달한 `artifacts`(bucket/key)로 받고 DB에는 key만 저장 (ML은 presigned URL을 만들지 않음)
  - 설정된 버킷의 `ml-results/` 경로이면서 S3에 실제로 있는 파일만 연결, 그 외에는 해당 파일만 연결하지 않음 (Job은 SUCCESS 유지)
- **재시도 · 취소**
  - 실패·취소된 Job을 같은 입력으로 다시 실행 (프로젝트 멤버 누구나)
  - 취소는 ML 취소 요청 후 상태 반영, 폴러와 동시 변경은 낙관적 락으로 한 번 재판단
  - `ML_CANCEL_ENABLED=false`면 ML 취소 엔드포인트 배포 전까지 취소 API 비활성
- **설정** — ML 서버 주소·키, 타임아웃, 파라미터 기본값·허용 범위는 `application.yml`의 `ml.*`로 관리

### 신뢰성 설계

- 학습된 모델·유효 데이터·유효 입력이 없으면 가짜 결과를 내지 않고 **명시적으로 실패(fail-loud)**
- S3 객체 접근을 **Presigned URL**로 전환해 버킷을 private으로 유지

<br/>

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Persistence | Spring Data JPA |
| Database | PostgreSQL 16 |
| Security | Spring Security · JWT (jjwt 0.12) · OAuth2 (Google) |
| Storage | AWS S3 (Spring Cloud AWS · Presigned URL) |
| Mail | Spring Mail (SMTP) |
| API Docs | SpringDoc (Swagger UI) |
| Build | Gradle |
| Infra | AWS EC2 · Docker Compose |

<br/>

## 프로젝트 구조

```
src/main/java/com/nodatanolife/bifusion_be
├── BifusionBeApplication.java
├── domain/                      # 도메인별 패키지 (entity · dto · repository · service · controller)
│   ├── auth/                    # 인증 · 로그인
│   ├── user/                    # 회원 · 마이페이지
│   ├── project/                 # 프로젝트 · 초대
│   ├── dataset/                 # 데이터셋
│   ├── recipe/                  # 레시피
│   ├── recruitment/             # 팀원 모집
│   ├── qna/                     # 전문가 Q&A
│   ├── expert/                  # 전문가 인증 · 검수
│   ├── job/                     # AI 작업 (증강 · 학습 · 추론) · ML 서버 연동
│   ├── tag/                     # 태그
│   ├── notification/            # 알림
│   ├── activityLog/             # 활동 로그
│   └── file/                    # 파일 업로드 · Presigned URL
└── global/                      # 공통 인프라
    ├── config/                  # SecurityConfig · 공통 설정
    ├── security/                # JWT · OAuth2 · 인증 유틸
    ├── entity/                  # BaseEntity (Auditing)
    ├── exception/               # ErrorCode · GlobalExceptionHandler · BusinessException
    ├── response/                # CommonResponse
    ├── converter/               # 공통 컨버터
    └── mail/                    # 메일 발송

src/main/resources
├── application.yml              # 공통 설정 (S3 · ML 서버 등)
├── application-local.yml        # 로컬 프로필 (DB · JPA · AWS)
└── application-prod.yml         # 운영 프로필 (EC2 IAM Role 사용)
```

<br/>

## 로컬 개발 환경 셋팅

### 사전 준비

- **Docker Desktop** 설치
- **Java 21** 설치

### 실행 방법

1. **레포 clone**

   ```bash
   git clone https://github.com/NoDataNoLife/BIFUSION-BE.git
   cd BIFUSION-BE
   ```

2. **환경변수 파일 준비** — `.env.example`을 복사한 뒤 `.env` 파일을 생성해 값을 채웁니다.

   ```bash
   cp .env.example .env
   ```

   | 변수 | 설명 |
   | --- | --- |
   | `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL 계정 |
   | `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | S3 접근 키 (로컬 전용) |
   | `S3_BUCKET` | S3 버킷 이름 (미설정 시 기본 버킷 사용) |
   | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
   | `JWT_SECRET` | JWT 서명 시크릿 |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | 메일 발송 계정 |
   | `ML_BASE_URL` | ML 서버 주소 (비어 있으면 AI 작업 요청이 연결 실패로 처리됨) |
   | `ML_API_KEY` | ML 서버 API 키 (`X-API-Key` 헤더로 전송) |

3. **DB 실행** — docker compose로 PostgreSQL 컨테이너를 띄웁니다.

   ```bash
   docker compose up -d
   ```

   > 호스트 `5432` 포트로 노출됩니다. `docker ps`에 `bifusion-db`가 보이면 정상입니다.

4. **애플리케이션 실행**

   ```bash
   ./gradlew bootRun
   ```

   > `local` 프로필이 기본 활성화되며, JPA `ddl-auto: update`로 스키마가 자동 반영됩니다.

<br/>

### 접속 정보

| 항목 | URL |
| --- | --- |
| 애플리케이션 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI 문서 | http://localhost:8080/api-docs |
| PostgreSQL | localhost:5432 |
