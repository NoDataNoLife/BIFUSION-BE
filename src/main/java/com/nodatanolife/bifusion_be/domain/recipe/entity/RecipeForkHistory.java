package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_fork_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeForkHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_fork_id")
    private Long recipeForkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_recipe_id", nullable = false)
    private Recipe forkedRecipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_recipe_id", nullable = false)
    private Recipe originalRecipe;

    @Column(name = "original_title")
    private String originalTitle;

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
