package com.example.aimlock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AimLockClient implements ClientModInitializer {

    private static final double MELEE_RANGE = 6.0;
    private static final double BOW_RANGE = 30.0;
    private static boolean onlyHostile = true;

    private static boolean enabled = false;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aimlock.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.aimlock"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal(enabled ? "[AimLock] Đã BẬT" : "[AimLock] Đã TẮT"),
                        true
                );
            }
        }

        if (!enabled || client.player == null || client.world == null) return;

        boolean attacking = client.options.attackKey.isPressed();
        boolean usingBow = client.options.useKey.isPressed()
                && (client.player.getMainHandStack().getItem() instanceof BowItem
                || client.player.getMainHandStack().getItem() instanceof CrossbowItem
                || client.player.getOffHandStack().getItem() instanceof BowItem
                || client.player.getOffHandStack().getItem() instanceof CrossbowItem);

        if (!attacking && !usingBow) return;

        double range = usingBow ? BOW_RANGE : MELEE_RANGE;
        Box box = client.player.getBoundingBox().expand(range);

        LivingEntity target = null;
        double bestDistSq = Double.MAX_VALUE;

        List<LivingEntity> candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                box,
                e -> e.isAlive()
                        && e != client.player
                        && (!onlyHostile || e instanceof HostileEntity)
        );

        for (LivingEntity e : candidates) {
            double distSq = client.player.squaredDistanceTo(e);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                target = e;
            }
        }

        if (target != null) {
            aimAt(client, target);
        }
    }

    private void aimAt(MinecraftClient client, LivingEntity target) {
        Vec3d eyePos = client.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -(Math.toDegrees(Math.atan2(dy, horizontalDist)));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        client.player.prevYaw = yaw;
        client.player.prevPitch = pitch;
    }
          }
