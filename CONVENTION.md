# BIFUSION-BE 코드 컨벤션

## 엔티티 (Entity)

- `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 기본 적용
- `@Builder`는 **생성자 레벨**에 선언
- `@Setter` 금지 — 도메인 의미 있는 변경 메서드만 허용
- `BaseEntity` 상속 — `createdAt`, `updatedAt` 자동 관리
- `@EntityListeners` 직접 선언 금지 (`BaseEntity`에 위임)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    private JobComment(User user, Job job, String content) {
        this.user = user;
        this.job = job;
        this.content = content;
    }

    public static JobComment create(User user, Job job, String content) {
        return JobComment.builder()
                .user(user)
                .job(job)
                .content(content)
                .build();
    }

    public void updateUser(User newUser) {
        this.user = newUser;
    }
}
```

---

## DTO

### Response DTO
- `record` 사용
- 정적 팩토리 메서드 `from()` 내부에서 생성자 호출

```java
public record JobCommentResponse(
        Long commentId,
        Long userId,
        String author,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static JobCommentResponse from(JobComment comment) {
        return new JobCommentResponse(
                comment.getId(),
                comment.getUser().getUserId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
```

### Request DTO
- `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 검증 어노테이션 적용 (`@NotBlank`, `@NotNull` 등)

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobCommentRequest {

    @NotBlank(message = "댓글 내용은 비어있을 수 없습니다.")
    private String content;
}
```

---

## 서비스 (Service)

- 인터페이스 + `Impl` 구조
- 클래스 레벨 `@Transactional` 선언
- 조회 메서드는 `@Transactional(readOnly = true)`
- 메서드 레벨 `@Transactional` 중복 선언 금지
- 비즈니스 로직은 서비스 레이어에서 처리 — 엔티티에 넣지 않음

```java
@Service
@RequiredArgsConstructor
@Transactional
public class JobCommentServiceImpl implements JobCommentService {

    @Override
    @Transactional(readOnly = true)
    public JobCommentResponse getJobComment(Long commentId) { ... }

    @Override
    public JobCommentResponse createJobComment(...) { ... }
}
```

### 인증된 유저 조회 — 존재 검증 생략

- 컨트롤러에서는 `@AuthenticationPrincipal`로부터 **userId만** 꺼내 서비스로 전달
- **인증을 통과한 유저는 존재가 보장**되므로, 서비스단에서 `findById(userId).orElseThrow(USER_NOT_FOUND)` 같은 존재 재검증은 하지 않는다
- 대신 `userRepository.getReferenceById(userId)`로 참조(프록시)만 확보한다
  - 필드 미접근(FK로만 사용)이면 **조회 쿼리 자체가 생략**된다
  - 수정 시 프록시도 dirty checking으로 정상 반영되고, 필드 접근 시 lazy 초기화된다 (트랜잭션 내에서 DTO 변환을 끝낼 것 — 밖에서 접근하면 `LazyInitializationException`)
- 서비스의 책임은 존재 재검증이 아니라 **권한 체크**다

```java
// O — 인증된 본인: 존재 보장, 참조만 확보
public ExpertQnaResponse createQna(Long userId, ExpertQnaRequest request) {
    User user = userRepository.getReferenceById(userId);   // 작성자 FK로만 사용 → 쿼리 생략
    ExpertQnA qna = ExpertQnA.create(user, request.getTitle(), request.getContent());
    ...
}

// X — 인증된 본인인데 불필요한 존재 검증 쿼리
User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
```

- **예외**: `@PathVariable` 등으로 받는 **임의의 타인 userId / 리소스 id**는 존재가 보장되지 않으므로 `findById(...).orElseThrow(...)`로 검증한다

```java
// O — 타인 프로필 조회: PathVariable userId는 존재 검증 필요
public MyPageBasicResponse getMyPageBasic(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    return MyPageBasicResponse.from(user);
}
```

---

## 예외 처리 (Exception)

- `BusinessException` 서브클래스 또는 `BusinessException(ErrorCode.XXX)` 직접 사용
- `ErrorCode` enum으로 에러 코드 관리
- `GlobalExceptionHandler`에서 표준 Java 예외 (`IllegalStateException` 등) 직접 핸들링 금지

```java
// 자주 쓰이는 에러는 서브클래스
throw new ProjectAccessDeniedException();

// 단순 one-off 에러는 직접 사용
throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
```

---

## 패키지 구조

도메인 기반 패키지 구조를 따른다.

```
com.nodatanolife.bifusion_be
├── domain
│   ├── user
│   ├── project
│   ├── job
│   ├── expert
│   └── community
└── global
    ├── exception
    ├── response
    └── security
```

---

## 기타

- DB 컨벤션: PostgreSQL, 단수 테이블명
- 폴리모픽 참조: `target_id + target_type` 패턴
- 설정 파일: `application.yml` (공통, 커밋) + `application-local.yml` (로컬 전용, Git 무시)
- 시크릿: `.env` 관리
