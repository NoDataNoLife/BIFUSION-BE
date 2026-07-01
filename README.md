# BIFUSION-BE
의료 데이터 부족 문제를 해결하는 **AI 증강·학습·추론 올인원 플랫폼**

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

<br/>

## 프로젝트 구조

```
src/main/java/com/nodatanolife/bifusion_be
├── BifusionBeApplication.java
├── domain/                      # 도메인별 패키지 (entity · dto · repository · service · controller)
│   ├── auth/                    # 인증 · 로그인
│   ├── user/                    # 회원
│   ├── project/                 # 프로젝트
│   ├── dataset/                 # 데이터셋
│   ├── recipe/                  # 레시피
│   ├── recruitment/             # 팀원 모집
│   ├── qna/                     # 전문가 Q&A
│   ├── expert/                  # 전문가
│   ├── job/                     # 채용
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
├── application.yml              # 공통 설정
└── application-local.yml        # 로컬 프로필 (DB · JPA · AWS)
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
   | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
   | `JWT_SECRET` | JWT 서명 시크릿 |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | 메일 발송 계정 |

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
