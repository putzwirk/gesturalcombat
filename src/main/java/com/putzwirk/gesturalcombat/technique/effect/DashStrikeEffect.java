package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.DashMovement;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.HitStun;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DashStrikeEffect implements TechniqueEffect {
    private static final int MOVE_TICKS = 9;
    private static final int LIGHT_MOVE_TICKS = 6;
    private static final double STEP = 0.42;
    private static final double LIGHT_STEP = 0.62;
    private static final double LIGHT_REACH = 1.30;
    private static final double REACH = 1.15;
    private static final double LIGHT_MAX_TRAVEL = 6.0;
    private static final double MAX_TRAVEL = 6.5;
    private static final double FAST_ATTACK_SPEED = 2.0;
    private static final double CONTACT = 1.6;
    private static final int HIT_SPACING = 4;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final double dash;
    private final double pull;
    private final int pin;
    private final FamilyRule rule;
    private final int hits;
    private final boolean flip;

    public DashStrikeEffect(double range, double arcDegrees, double damageMultiplier, double dash, double pull, int pin,
                            FamilyRule rule) {
        this(range, arcDegrees, damageMultiplier, dash, pull, pin, rule, 1, false);
    }

    public DashStrikeEffect(double range, double arcDegrees, double damageMultiplier, double dash, double pull, int pin,
                            FamilyRule rule, int hits, boolean flip) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.dash = dash;
        this.pull = pull;
        this.pin = Math.max(0, pin);
        this.rule = rule;
        this.hits = Math.max(1, hits);
        this.flip = flip;
    }

    public DashStrikeEffect(double range, double arcDegrees, double damageMultiplier, double dash, FamilyRule rule) {
        this(range, arcDegrees, damageMultiplier, dash, 0.0, 0, rule);
    }

    @Override
    public int windupTicks() {
        return 2;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier));
        int windup = AbilityConfigs.windupTicks(technique, stack);
        boolean light = lightweight(player, stack);
        double travel = Math.min(light ? LIGHT_MAX_TRAVEL : MAX_TRAVEL, dash * (light ? LIGHT_REACH : REACH));
        LivingEntity focus = player.onGround() ? EffectSupport.targetInFront(player, hitbox.range() + 2.5, 70.0) : null;
        Vec3 destination = focus == null
                ? DashMovement.forwardPoint(player, travel)
                : DashMovement.stopPoint(player, focus, CONTACT, travel);
        int ticks = DashMovement.run(player, level, destination, light ? LIGHT_MOVE_TICKS : MOVE_TICKS,
                windup + 1, light ? LIGHT_STEP : STEP);
        int land = windup + ticks;
        float per = damage / hits;
        for (int i = 0; i < hits; i++) {
            int index = i;
            GestureScheduler.schedule(level, land + i * HIT_SPACING,
                    () -> strike(player, level, stack, technique, hitbox, per, index));
        }
        int finish = land + (hits - 1) * HIT_SPACING + 3;
        if (flip) {
            GestureScheduler.schedule(level, finish, () -> {
                if (player.isAlive() && !player.isRemoved() && player.level() == level) {
                    com.putzwirk.gesturalcombat.server.Backflip.launch(player);
                }
            });
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 back = flat.normalize().scale(-0.85);
        GestureScheduler.schedule(level, finish, () -> {
            if (player.isAlive() && !player.isRemoved() && player.onGround()) {
                DashMovement.retreat(player, level, back, 1.6, 5);
            }
        });
    }

    private static boolean lightweight(ServerPlayer player, ItemStack stack) {
        if (com.putzwirk.gesturalcombat.weapon.WeaponRules.light(stack)) {
            return true;
        }
        return player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)
                >= FAST_ATTACK_SPEED;
    }

    private void strike(ServerPlayer player, Level level, ItemStack stack, Technique technique, Hitbox hitbox,
                        float damage, int index) {
        if (!player.isAlive() || player.isRemoved()) {
            return;
        }
        double force = AbilityConfigs.knockback(technique, stack, 0.05);
        double lift = AbilityConfigs.lift(technique, stack, 0.0);
        boolean pinning = pin > 0 || pull > 0.0;
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, (float) Math.max(0.0, rule.adjust(player, target, damage, index)),
                    technique, force, lift, pinning);
            if (pin > 0) {
                HitStun.pin(target, pin);
            }
            if (pull > 0.0) {
                Vec3 toward = player.position().subtract(target.position());
                Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
                if (flat.lengthSqr() > 1.0E-4) {
                    Vec3 shove = flat.normalize().scale(pull);
                    target.push(shove.x, 0.22, shove.z);
                    target.hurtMarked = true;
                }
            }
        }
    }
}
