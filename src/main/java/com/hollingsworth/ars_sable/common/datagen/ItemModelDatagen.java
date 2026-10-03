package com.hollingsworth.ars_sable.common.datagen;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class ItemModelDatagen extends ItemModelProvider {

    public ItemModelDatagen(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ArsSable.MODID, existingFileHelper);
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeClient(), new ItemModelDatagen(generator.getPackOutput(), event.getExistingFileHelper()));
    }

    @Override
    protected void registerModels() {
        for (ResourceLocation registryName : GlyphRegistry.getSpellpartMap().keySet()) {
            if (registryName.getNamespace().equals(ArsSable.MODID)) {
                basicItem(registryName);
            }
        }
    }
}
