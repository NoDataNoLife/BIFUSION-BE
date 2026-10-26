package com.nodatanolife.bifusion_be.recipe;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.ForkedFromInfo;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeListType;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeForkService;
import com.nodatanolife.bifusion_be.domain.user.dto.response.MyActivityListResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.service.MyActivityService;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 포크한 레시피의 원본 포크 수·원본 정보, 복제 사본을 내 활동에서 제외.
 */
class RecipeForkDisplayIntegrationTest extends IntegrationTestSupport {

    @Autowired
    RecipeForkService recipeForkService;
    @Autowired
    MyActivityService myActivityService;

    @Test
    void 포크한_레시피는_원본의_포크_수와_원본_정보를_보여준다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 100);
        User other = user("other@test.com", 100);
        Long originalId = recipe(author, null, true);
        Long copyId = recipeForkService.forkRecipe(originalId, forker.getUserId()).newRecipeId();
        recipeForkService.forkRecipe(originalId, other.getUserId());

        RecipeDetailResponse copy = recipeService.getRecipeDetail(copyId, forker.getUserId());
        assertThat(copy.forkCount()).isEqualTo(2);
        ForkedFromInfo forkedFrom = copy.forkedFrom();
        assertThat(forkedFrom.recipeId()).isEqualTo(originalId);
        assertThat(forkedFrom.title()).isEqualTo("레시피");
        assertThat(forkedFrom.authorNickname()).isEqualTo("author");
        assertThat(forkedFrom.isDeleted()).isFalse();

        RecipeDetailResponse original = recipeService.getRecipeDetail(originalId, author.getUserId());
        assertThat(original.forkCount()).isEqualTo(2);
        assertThat(original.forkedFrom()).isNull();

        var forkedList = recipeService.getMyRecipes(RecipeListType.FORKED, forker.getUserId(), 0, 10).getContent();
        assertThat(forkedList.get(0).forkCount()).isEqualTo(2L);
        assertThat(forkedList.get(0).forkedFrom().recipeId()).isEqualTo(originalId);
    }

    @Test
    void 커뮤니티_목록의_다시_등록한_포크도_원본_기준으로_보여준다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 0);
        Long originalId = recipe(author, null, true);
        Long reRegisteredId = recipe(forker, originalId, true);

        var item = recipeService.getRecipeList("LATEST", null, 0, 10).getContent().stream()
                .filter(r -> r.recipeId().equals(reRegisteredId)).findFirst().orElseThrow();

        assertThat(item.forkCount()).isEqualTo(1);
        assertThat(item.forkedFrom().recipeId()).isEqualTo(originalId);
    }

    @Test
    void 원본이_삭제되면_isDeleted로_알려준다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 100);
        Long originalId = recipe(author, null, true);
        Long copyId = recipeForkService.forkRecipe(originalId, forker.getUserId()).newRecipeId();

        recipeService.deleteRecipe(originalId, author.getUserId());

        assertThat(recipeService.getRecipeDetail(copyId, forker.getUserId()).forkedFrom().isDeleted()).isTrue();
    }

    @Test
    void 내_활동에서_복제한_사본은_빠지고_다시_등록한_레시피는_남는다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 100);
        Long originalId = recipe(author, null, true);
        Long copyId = recipeForkService.forkRecipe(originalId, forker.getUserId()).newRecipeId();
        Long reRegisteredId = recipe(forker, originalId, true);
        Long ownId = recipe(forker, null, false);

        assertThat(myActivityService.getMyActivities(forker.getUserId(), null, 20).items())
                .filteredOn(item -> item instanceof MyActivityListResponse.RecipeItem)
                .extracting(MyActivityListResponse.Item::activityId)
                .containsExactlyInAnyOrder(reRegisteredId, ownId)
                .doesNotContain(copyId);
    }
}
