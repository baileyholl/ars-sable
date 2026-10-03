package com.hollingsworth.ars_sable.common.datagen;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.spell.EffectMiniaturize;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.datagen.GlyphRecipeProvider;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class GlyphRecipeDatagen extends GlyphRecipeProvider {

    public GlyphRecipeDatagen(DataGenerator generator) {
        super(generator);
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new GlyphRecipeDatagen(generator));
    }

    @Override
    public void collectJsons(CachedOutput cache) {
        add(get(EffectMiniaturize.INSTANCE).withItem(Items.PISTON, 8));

        for (GlyphRecipe recipe : recipes) {
            String path = BuiltInRegistries.ITEM.getKey(recipe.output.getItem()).getPath();
            saveStable(cache, Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipe).getOrThrow(), output.resolve("data/" + ArsSable.MODID + "/recipe/" + path + ".json"));
        }
    }
}
