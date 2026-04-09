package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_fork_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeForkHistory {

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
