/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.StatusEffectInstanceAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

/**
 * @author Walaryne
 */
public class Ambience extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSky = settings.createGroup("Sky");
    private final SettingGroup sgWorld = settings.createGroup("World");

    public final Setting<Ambience.Mode> mode = sgGeneral.add(new EnumSetting.Builder<Ambience.Mode>()
        .name("mode")
        .description("The mode to use for Fullbright.")
        .defaultValue(Ambience.Mode.Gamma)
        .onChanged(mode -> {
            if (isActive()) {
                if (mode != Ambience.Mode.Potion) disableNightVision();
                if (mc.worldRenderer != null) mc.worldRenderer.reload();
            }
        })
        .build()
    );

    public final Setting<LightType> lightType = sgGeneral.add(new EnumSetting.Builder<LightType>()
        .name("light-type")
        .description("Which type of light to use for Luminance mode.")
        .defaultValue(LightType.BLOCK)
        .visible(() -> mode.get() == Ambience.Mode.Luminance)
        .onChanged(integer -> {
            if (mc.worldRenderer != null && isActive()) mc.worldRenderer.reload();
        })
        .build()
    );

    private final Setting<Integer> minimumLightLevel = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-light-level")
        .description("Minimum light level when using Luminance mode.")
        .visible(() -> mode.get() == Ambience.Mode.Luminance)
        .defaultValue(8)
        .range(0, 15)
        .sliderMax(15)
        .onChanged(integer -> {
            if (mc.worldRenderer != null && isActive()) mc.worldRenderer.reload();
        })
        .build()
    );

    // Sky

    public final Setting<Boolean> endSky = sgSky.add(new BoolSetting.Builder()
        .name("end-sky")
        .description("Makes the sky like the end.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> customSkyColor = sgSky.add(new BoolSetting.Builder()
        .name("custom-sky-color")
        .description("Whether the sky color should be changed.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> overworldSkyColor = sgSky.add(new ColorSetting.Builder()
        .name("overworld-sky-color")
        .description("The color of the overworld sky.")
        .defaultValue(new SettingColor(0, 125, 255))
        .visible(customSkyColor::get)
        .build()
    );

    public final Setting<SettingColor> netherSkyColor = sgSky.add(new ColorSetting.Builder()
        .name("nether-sky-color")
        .description("The color of the nether sky.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customSkyColor::get)
        .build()
    );

    public final Setting<SettingColor> endSkyColor = sgSky.add(new ColorSetting.Builder()
        .name("end-sky-color")
        .description("The color of the end sky.")
        .defaultValue(new SettingColor(65, 30, 90))
        .visible(customSkyColor::get)
        .build()
    );

    public final Setting<Boolean> customCloudColor = sgSky.add(new BoolSetting.Builder()
        .name("custom-cloud-color")
        .description("Whether the clouds color should be changed.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> cloudColor = sgSky.add(new ColorSetting.Builder()
        .name("cloud-color")
        .description("The color of the clouds.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customCloudColor::get)
        .build()
    );

    public final Setting<Boolean> changeLightningColor = sgSky.add(new BoolSetting.Builder()
        .name("custom-lightning-color")
        .description("Whether the lightning color should be changed.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> lightningColor = sgSky.add(new ColorSetting.Builder()
        .name("lightning-color")
        .description("The color of the lightning.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(changeLightningColor::get)
        .build()
    );

    // World
    public final Setting<Boolean> customGrassColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-grass-color")
        .description("Whether the grass color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<SettingColor> grassColor = sgWorld.add(new ColorSetting.Builder()
        .name("grass-color")
        .description("The color of the grass.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customGrassColor::get)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<Boolean> customFoliageColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-foliage-color")
        .description("Whether the foliage color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<SettingColor> foliageColor = sgWorld.add(new ColorSetting.Builder()
        .name("foliage-color")
        .description("The color of the foliage.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customFoliageColor::get)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<Boolean> customWaterColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-water-color")
        .description("Whether the water color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<SettingColor> waterColor = sgWorld.add(new ColorSetting.Builder()
        .name("water-color")
        .description("The color of the water.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customWaterColor::get)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<Boolean> customLavaColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-lava-color")
        .description("Whether the lava color should be changed.")
        .defaultValue(false)
        .onChanged(val -> reload())
        .build()
    );

    public final Setting<SettingColor> lavaColor = sgWorld.add(new ColorSetting.Builder()
        .name("lava-color")
        .description("The color of the lava.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customLavaColor::get)
        .onChanged(val -> reload())
        .build()
    );

    public Ambience() {
        super(Categories.Render, "ambience", "Change the color of various pieces of the environment.");
    }

    @Override
    public void onActivate() {
        reload();
        if (mode.get() == Ambience.Mode.Luminance) mc.worldRenderer.reload();
        if (mode.get() == Ambience.Mode.Luminance) mc.worldRenderer.reload();
        else if (mode.get() == Ambience.Mode.Potion) disableNightVision();
    }

    @Override
    public void onDeactivate() {
        reload();
    }

    private void reload() {
        if (mc.worldRenderer != null && isActive()) mc.worldRenderer.reload();
    }

    public static class Custom extends DimensionEffects {
        public Custom() {
            super(Float.NaN, true, DimensionEffects.SkyType.END, true, false);
        }

        @Override
        public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
            return color.multiply(0.15000000596046448D);
        }

        @Override
        public boolean useThickFog(int camX, int camY) {
            return false;
        }

        @Override
        public float[] getFogColorOverride(float skyAngle, float tickDelta) {
            return null;
        }
    }

    public SettingColor skyColor() {
        switch (PlayerUtils.getDimension()) {
            case Overworld -> {
                return overworldSkyColor.get();
            }
            case Nether -> {
                return netherSkyColor.get();
            }
            case End -> {
                return endSkyColor.get();
            }
        }

        return null;
    }


    public int getLuminance(LightType type) {
        if (!isActive() || mode.get() != Ambience.Mode.Luminance || type != lightType.get()) return 0;
        return minimumLightLevel.get();
    }

    public boolean getGamma() {
        return isActive() && mode.get() == Ambience.Mode.Gamma;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || !mode.get().equals(Ambience.Mode.Potion)) return;
        if (mc.player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()))) {
            StatusEffectInstance instance = mc.player.getStatusEffect(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()));
            if (instance != null && instance.getDuration() < 420) ((StatusEffectInstanceAccessor) instance).setDuration(420);
        } else {
            mc.player.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()), 420, 0));
        }
    }

    private void disableNightVision() {
        if (mc.player == null) return;
        if (mc.player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()))) {
            mc.player.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(StatusEffects.NIGHT_VISION.value()));
        }
    }

    public enum Mode {
        Gamma,
        Potion,
        Luminance
    }
}
