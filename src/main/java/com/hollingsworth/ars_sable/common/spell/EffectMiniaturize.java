package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.helper.MiniatureSublevelHelper;
import com.hollingsworth.ars_sable.common.helper.SublevelSpellHelper;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;

import java.util.Set;

public class EffectMiniaturize extends AbstractEffect {
    public static EffectMiniaturize INSTANCE = new EffectMiniaturize();

    private EffectMiniaturize() {
        super(ArsSable.prefix("glyph_miniaturize"), "Miniaturize");
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }
        SublevelSpellHelper.SublevelHit hit = SublevelSpellHelper.getSublevelHit(level, rayTraceResult, shooter, spellContext);
        if (hit == null) {
            return;
        }
        ServerSubLevel subLevel = hit.subLevel();
        Vector3dc position = subLevel.logicalPose().position();
        ItemStack miniature = MiniatureSublevelHelper.miniaturize(level, subLevel, getPlayer(shooter, level));
        if (miniature.isEmpty()) {
            return;
        }
        if (isRealPlayer(shooter) && shooter instanceof Player player) {
            if (!player.addItem(miniature)) {
                player.drop(miniature, false);
            }
            return;
        }
        level.addFreshEntity(new ItemEntity(level, position.x(), position.y(), position.z(), miniature));
    }

    @Override
    public int getDefaultManaCost() {
        return 100;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Converts a targeted sublevel into an item that can be placed in the world.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }

    @Override
    protected @NotNull Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf();
    }
}
