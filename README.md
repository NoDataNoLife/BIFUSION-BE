# BIFUSION-BE

NoDataNoLife 팀의 BIFUSION 백엔드 프로젝트입니다.

## 기술 스택

- Java 21
- Spring Boot 3.x
- PostgreSQL 16
- Gradle
- Docker

---

## 사전 설치 (처음 한 번만)

### 1. Git 설치

**Windows**
1. https://git-scm.com/download/win 접속
2. 다운로드 후 설치 (기본 옵션으로 Next)

**Mac**
```bash
brew install git
```

설치 확인:
```bash
git --version
```

---

### 2. Java 21 설치

**Windows**
1. https://adoptium.net/ 접속
2. "Temurin 21 (LTS)" 다운로드
3. 설치 시 "Set JAVA_HOME variable" 체크

**Mac**
```bash
brew install openjdk@21
```

설치 확인:
```bash
java -version
```
`openjdk version "21.x.x"` 출력되면 성공

---

### 3. Docker Desktop 설치

**Windows**
1. https://www.docker.com/products/docker-desktop/ 접속
2. "Download for Windows" 클릭
3. 설치 후 재부팅
4. Docker Desktop 실행 (시스템 트레이에 고래 아이콘 확인)

**Mac**
1. https://www.docker.com/products/docker-desktop/ 접속
2. "Download for Mac" 클릭 (Intel / Apple Chip 선택)
3. 설치 후 Docker Desktop 실행

설치 확인:
```bash
docker --version
docker-compose --version
```

---

## 개발 환경 설정

### 1. 프로젝트 클론

```bash
git clone https://github.com/NoDataNoLife/BIFUSION-BE.git
cd BIFUSION-BE
```

---

### 2. 환경변수 설정

`.env.example` 파일을 복사해서 `.env` 파일을 만듭니다.

**Windows (PowerShell)**
```powershell
copy .env.example .env
```

**Windows (CMD)**
```cmd
copy .env.example .env
```

**Mac / Linux**
```bash
cp .env.example .env
```

`.env` 파일을 열어서 아래와 같이 수정합니다:
```
DB_USERNAME=admin
DB_PASSWORD=0000
```

---

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

---

### 4. Spring Boot 애플리케이션 실행

**Windows (PowerShell / CMD)**
```cmd
.\gradlew.bat bootRun
```

**Mac / Linux**
```bash
./gradlew bootRun
```

아래 로그가 보이면 성공:
```
Started BifusionBeApplication in X.XXX seconds
```

---

## 실행 확인

| 항목 | 주소 |
|------|------|
| 백엔드 API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |

---

## 종료 방법

### Spring Boot 종료
터미널에서 `Ctrl + C`

### PostgreSQL 종료
```bash
docker-compose down
```

---

## 매일 개발 시작할 때

```bash
# 1. Docker Desktop 실행 (시스템 트레이 확인)

# 2. DB 실행
docker-compose up -d

# 3. 백엔드 실행
.\gradlew.bat bootRun   # Windows
./gradlew bootRun       # Mac
```

---

## 문제 해결

### Docker 명령어가 안 될 때
- Docker Desktop이 실행 중인지 확인
- 재부팅 후 다시 시도

### DB 연결 오류
```bash
# 컨테이너 상태 확인
docker ps

# 컨테이너가 없으면 다시 실행
docker-compose up -d

# 로그 확인
docker logs bifusion-db
```

### 포트 충돌 (5432 already in use)
```bash
# 기존 컨테이너 정리
docker-compose down

# 다시 실행
docker-compose up -d
```

### gradlew 실행 권한 오류 (Mac)
```bash
chmod +x gradlew
./gradlew bootRun
```

### PostgreSQL 직접 접속 (DB 확인용)
```bash
docker exec -it bifusion-db psql -U admin -d bifusion
```

SQL 명령어 예시:
```sql
\dt          -- 테이블 목록
\q           -- 종료
```

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
