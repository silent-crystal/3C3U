/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.mixin.ClientWorldAccessor;
import meteordevelopment.meteorclient.mixin.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.enchantment.EnchantmentHelper.getEquipmentLevel;

public class SpeedMine extends Module {

    /*
        周家梅你写的一坨
    */

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCheck = settings.createGroup("Check");

    // General
    public final Setting<List<Block>> godBlocks = sgGeneral.add(new BlockListSetting.Builder().name("blocks").description("不可破坏的方块").defaultValue(Blocks.COMMAND_BLOCK, Blocks.LAVA_CAULDRON, Blocks.LAVA, Blocks.WATER_CAULDRON, Blocks.WATER, Blocks.BEDROCK, Blocks.BARRIER, Blocks.END_PORTAL, Blocks.NETHER_PORTAL, Blocks.END_PORTAL_FRAME).onChanged(blocks1 -> {if (isActive() && Utils.canUpdate()) onActivate();}).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(50).min(0).sliderMax(500)
        .description("晓骇没汉化").build());
    private final Setting<Double> damage = sgGeneral.add(new DoubleSetting.Builder().name("damage").defaultValue(0.7).min(0.0).sliderMax(2.0)
        .description("晓骇没汉化").build());
    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(6.0).min(3.0).sliderMax(10.0)
        .description("晓骇没汉化").build());
    private final Setting<Integer> maxBreak = sgGeneral.add(new IntSetting.Builder().name("max-break").defaultValue(50).min(0).sliderMax(500)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> grim = sgGeneral.add(new BoolSetting.Builder().name("grim").defaultValue(false)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder().name("instant").defaultValue(false)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> wait = sgGeneral.add(new BoolSetting.Builder().name("wait").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> mineAir = sgGeneral.add(new BoolSetting.Builder().name("mine-air").defaultValue(true)
        .description("晓骇没汉化").build());
    public final Setting<Boolean> hotBar = sgGeneral.add(new BoolSetting.Builder().name("hotbar-swap").defaultValue(false)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> setAir = sgGeneral.add(new BoolSetting.Builder().name("set-air").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> endSwing = sgGeneral.add(new BoolSetting.Builder().name("end-swing").defaultValue(true)
        .description("晓骇没汉化").build());

    // Check
    private final Setting<Boolean> switchReset = sgCheck.add(new BoolSetting.Builder().name("switch-reset").defaultValue(false)
        .description("晓骇没汉化").build());
    public final Setting<Boolean> farCancel = sgCheck.add(new BoolSetting.Builder().name("far-cancel").defaultValue(false)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> onlyGround = sgCheck.add(new BoolSetting.Builder().name("only-ground").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> checkGround = sgCheck.add(new BoolSetting.Builder().name("check-ground").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> smart = sgCheck.add(new BoolSetting.Builder().name("smart").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> usingPause = sgCheck.add(new BoolSetting.Builder().name("using-pause").defaultValue(false)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> bypassGround = sgCheck.add(new BoolSetting.Builder().name("bypass-ground").defaultValue(true)
        .description("晓骇没汉化").build());
    private final Setting<Integer> bypassTime = sgCheck.add(new IntSetting.Builder().name("bypass-time").defaultValue(400).min(0).sliderMax(2000).visible(() -> bypassGround.get())
        .description("晓骇没汉化").build());


    public CopyOnWriteArrayList<PlayerEntity> inWebPlayers = new CopyOnWriteArrayList<>();

    int lastSlot = -1;
    public static SpeedMine INSTANCE;
    public static BlockPos breakPos;
    public static double progress = 0;
    private final meteordevelopment.meteorclient.utils.player.Timer mineTimer = new meteordevelopment.meteorclient.utils.player.Timer();
    private boolean startPacket = false;
    private int breakNumber = 0;
    private final meteordevelopment.meteorclient.utils.player.Timer delayTimer = new meteordevelopment.meteorclient.utils.player.Timer();
    public static boolean sendGroundPacket = false;

    static DecimalFormat df = new DecimalFormat("0.0");

    public SpeedMine() {
        super(Categories.Player, "speed-mine", "包挖 (Alien V1)");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        startPacket = false;
        breakPos = null;
        if(!Modules.get().get(AutoTool.class).isActive()) warning("建议配合AutoTool使用");
    }

    @Override
    public void onDeactivate() {
        startPacket = false;
        breakPos = null;
    }

    boolean done = false;
    boolean skip = false;

    int prevSlot = 0;

    @EventHandler
    private void onUpdate(TickEvent.Post event){

        if (skip) {
            skip = false;
            return;
        }

        if (mc.player.isCreative()) {
            startPacket = false;
            breakNumber = 0;
            breakPos = null;
            return;
        }

        if (breakPos == null) {
            breakNumber = 0;
            startPacket = false;
            return;
        }

        if (isAir(breakPos)) breakNumber = 0;

        if (breakNumber > maxBreak.get() - 1 && maxBreak.get() > 0 || !wait.get() && isAir(breakPos) && !instant.get()) {
            startPacket = false;
            breakNumber = 0;
            breakPos = null;
            return;
        }

        if (godBlocks.get().contains(mc.world.getBlockState(breakPos).getBlock())) {
            breakPos = null;
            startPacket = false;
            return;
        }

        if (usingPause.get() && mc.player.isUsingItem()) return;

        if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(breakPos.toCenterPos())) > range.get()) {
            if (farCancel.get()) {
                startPacket = false;
                breakNumber = 0;
                breakPos = null;
            }
            return;
        }

        if (!hotBar.get() && mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof ModulesScreen)) return;

        int slot = getTool(breakPos);
        if (slot == -1) slot = mc.player.getInventory().selectedSlot;

        if (!delayTimer.passedMs((long) delay.get())) return;

        if(!mineAir.get() && isAir(breakPos) && godBlocks.get().contains(mc.world.getBlockState(breakPos).getBlock())) { startPacket = true; }

        if (startPacket) {
            if (isAir(breakPos)) return;
            if (onlyGround.get() && !mc.player.isOnGround()) return;
            done = mineTimer.passedMs((long) getBreakTime(breakPos, slot));
            if (done) {
                if (hotBar.get()) switchToSlot(prevSlot);
                if (endSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakPos, BlockUtils.getPlaceSide(breakPos), id));
                breakNumber++;
                delayTimer.reset();
                if (setAir.get()) mc.world.setBlockState(breakPos, Blocks.AIR.getDefaultState());
                skip = true;
            }
            else{
                prevSlot = mc.player.getInventory().selectedSlot;
                if (hotBar.get()) switchToSlot(slot);
            }
        } else {
            if (!mineAir.get() && isAir(breakPos)) return;
            Direction side = BlockUtils.getPlaceSide(breakPos);
            mineTimer.reset();
            done = false;
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, breakPos, side, id));
            if (grim.get()) {
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakPos, side, id));
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, breakPos, side, id));
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, breakPos, side, id));
            }
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacketSent(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || mc.player.isCreative()) return;
        if (event.packet instanceof PlayerMoveC2SPacket) {
            if (bypassGround.get() && !mc.player.isFallFlying() && breakPos != null && !isAir(breakPos) && bypassTime.get() > 0 && MathHelper.sqrt((float) breakPos.toCenterPos().squaredDistanceTo(mc.player.getEyePos())) <= range.get() + 2) {
                int slot = getTool(breakPos);
                if (slot == -1) {
                    slot = mc.player.getInventory().selectedSlot;
                }
                double breakTime = (getBreakTime(breakPos, slot) - bypassTime.get());
                if (breakTime <= 0 || mineTimer.passedMs((long) breakTime)) {
                    sendGroundPacket = true;
                    ((IPlayerMoveC2SPacket) event.packet).setOnGround(true);
                }
            } else sendGroundPacket = false;
            return;
        }
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            if (packet.getSelectedSlot() != lastSlot) {
                lastSlot = packet.getSelectedSlot();
                if (switchReset.get()) {
                    startPacket = false;
                    mineTimer.reset();
                    done = false;
                }
            }
            return;
        }
        if (!(event.packet instanceof PlayerActionC2SPacket)) return;
        if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            if (breakPos == null || !((PlayerActionC2SPacket) event.packet).getPos().equals(breakPos)) return;
            startPacket = true;
        } else if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            if (breakPos == null || !((PlayerActionC2SPacket) event.packet).getPos().equals(breakPos)) return;
            if (!instant.get()) startPacket = false;
        }
    }


    @EventHandler
    public void onStartBreakingBlock(StartBreakingBlockEvent event){
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isCreative()) return;
        event.cancel();

        BlockPos pos = event.blockPos;
        if (pos.equals(breakPos)) return;
        if (godBlocks.get().contains(mc.world.getBlockState(pos).getBlock())) return;
        if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > range.get()) return;
        breakPos = pos;
        breakNumber = 0;
        startPacket = false;
        mineTimer.reset();
        done = false;
        skip = true;
        mineTimer.reset();
        done = false;
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (MeteorClient.mc.getNetworkHandler() == null || MeteorClient.mc.world == null) return;

        ClientWorld world = MeteorClient.mc.world;
        ClientWorldAccessor accessor = (ClientWorldAccessor) world;

        try (PendingUpdateManager pendingUpdateManager = accessor.getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            MeteorClient.mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }

    public final double getBreakTime(BlockPos pos, int slot) { return getBreakTime(pos, slot, damage.get()); }


    public final double getBreakTime(BlockPos pos, int slot, double damage) { return (1 / getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20 * 1000 * damage); }

    public float getBlockStrength(BlockPos position, ItemStack itemStack) {
        BlockState state = mc.world.getBlockState(position);
        float hardness = state.getHardness(mc.world, position);
        if (hardness < 0) return 0;
        return getDigSpeed(state, itemStack) / hardness / 30F;
    }

    public float getDigSpeed(BlockState state, ItemStack itemStack) {
        float digSpeed = getDestroySpeed(state, itemStack);
        if (digSpeed > 1) {
            int efficiencyModifier = EnchantmentHelper.getLevel(getEntry(Enchantments.EFFICIENCY),itemStack);
            if (efficiencyModifier > 0 && !itemStack.isEmpty()) {
                digSpeed += (float) (StrictMath.pow(efficiencyModifier, 2) + 1);
            }
        }
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) digSpeed *= 1 + (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float fatigueScale;
            switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> fatigueScale = 0.3F;
                case 1 -> fatigueScale = 0.09F;
                case 2 -> fatigueScale = 0.0027F;
                default -> fatigueScale = 8.1E-4F;
            }
            digSpeed *= fatigueScale;
        }
        if (mc.player.isSubmergedInWater() && !(getEquipmentLevel(getEntry(Enchantments.AQUA_AFFINITY),mc.player) > 0)) digSpeed /= 5;
        boolean inWeb = inWebPlayers.contains(mc.player) && mc.world.getBlockState(breakPos).getBlock() == Blocks.COBWEB;
        if ((!mc.player.isOnGround() || inWeb) && INSTANCE.checkGround.get() && (!smart.get() || mc.player.isFallFlying() || inWeb)) digSpeed /= 5;
        return (digSpeed < 0 ? 0 : digSpeed);
    }

    public float getDestroySpeed(BlockState state, ItemStack itemStack) {
        float destroySpeed = 1;
        if (itemStack != null && !itemStack.isEmpty()) destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
        return destroySpeed;
    }

    private boolean isAir(BlockPos breakPos) { return mc.world.isAir(breakPos) || BlockUtils.getBlock(breakPos) == Blocks.FIRE && BlockUtils.hasCrystal(breakPos); }

    public static RegistryEntry<Enchantment> getEntry(RegistryKey<Enchantment> ench){ return MeteorClient.mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(ench).get(); }

    private int getTool(BlockPos pos) {
        if (hotBar.get()) {
            int index = -1;
            float CurrentFastest = 1.0f;
            for (int i = 0; i < 9; ++i) {
                final ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != ItemStack.EMPTY) {
                    final float digSpeed = EnchantmentHelper.getLevel(getEntry(Enchantments.EFFICIENCY), stack);
                    final float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
                    if (digSpeed + destroySpeed > CurrentFastest) {
                        CurrentFastest = digSpeed + destroySpeed;
                        index = i;
                    }
                }
            }
            return index;
        } else {
            AtomicInteger slot = new AtomicInteger();
            slot.set(-1);
            float CurrentFastest = 1.0f;
            for (Map.Entry<Integer, ItemStack> entry : InvUtils.getInventoryAndHotbarSlots().entrySet()) {
                if (!(entry.getValue().getItem() instanceof AirBlockItem)) {
                    final float digSpeed = EnchantmentHelper.getLevel(getEntry(Enchantments.EFFICIENCY), entry.getValue());
                    final float destroySpeed = entry.getValue().getMiningSpeedMultiplier(mc.world.getBlockState(pos));
                    if (digSpeed + destroySpeed > CurrentFastest) {
                        CurrentFastest = digSpeed + destroySpeed;
                        slot.set(entry.getKey());
                    }
                }
            }
            return slot.get();
        }
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (breakPos != null) {
            Box pos = new Box(breakPos);
            pos = pos.stretch(0, 0, 0);
            event.renderer.box(pos, new Color(255, 0, 0, 25), new Color(255, 0, 0, 50), ShapeMode.Both, 0);
        }
    }

    public static void switchToSlot(int slot) {
        MeteorClient.mc.player.getInventory().selectedSlot = slot;
        MeteorClient.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}

