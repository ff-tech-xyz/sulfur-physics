package xyz.fftech.sulfurphysics.mixin;

import java.util.function.BooleanSupplier;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.fftech.sulfurphysics.SulfurPhysics;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void sulfurPhysics$tickMovingBlocks(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SulfurPhysics.tickMovingBlocks((ServerLevel) (Object) this);
    }
}
