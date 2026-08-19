package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 레시피 포크 이력 엔티티.
 * 누가 어떤 원본 레시피를(originalRecipe) 언제 포크해서 어떤 사본을(forkedRecipe) 만들었는지 기록한다.
 * 동일 사용자가 같은 원본을 중복으로 포크하지 못하도록 막는 유니크 체크에도 사용된다
 * (RecipeForkServiceImpl#forkRecipe의 existsByUserAndOriginalRecipe 검증).
 */
@Entity
@Table(name = "recipe_fork_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeForkHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_fork_id")
    private Long recipeForkId;

    // 포크를 실행한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 포크로 새로 생성된 사본 레시피
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_recipe_id", nullable = false)
    private Recipe forkedRecipe;

    // 포크 대상이 된 원본 레시피
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_recipe_id", nullable = false)
    private Recipe originalRecipe;

    // 포크 시점의 원본 제목 (원본이 이후 삭제/수정되어도 이력이 남도록 스냅샷 보관)
    @Column(name = "original_title")
    private String originalTitle;

    /**
     * 회원 탈퇴 시 작성자를 탈퇴 계정으로 치환한다.
     */
    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }


    @Builder
    private RecipeForkHistory(User user, Recipe forkedRecipe, Recipe originalRecipe, String originalTitle) {
        this.user = user;
        this.forkedRecipe = forkedRecipe;
        this.originalRecipe = originalRecipe;
        this.originalTitle = originalTitle;
    }

    public static RecipeForkHistory create(User user, Recipe forkedRecipe, Recipe originalRecipe, String originalTitle) {
        return RecipeForkHistory.builder()
                .user(user)
                .forkedRecipe(forkedRecipe)
                .originalRecipe(originalRecipe)
                .originalTitle(originalTitle)
                .build();
    }
}
