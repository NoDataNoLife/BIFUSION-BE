package com.nodatanolife.bifusion_be.domain.recipe.entity.converter;

import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeSettingsFormat;
import com.nodatanolife.bifusion_be.global.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class RecipeSettingsFormatConverter extends EnumNameConverter<RecipeSettingsFormat> {

    public RecipeSettingsFormatConverter() {
        super(RecipeSettingsFormat.class);
    }
}
