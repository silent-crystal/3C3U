/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.*;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.misc.HighwayBuilder;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isTouchingWater", at = @At(value = "HEAD"), cancellable = true)
    private void isTouchingWater(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(Flight.class).isActive()) info.setReturnValue(false);
    }

    @Inject(method = "isInLava", at = @At(value = "HEAD"), cancellable = true)
    private void isInLava(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(Flight.class).isActive()) info.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSubmergedInWater()Z"))
    private boolean isSubmergedInWater(boolean submerged) {
        if ((Object) this != mc.player) return submerged;
        if (Modules.get().get(Flight.class).isActive()) return false;
        return submerged;
    }

    @ModifyArgs(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    private void onPushAwayFrom(Args args, Entity entity) {
        // FakePlayerEntity
        if (entity instanceof FakePlayerEntity player && player.doNotPush) {
            args.set(0, 0.0);
            args.set(2, 0.0);
        }
    }

    @ModifyReturnValue(method = "getJumpVelocityMultiplier", at = @At("RETURN"))
    private float onGetJumpVelocityMultiplier(float original) {
        if ((Object) this == mc.player) {
            JumpVelocityMultiplierEvent event = MeteorClient.EVENT_BUS.post(JumpVelocityMultiplierEvent.get());
            return (original * event.multiplier);
        }

        return original;
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this == mc.player) {
            MeteorClient.EVENT_BUS.post(PlayerMoveEvent.get(type, movement));
        }
        else if ((Object) this instanceof LivingEntity) {
            MeteorClient.EVENT_BUS.post(LivingEntityMoveEvent.get((LivingEntity) (Object) this, movement));
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> info) {
        if (PostProcessShaders.rendering) {
            Color color = Modules.get().get(ESP.class).getColor((Entity) (Object) this);
            if (color != null) info.setReturnValue(color.getPacked());
        }
    }

    @Redirect(method = "getVelocityMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"))
    private Block getVelocityMultiplierGetBlockProxy(BlockState blockState) {
        if ((Object) this != mc.player) return blockState.getBlock();
        return blockState.getBlock();
    }

    @ModifyReturnValue(method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("RETURN"))
    private boolean isInvisibleToCanceller(boolean original) {
        if (!Utils.canUpdate()) return original;
        ESP esp = Modules.get().get(ESP.class);
        if (Modules.get().get(NoRender.class).noInvisibility() || esp.isActive() && !esp.shouldSkip((Entity) (Object) this)) return false;
        return original;
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void isGlowing(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(NoRender.class).noGlowing()) info.setReturnValue(false);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if (player == null) info.setReturnValue(false);
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void getPoseHook(CallbackInfoReturnable<EntityPose> info) {
        if ((Object) this != mc.player) return;

        if (Modules.get().get(ElytraFly.class).canPacketEfly()) {
            info.setReturnValue(EntityPose.FALL_FLYING);
        }
    }

    @ModifyReturnValue(method = "getPose", at = @At("RETURN"))
    private EntityPose modifyGetPose(EntityPose original) {
        if ((Object) this != mc.player) return original;

        if (original == EntityPose.CROUCHING && !mc.player.isSneaking()) return EntityPose.STANDING;
        return original;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void updateChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        Freecam freecam = Modules.get().get(Freecam.class);
        FreeLook freeLook = Modules.get().get(FreeLook.class);

        if (freecam.isActive()) {
            freecam.changeLookDirection(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
        }
        else if (Modules.get().isActive(HighwayBuilder.class)) {
            Camera camera = mc.gameRenderer.getCamera();
            ((ICamera) camera).setRot(camera.getYaw() + cursorDeltaX * 0.15, camera.getPitch() + cursorDeltaY * 0.15);
            ci.cancel();
        }
        else if (freeLook.cameraMode()) {
            freeLook.cameraYaw += (float) (cursorDeltaX / freeLook.sensitivity.get().floatValue());
            freeLook.cameraPitch += (float) (cursorDeltaY / freeLook.sensitivity.get().floatValue());

            if (Math.abs(freeLook.cameraPitch) > 90.0F) freeLook.cameraPitch = freeLook.cameraPitch > 0.0F ? 90.0F : -90.0F;
            ci.cancel();
        }
    }
}
