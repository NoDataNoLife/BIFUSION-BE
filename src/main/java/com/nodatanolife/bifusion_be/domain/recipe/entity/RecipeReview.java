package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_review_id")
    private Long recipeReviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating")
    private Short rating;

    @Column(name = "content")
    private String content;

    @Builder
    private RecipeReview(Recipe recipe, User user, Short rating, String content) {
        this.recipe = recipe;
        this.user = user;
        this.rating = rating;
        this.content = content;
    }

    public static RecipeReview create(Recipe recipe, User user, Short rating, String content) {
        return RecipeReview.builder()
                .recipe(recipe)
                .user(user)
                .rating(rating)
                .content(content)
                .build();
    }
    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }
}
