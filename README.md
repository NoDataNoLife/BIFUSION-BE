# BIFUSION-BE

NoDataNoLife 팀의 BIFUSION 백엔드 프로젝트입니다.

## 기술 스택

- Java 21
- Spring Boot 3.x
- PostgreSQL 16
- Gradle
- Docker

---

## 프로젝트 구조

```
BIFUSION-BE/
├── src/
│   ├── main/
│   │   ├── java/          # Java 소스 코드
│   │   └── resources/
│   │       └── application.yaml  # Spring 설정
│   └── test/              # 테스트 코드
├── docker-compose.yml     # PostgreSQL 컨테이너 설정
├── build.gradle           # Gradle 빌드 설정
├── .env                   # 환경변수 (git 제외)
└── .env.example           # 환경변수 템플릿
```

---
## 개발 환경 설정

### 1. 프로젝트 클론

```bash
git clone https://github.com/NoDataNoLife/BIFUSION-BE.git
cd BIFUSION-BE
```

### 2. 환경변수 설정

`.env.example` 파일을 복사해서 `.env` 파일을 만듭니다.


### 3. Docker로 PostgreSQL 실행

Docker Desktop이 실행 중인지 확인 후:

```bash
docker-compose up -d
```

정상 실행 확인:
```bash
docker ps
```

아래와 같이 `bifusion-db`가 보이면 성공:
```
CONTAINER ID   IMAGE         ...   NAMES
xxxxxxxxxxxx   postgres:16   ...   bifusion-db
```


## 실행 확인

| 항목 | 주소 |
|------|------|
| 백엔드 API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
