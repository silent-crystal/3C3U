/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Aura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTiming = settings.createGroup("Timing");

    // General

    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
        .name("weapon")
        .description("武器")
        .defaultValue(Weapon.All)
        .build()
    );

    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotate")
        .description("转头条件（敌人在范围时、挥刀时、无转头）")
        .defaultValue(RotationMode.Always)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("自动切换武器")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("在敌人进入范围时自动暂停AI（敌人不在范围后将继续）")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShieldMode> shieldMode = sgGeneral.add(new EnumSetting.Builder<ShieldMode>()
        .name("shield-mode")
        .description("自动破盾")
        .defaultValue(ShieldMode.Break)
        .visible(() -> autoSwitch.get() && weapon.get() != Weapon.Axe)
        .build()
    );

    // Targeting

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("猎杀名单")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("范围内多个敌人的攻击顺序")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("range")
        .description("攻击范围")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> wallsRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("当敌人隔墙时的攻击范围")
        .defaultValue(3.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<EntityAge> mobAgeFilter = sgTargeting.add(new EnumSetting.Builder<EntityAge>()
        .name("mob-age-filter")
        .description("猎杀体型")
        .defaultValue(EntityAge.Adult)
        .build()
    );

    private final Setting<Boolean> ignoreNamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-named")
        .description("被命名生物不计入攻击范围")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignorePassive = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-passive")
        .description("攻击被惹怒的中立生物")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-tamed")
        .description("被驯服生物不攻击")
        .defaultValue(false)
        .build()
    );

    // Timing

    private final Setting<Boolean> pauseOnLag = sgTiming.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("卡顿时暂停")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnUse = sgTiming.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("使用物品时不会攻击")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> tpsSync = sgTiming.add(new BoolSetting.Builder()
        .name("TPS-sync")
        .description("同步服务器TPS")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> switchDelay = sgTiming.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("武器切换时间")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final List<Entity> targets = new ArrayList<>();
    private int switchTimer;
    private boolean wasPathing = false;
    public boolean attacking;

    public Aura() {
        super(Categories.Player, "aura", "杀戮");
    }

    @Override
    public void onDeactivate() {
        targets.clear();
        attacking = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
        if (pauseOnUse.get() && (mc.interactionManager.isBreakingBlock() || mc.player.isUsingItem())) return;
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && pauseOnLag.get()) return;

        targets.clear();
        TargetUtils.getList(targets, this::entityCheck, priority.get(), 1);

        if (targets.isEmpty()) {
            attacking = false;
            if (wasPathing) {
                PathManagers.get().resume();
                wasPathing = false;
            }
            return;
        }

        Entity primary = targets.getFirst();

        if (autoSwitch.get()) {
            Predicate<ItemStack> predicate = switch (weapon.get()) {
                case Axe -> stack -> stack.getItem() instanceof AxeItem;
                case Sword -> stack -> stack.getItem() instanceof SwordItem;
                case Mace -> stack -> stack.getItem() instanceof MaceItem;
                case All -> stack -> stack.getItem() instanceof AxeItem || stack.getItem() instanceof SwordItem || stack.getItem() instanceof MaceItem;
                default -> o -> true;
            };
            FindItemResult weaponResult = InvUtils.findInHotbar(predicate);

            if (shouldShieldBreak()) {
                FindItemResult axeResult = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof AxeItem);
                if (axeResult.found()) weaponResult = axeResult;
            }

            InvUtils.swap(weaponResult.slot(), false);
        }

        if (!itemInHand()) return;

        attacking = true;
        if (rotation.get() == RotationMode.Always){
            Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
            MeteorClient.mc.player.setYaw((float) Rotations.getYaw(primary));
            MeteorClient.mc.player.setPitch((float) Rotations.getPitch(primary, Target.Body));
        }
        if (pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
            PathManagers.get().pause();
            wasPathing = true;
        }

        if (delayCheck()) targets.forEach(this::attack);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    private boolean shouldShieldBreak() {
        for (Entity target : targets) {
            if (target instanceof PlayerEntity player) {
                if (player.blockedByShield(mc.world.getDamageSources().playerAttack(mc.player)) && shieldMode.get() == ShieldMode.Break) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDead()) || !entity.isAlive()) return false;

        Box hitbox = entity.getBoundingBox();
        if (!PlayerUtils.isWithin(
            MathHelper.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
            MathHelper.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
            MathHelper.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
            range.get()
        )) return false;

        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;
        if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, wallsRange.get())) return false;
        if (ignoreTamed.get()) {
            if (entity instanceof Tameable tameable
                && tameable.getOwnerUuid() != null
                && tameable.getOwnerUuid().equals(mc.player.getUuid())
            ) return false;
        }
        if (ignorePassive.get()) {
            if (entity instanceof EndermanEntity enderman && !enderman.isAngry()) return false;
            if (entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) return false;
            if (entity instanceof WolfEntity wolf && !wolf.isAttacking()) return false;
        }
        if (entity instanceof PlayerEntity player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
            if (shieldMode.get() == ShieldMode.Ignore && player.blockedByShield(mc.world.getDamageSources().playerAttack(mc.player))) return false;
        }
        if (entity instanceof AnimalEntity animal) {
            return switch (mobAgeFilter.get()) {
                case Baby -> animal.isBaby();
                case Adult -> !animal.isBaby();
                case Both -> true;
            };
        }
        return true;
    }

    private boolean delayCheck() {
        if (switchTimer > 0) {
            switchTimer--;
            return false;
        }

        float delay = 0.5f;
        if (tpsSync.get()) delay /= (TickRate.INSTANCE.getTickRate() / 20);

        return mc.player.getAttackCooldownProgress(delay) >= 1;
    }

    private void attack(Entity target) {
        if (rotation.get() == RotationMode.OnHit) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));
            MeteorClient.mc.player.setYaw((float) Rotations.getYaw(target));
            MeteorClient.mc.player.setPitch((float) Rotations.getPitch(target, Target.Body));
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean itemInHand() {
        if (shouldShieldBreak()) return mc.player.getMainHandStack().getItem() instanceof AxeItem;

        return switch (weapon.get()) {
            case Axe -> mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword -> mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case Mace -> mc.player.getMainHandStack().getItem() instanceof MaceItem;
            case All -> mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof MaceItem;
            default -> true;
        };
    }

    public Entity getTarget() {
        if (!targets.isEmpty()) return targets.getFirst();
        return null;
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) return EntityUtils.getName(getTarget());
        return null;
    }

    public enum Weapon {
        Sword,
        Axe,
        Mace,
        All,
        Any
    }

    public enum RotationMode {
        Always,
        OnHit,
        None
    }

    public enum ShieldMode {
        Ignore,
        Break,
        None
    }

    public enum EntityAge {
        Baby,
        Adult,
        Both
    }
}
