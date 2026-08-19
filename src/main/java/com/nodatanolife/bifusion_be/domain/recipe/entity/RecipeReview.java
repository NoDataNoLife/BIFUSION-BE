package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 레시피 댓글(리뷰) 엔티티. 최상위 댓글과 대댓글을 같은 테이블에서
 * {@link #parent} 자기참조로 구분해서 관리한다 (depth는 최대 1단계).
 */
@Entity
@Table(name = "recipe_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_review_id")
    private Long recipeReviewId;

    // 어떤 레시피에 달린 댓글인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 최상위 리뷰면 null, 대댓글이면 대상 리뷰를 가리킨다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_review_id")
    private RecipeReview parent;

    // 최상위 리뷰만 별점을 가진다. 대댓글은 항상 null.
    @Column(name = "rating")
    private Short rating;

    // 댓글 내용
    @Column(name = "content")
    private String content;

    @Builder
    private RecipeReview(Recipe recipe, User user, RecipeReview parent, Short rating, String content) {
        this.recipe = recipe;
        this.user = user;
        this.parent = parent;
        this.rating = rating;
        this.content = content;
    }

    /**
     * 댓글/대댓글 생성 팩토리 메서드. parent가 null이면 최상위 댓글, 있으면 대댓글이 된다.
     * 별점 유무에 대한 검증은 서비스단(RecipeReviewServiceImpl#validateRating)에서 처리한다.
     */
    public static RecipeReview create(Recipe recipe, User user, RecipeReview parent, Short rating, String content) {
        return RecipeReview.builder()
                .recipe(recipe)
                .user(user)
                .parent(parent)
                .rating(rating)
                .content(content)
                .build();
    }

    /**
     * 대댓글 여부. parent가 있으면 대댓글이다.
     */
    public boolean isReply() {
        return parent != null;
    }

    /**
     * 회원 탈퇴 시 작성자를 탈퇴 계정으로 치환한다.
     */
    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    /**
     * 댓글 내용/별점 수정. 별점 유무 검증은 호출부(서비스)에서 선행한다.
     */
    public void update(Short rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
