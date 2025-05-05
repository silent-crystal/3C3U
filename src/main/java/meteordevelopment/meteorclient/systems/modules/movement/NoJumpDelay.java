/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LivingEntityAccessor;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super(Categories.Movement, "no-jump-delay", "顶头跳");
    }
    @EventHandler
    private void onUpdate(TickEvent.Post e){
        LivingEntityAccessor accessor = (LivingEntityAccessor) mc.player;
        accessor.setJumpCooldown(0);
    }
}
