package com.nodatanolife.bifusion_be.recipe;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 레시피 등록 시 대표 이미지(thumbnailFileId).
 */
class RecipeThumbnailIntegrationTest extends IntegrationTestSupport {

    @Test
    void 본인이_올린_이미지를_대표_이미지로_등록하면_영구_보관으로_바뀌고_응답에_나온다() {
        User author = user("author@test.com", 0);
        File image = tempFile(author, "image/png", 1000);

        Long recipeId = register(author, image.getFileId());

        assertThat(recipeService.getRecipeDetail(recipeId, author.getUserId()).bannerUrl()).isEqualTo(image.getS3Key());
        assertThat(recipeService.getRecipeList("LATEST", null, 0, 10).getContent().get(0).bannerUrl()).isEqualTo(image.getS3Key());
        assertThat(fileRepository.findById(image.getFileId()).orElseThrow().getIsTemp()).isFalse();
    }

    @Test
    void 대표_이미지_없이도_등록된다() {
        User author = user("author@test.com", 0);

        Long recipeId = register(author, null);

        assertThat(recipeService.getRecipeDetail(recipeId, author.getUserId()).bannerUrl()).isNull();
    }

    @Test
    void 남이_올린_파일이나_이미지가_아닌_파일_없는_파일은_대표_이미지로_쓸_수_없다() {
        User author = user("author@test.com", 0);
        User other = user("other@test.com", 0);
        File othersImage = tempFile(other, "image/png", 1000);
        File csv = tempFile(author, "text/csv", 1000);
        long before = recipeRepository.count();

        assertError(() -> register(author, othersImage.getFileId()), ErrorCode.FILE_READ_FAILED);
        assertError(() -> register(author, csv.getFileId()), ErrorCode.RECIPE_THUMBNAIL_NOT_IMAGE);
        assertError(() -> register(author, 999_999L), ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(recipeRepository.count()).isEqualTo(before);
    }

    private Long register(User author, Long thumbnailFileId) {
        RecipeRegisterRequest request = new RecipeRegisterRequest("레시피", "설명", augmentationSettings(), null,
                null, List.of(), List.of(), null, true, thumbnailFileId);
        String code = recipeService.registerRecipe(request, author.getUserId()).recipeId();
        return Long.parseLong(code.substring("REC-".length()));
    }

    private static void assertError(ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(errorCode);
    }
}
