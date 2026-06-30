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
