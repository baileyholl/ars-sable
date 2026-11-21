package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;

public class AugmentSublevel extends AbstractAugment {
    public static AugmentSublevel INSTANCE = new AugmentSublevel();

    private AugmentSublevel() {
        super(ArsSable.prefix("glyph_sublevel"), "Target Simulated Level");
    }

    @Override
    public int getDefaultManaCost() {
        return 30;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Allows glyphs to target the simulated level instead of the block or entity that was hit. This allows for other block-moving glyphs like Pull or Knockback to move entire the simulated level around.";
    }
}
