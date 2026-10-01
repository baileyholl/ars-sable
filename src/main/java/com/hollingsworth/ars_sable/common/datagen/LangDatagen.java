package com.hollingsworth.ars_sable.common.datagen;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.registry.ModBlockRegistry;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class LangDatagen extends LanguageProvider {

    public LangDatagen(PackOutput output) {
        super(output, ArsSable.MODID, "en_us");
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeClient(), new LangDatagen(generator.getPackOutput()));
    }

    @Override
    protected void addTranslations() {
        for (AbstractSpellPart spellPart : GlyphRegistry.getSpellpartMap().values()) {
            ResourceLocation registryName = spellPart.getRegistryName();
            if (!registryName.getNamespace().equals(ArsSable.MODID)) {
                continue;
            }
            add(spellPart.getLocalizationKey(), spellPart.getName());
            add(ArsSable.MODID + ".glyph_desc." + registryName.getPath(), spellPart.getBookDescription());
        }

        addItem(ModBlockRegistry.MINIATURE_SUBLEVEL, "Miniature Sublevel");
        add("ars_sable.miniature_sublevel.blocks", "Blocks: %s");
        add("ars_sable.miniature_sublevel.size", "Size: %sx%sx%s");
        add("ars_sable.miniaturize.missing", "This miniature sublevel no longer exists.");
        add("ars_sable.miniaturize.failed", "Failed to place.");
        add("ars_sable.miniaturize.no_space", "Not enough space to place.");
    }
}
