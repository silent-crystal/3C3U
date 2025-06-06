/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerTickMovementEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixin.CreativeInventoryScreenAccessor;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.math.MathHelper;

import static org.lwjgl.glfw.GLFW.*;

public class GUIMove extends Module {
    public enum Screens {
        GUI,
        Inventory,
        Both
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Screens> screens = sgGeneral.add(new EnumSetting.Builder<Screens>()
        .name("guis")
        .description("打开GUI时移动")
        .defaultValue(Screens.Inventory)
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("打开GUI时跳跃")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) set(mc.options.jumpKey, false);
        })
        .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("打开GUI时潜行")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) set(mc.options.sneakKey, false);
        })
        .build()
    );

    public final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("打开GUI时疾跑")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) set(mc.options.sprintKey, false);
        })
        .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("arrows-rotate")
        .description("打开GUI时转动视角")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate-speed")
        .description("旋转速度")
        .defaultValue(4)
        .min(0)
        .build()
    );

    public GUIMove() {
        super(Categories.Movement, "gui-move", "更好的GUI");
    }

    @Override
    public void onDeactivate() {
        set(mc.options.forwardKey, false);
        set(mc.options.backKey, false);
        set(mc.options.leftKey, false);
        set(mc.options.rightKey, false);

        if (jump.get()) set(mc.options.jumpKey, false);
        if (sneak.get()) set(mc.options.sneakKey, false);
        if (sprint.get()) set(mc.options.sprintKey, false);
    }

    public boolean disableSpace() {
        return isActive() && jump.get() && mc.options.jumpKey.isDefault();
    }
    public boolean disableArrows() {
        return isActive() && arrowsRotate.get();
    }

    @EventHandler
    private void onPlayerMoveEvent(PlayerTickMovementEvent event) {
        if (skip()) return;
        if (screens.get() == Screens.GUI && !(mc.currentScreen instanceof WidgetScreen)) return;
        if (screens.get() == Screens.Inventory && mc.currentScreen instanceof WidgetScreen) return;

        set(mc.options.forwardKey, Input.isPressed(mc.options.forwardKey));
        set(mc.options.backKey, Input.isPressed(mc.options.backKey));
        set(mc.options.leftKey, Input.isPressed(mc.options.leftKey));
        set(mc.options.rightKey, Input.isPressed(mc.options.rightKey));

        if (jump.get()) set(mc.options.jumpKey, Input.isPressed(mc.options.jumpKey));
        if (sneak.get()) set(mc.options.sneakKey, Input.isPressed(mc.options.sneakKey));
        if (sprint.get()) set(mc.options.sprintKey, Input.isPressed(mc.options.sprintKey));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (skip()) return;
        if (screens.get() == Screens.GUI && !(mc.currentScreen instanceof WidgetScreen)) return;
        if (screens.get() == Screens.Inventory && mc.currentScreen instanceof WidgetScreen) return;

        float rotationDelta = Math.min((float) (rotateSpeed.get() * event.frameTime * 20f), 100);

        if (arrowsRotate.get()) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            if (Input.isKeyPressed(GLFW_KEY_LEFT)) yaw -= rotationDelta;
            if (Input.isKeyPressed(GLFW_KEY_RIGHT)) yaw += rotationDelta;
            if (Input.isKeyPressed(GLFW_KEY_UP)) pitch -= rotationDelta;
            if (Input.isKeyPressed(GLFW_KEY_DOWN)) pitch += rotationDelta;


            pitch = MathHelper.clamp(pitch, -90, 90);

            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private void set(KeyBinding bind, boolean pressed) {
        boolean wasPressed = bind.isPressed();
        bind.setPressed(pressed);

        InputUtil.Key key = ((KeyBindingAccessor) bind).getKey();
        if (wasPressed != pressed && key.getCategory() == InputUtil.Type.KEYSYM) {
            MeteorClient.EVENT_BUS.post(KeyEvent.get(key.getCode(), 0, pressed ? KeyAction.Press : KeyAction.Release));
        }
    }

    public boolean skip() {
        return mc.currentScreen == null || (mc.currentScreen instanceof CreativeInventoryScreen && CreativeInventoryScreenAccessor.getSelectedTab() == ItemGroups.getSearchGroup()) || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof AnvilScreen || mc.currentScreen instanceof AbstractCommandBlockScreen || mc.currentScreen instanceof StructureBlockScreen;
    }
}
