package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.common.helper.SpellProjectileHelpers;
import com.hollingsworth.arsnouveau.api.spell.ITurretBehavior;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.block.tile.RotatingTurretTile;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record SublevelTurretProjectileBehavior(ITurretBehavior original) implements ITurretBehavior {

    @Override
    public void onCast(SpellResolver resolver, ServerLevel serverLevel, BlockPos pos, Player fakePlayer, Position dispensePosition, Direction direction) {
        SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, pos);
        if (subLevel == null) {
            original.onCast(resolver, serverLevel, pos, fakePlayer, dispensePosition, direction);
            return;
        }
        Vec3 aim = serverLevel.getBlockEntity(pos) instanceof RotatingTurretTile tile ? tile.getShootAngle() : Vec3.atLowerCornerOf(direction.getNormal());
        EntityProjectileSpell spell = new EntityProjectileSpell(serverLevel, resolver);
        spell.setOwner(fakePlayer);
        spell.setPos(dispensePosition.x(), dispensePosition.y() - 0.25, dispensePosition.z());
        SpellProjectileHelpers.launchFromSublevel(spell, subLevel, aim.normalize().scale(Math.max(0.1f, 0.75f + resolver.getCastStats().getAccMultiplier() / 2)));
        // Fixes timer spell turrets as those get spawned at a different point in the tick loop and collide with sublevels
        if (!serverLevel.isHandlingTick()) {
            spell.setPos(spell.position().add(Sable.HELPER.getVelocity(serverLevel, subLevel, dispensePosition).scale(1 / 20.0)));
        }
        serverLevel.addFreshEntity(spell);
    }
}
