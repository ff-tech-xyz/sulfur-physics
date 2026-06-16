package xyz.fftech.sulfurphysics.mixin;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.fftech.sulfurphysics.SulfurPhysics;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Vec3 center;
    @Shadow @Final private float radius;

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void sulfurPhysics$replaceOuterBlocksWithCubes(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> affectedBlocks = cir.getReturnValue();
        SulfurPhysics.replaceOuterExplosionBlocks(this.level, this.center, this.radius, affectedBlocks);
        cir.setReturnValue(affectedBlocks);
    }
}
