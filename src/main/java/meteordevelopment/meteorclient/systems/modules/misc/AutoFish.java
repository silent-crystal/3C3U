/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;

public class AutoFish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSplashRangeDetection = settings.createGroup("Splash Detection");

    // General

    private final Setting<Boolean> autoCast = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-cast")
        .description("不钓鱼时自动抛竿")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> ticksAutoCast = sgGeneral.add(new IntSetting.Builder()
        .name("ticks-auto-cast")
        .description("等待时间")
        .defaultValue(10)
        .min(0)
        .sliderMax(60)
        .build()
    );

    private final Setting<Integer> ticksCatch = sgGeneral.add(new IntSetting.Builder()
        .name("catch-delay")
        .description("钓鱼前等待时间")
        .defaultValue(6)
        .min(0)
        .sliderMax(60)
        .build()
    );

    private final Setting<Integer> ticksThrow = sgGeneral.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("投掷鱼饵前等待时间")
        .defaultValue(14)
        .min(0)
        .sliderMax(60)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("防止鱼竿损坏")
        .defaultValue(false)
        .build()
    );

    // Splash Detection

    private final Setting<Boolean> splashDetectionRangeEnabled = sgSplashRangeDetection.add(new BoolSetting.Builder()
        .name("splash-detection-range-enabled")
        .description("允许您同时使用多个账户")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> splashDetectionRange = sgSplashRangeDetection.add(new DoubleSetting.Builder()
        .name("splash-detection-range")
        .description("不会翻译")
        .defaultValue(10)
        .min(0)
        .build()
    );

    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    private int autoCastTimer;
    private boolean autoCastEnabled;

    private int autoCastCheckTimer;

    public AutoFish() {
        super(Categories.Misc, "auto-fish", "自动钓鱼");
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        autoCastEnabled = false;
        autoCastCheckTimer = 0;
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        SoundInstance p = event.sound;
        FishingBobberEntity b = mc.player.fishHook;

        if (p.getId().getPath().equals("entity.fishing_bobber.splash")) {
            if (!splashDetectionRangeEnabled.get() || Utils.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ()) <= splashDetectionRange.get()) {
                ticksEnabled = true;
                ticksToRightClick = ticksCatch.get();
                ticksData = 0;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Auto cast
        if (autoCastCheckTimer <= 0) {
            autoCastCheckTimer = 30;

            if (autoCast.get() && !ticksEnabled && !autoCastEnabled && mc.player.fishHook == null && hasFishingRod()) {
                autoCastTimer = 0;
                autoCastEnabled = true;
            }
        } else {
            autoCastCheckTimer--;
        }

        // Check for auto cast timer
        if (autoCastEnabled) {
            autoCastTimer++;

            if (autoCastTimer > ticksAutoCast.get()) {
                autoCastEnabled = false;
                Utils.rightClick();
            }
        }

        // Handle logic
        if (ticksEnabled && ticksToRightClick <= 0) {
            if (ticksData == 0) {
                Utils.rightClick();
                ticksToRightClick = ticksThrow.get();
                ticksData = 1;
            }
            else if (ticksData == 1) {
                Utils.rightClick();
                ticksEnabled = false;
            }
        }

        ticksToRightClick--;
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.options.useKey.isPressed()) ticksEnabled = false;
    }

    private boolean hasFishingRod() {
        return InvUtils.swap(InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.FISHING_ROD && (!antiBreak.get() || itemStack.getDamage() < itemStack.getMaxDamage() - 1)).slot(), false);
    }
}
