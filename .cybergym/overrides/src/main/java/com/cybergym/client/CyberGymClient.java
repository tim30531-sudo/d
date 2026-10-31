package com.cybergym.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * CyberGym PvP. Client-only. No packet spoofing, packet dropping, or security bypass.
 * All player actions use vanilla APIs and remain server validated.
 */
public final class CyberGymClient implements ClientModInitializer {
    private static final ModuleManager MODULES = new ModuleManager();
    private static final Settings SETTINGS = new Settings();
    private static final CpsTracker CLICKS = new CpsTracker();
    private static final AntiCheatGuard GUARD = new AntiCheatGuard();
    private static ConfigStore config;
    private KeyBinding guiKey, swordKey, bowKey, totemKey, appleKey, pickKey;
    private boolean shiftWasHeld, mouseWasHeld;
    private long lastFailureNoticeNs, ticks;
    private long lastUserInputTick;
    private boolean disconnectedForHealth;

    public static ModuleManager modules() { return MODULES; }
    public static Settings settings() { return SETTINGS; }
    public static CpsTracker clicks() { return CLICKS; }
    public static AntiCheatGuard guard() { return GUARD; }
    public static ConfigStore config() { return config; }

    @Override public void onInitializeClient() {
        registerHandlers();
        config = new ConfigStore(FabricLoader.getInstance().getConfigDir(), MODULES, SETTINGS);
        try { config.load(); }
        catch (IOException | RuntimeException ex) {
            MODULES.reset(); SETTINGS.reset();
            System.err.println("[CyberGym] Configuration failed, safe defaults loaded: " + ex);
        }
        guiKey = bind("gui", GLFW.GLFW_KEY_RIGHT_SHIFT);
        swordKey = bind("sword", GLFW.GLFW_KEY_V);
        bowKey = bind("bow", GLFW.GLFW_KEY_B);
        totemKey = bind("totem", GLFW.GLFW_KEY_G);
        appleKey = bind("apple", GLFW.GLFW_KEY_H);
        pickKey = bind("pickaxe", GLFW.GLFW_KEY_J);
        ClientTickEvents.END_CLIENT_TICK.register(this::safeTick);
        HudElementRegistry.addLast(Identifier.of("cybergym_pvp", "hud"), HudOverlay::render);
        System.out.println("[CyberGym] v0.9 loaded with post-action and teleport safety. Right Shift opens ClickGUI.");
    }
    private static KeyBinding bind(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cybergym_pvp." + id, InputUtil.Type.KEYSYM, key, KeyBinding.Category.MISC));
    }
    private void registerHandlers() {
        MODULES.register(Module.AUTO_SPRINT, new ModuleManager.Handler() {
            @Override public void onUpdate(MinecraftClient mc) {
                if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking()
                    && !mc.player.isUsingItem()) mc.player.setSprinting(true);
            }
        });
        MODULES.register(Module.AUTO_JUMP, new ModuleManager.Handler() {
            @Override public void onUpdate(MinecraftClient mc) {
                if (mc.options.forwardKey.isPressed() && mc.player.isOnGround()
                    && !mc.player.isSneaking() && !mc.player.isUsingItem() && ticks % 6 == 0)
                    mc.player.jump();
            }
        });
        MODULES.register(Module.SAFE_WALK, new ModuleManager.Handler() {
            @Override public void onUpdate(MinecraftClient mc) {
                if (!mc.player.isOnGround() || !mc.options.forwardKey.isPressed()) return;
                float yaw = (float)Math.toRadians(mc.player.getYaw());
                double dx = -Math.sin(yaw) * 0.72;
                double dz = Math.cos(yaw) * 0.72;
                BlockPos below = BlockPos.ofFloored(
                    mc.player.getX() + dx, mc.player.getY() - 1.0, mc.player.getZ() + dz);
                if (mc.world.getBlockState(below).isAir()
                    && mc.world.getBlockState(below.down()).isAir()) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(0.0, v.y, 0.0);
                }
            }
        });
        MODULES.register(Module.ANTI_AFK, new ModuleManager.Handler() {
            @Override public void onUpdate(MinecraftClient mc) {
                if (ticks - lastUserInputTick > 300 && ticks % 300 == 0) {
                    mc.player.setYaw(mc.player.getYaw() + 1.0f);
                }
            }
        });
        MODULES.register(Module.AUTO_TOOL, new ModuleManager.Handler() {
            @Override public void onUpdate(MinecraftClient mc) {
                if (!mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof BlockHitResult hit))
                    return;
                BlockState state = mc.world.getBlockState(hit.getBlockPos());
                if (state.isAir()) return;
                int selected = mc.player.getInventory().getSelectedSlot();
                float best = mc.player.getInventory().getStack(selected).getMiningSpeedMultiplier(state);
                int bestIndex = selected;
                for (int i = 0; i < 9; i++) {
                    float speed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
                    if (speed > best + 0.01f) { best = speed; bestIndex = i; }
                }
                if (bestIndex != selected) mc.player.getInventory().setSelectedSlot(bestIndex);
            }
        });
        MODULES.register(Module.AUTO_LEAVE, new ModuleManager.Handler() {
            @Override public void onEnable() { disconnectedForHealth = false; }
            @Override public void onUpdate(MinecraftClient mc) {
                if (!disconnectedForHealth && mc.getCurrentServerEntry() != null
                    && mc.player.getHealth() <= SETTINGS.lowHealthHearts * 2.0f) {
                    disconnectedForHealth = true;
                    mc.disconnect(Text.literal("CyberGym: low health"));
                }
            }
        });
    }
    public static void saveSettings() {
        if (config == null) return;
        try { config.save(); }
        catch (IOException ex) { System.err.println("[CyberGym] Settings save failed: " + ex); }
    }
    private void safeTick(MinecraftClient mc) {
        try { tick(mc); }
        catch (RuntimeException | LinkageError ex) {
            long now = System.nanoTime();
            if (now - lastFailureNoticeNs > 10_000_000_000L) {
                lastFailureNoticeNs = now;
                System.err.println("[CyberGym] Recoverable client tick error: " + ex);
                ex.printStackTrace();
            }
        }
    }
    private void tick(MinecraftClient mc) {
        if (mc == null) return;
        boolean guiPressed = guiKey.wasPressed();
        boolean rawShift = mc.getWindow() != null &&
            GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean shiftEdge = rawShift && !shiftWasHeld;
        shiftWasHeld = rawShift;
        if (guiPressed || shiftEdge) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
            else if (mc.currentScreen instanceof ClickGuiScreen) mc.setScreen(null);
        }
        boolean sword = swordKey.wasPressed();
        boolean bow = bowKey.wasPressed();
        boolean totem = totemKey.wasPressed();
        boolean apple = appleKey.wasPressed();
        boolean pick = pickKey.wasPressed();
        boolean physicalClick = mc.getWindow() != null &&
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (mc.currentScreen == null && mc.player != null && physicalClick && !mouseWasHeld)
            CLICKS.click(System.currentTimeMillis());
        mouseWasHeld = physicalClick;
        if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null) {
            GUARD.resetTransient();
            return;
        }
        ticks++;
        GUARD.trackView(mc, ticks);
        GUARD.observePlayerContext(mc, ticks, SETTINGS);
        if (sword && MODULES.enabled(Module.QUICK_SWORD)) equipHotbar(mc, stack -> stack.isIn(ItemTags.SWORDS));
        if (bow && MODULES.enabled(Module.QUICK_BOW)) equipHotbar(mc,
            stack -> stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem);
        if (totem && MODULES.enabled(Module.QUICK_TOTEM))
            equipHotbar(mc, stack -> stack.isOf(Items.TOTEM_OF_UNDYING));
        if (apple && MODULES.enabled(Module.QUICK_GAPPLE))
            equipHotbar(mc, stack -> stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE));
        if (pick && MODULES.enabled(Module.QUICK_PICKAXE)) equipHotbar(mc, stack -> stack.isIn(ItemTags.PICKAXES));
        if (mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()
            || mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()
            || mc.options.jumpKey.isPressed()) lastUserInputTick = ticks;
        MODULES.update(mc);
        // Any action after auto-leave returns immediately; the world may be unloaded.
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        boolean auraArmed = MODULES.enabled(Module.KILL_AURA)
            && (!SETTINGS.auraRequiresHeldAttack || mc.options.attackKey.isPressed());
        if ((MODULES.enabled(Module.AIM_ASSIST) && mc.options.attackKey.isPressed())
            || auraArmed) smoothAim(mc);
        PlayerEntity target = crosshairPlayer(mc);
        if (target == null) return;
        if (!(MODULES.enabled(Module.TRIGGER_BOT)
            || auraArmed
            || (MODULES.enabled(Module.AUTO_CLICKER) && mc.options.attackKey.isPressed()))) return;
        if (!GUARD.permitAutomatedAttack(mc, target, SETTINGS, ticks, System.nanoTime(),
            MODULES.enabled(Module.ANTICHEAT_GUARD))) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    private static void equipHotbar(MinecraftClient mc, Predicate<net.minecraft.item.ItemStack> accepts) {
        for (int i = 0; i < 9; i++) {
            if (accepts.test(mc.player.getInventory().getStack(i))) {
                mc.player.getInventory().setSelectedSlot(i); return;
            }
        }
    }
    public static PlayerEntity crosshairPlayer(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.world == null || mc.currentScreen != null
            || !(mc.crosshairTarget instanceof EntityHitResult hit)) return null;
        Entity entity = hit.getEntity();
        return entity instanceof PlayerEntity player && validTarget(mc, player) ? player : null;
    }
    private static boolean validTarget(MinecraftClient mc, PlayerEntity target) {
        return mc.player != null && TargetingRules.eligible(target != mc.player,
            target.isAlive(), target.isSpectator(), mc.player.canSee(target),
            mc.player.squaredDistanceTo(target), SETTINGS.maxTargetRange);
    }
    private static void smoothAim(MinecraftClient mc) {
        PlayerEntity best = null;
        double lowestAngle = SETTINGS.aimFovDegrees;
        Vec3d eye = mc.player.getEyePos();
        for (PlayerEntity candidate : mc.world.getPlayers()) {
            if (!validTarget(mc, candidate)) continue;
            Vec3d direction = candidate.getEyePos().subtract(eye);
            double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            double pitch = -Math.toDegrees(Math.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
            double yError = TargetingRules.wrappedAngle(yaw - mc.player.getYaw());
            double pError = pitch - mc.player.getPitch();
            double error = Math.hypot(yError, pError);
            if (error < lowestAngle) { lowestAngle = error; best = candidate; }
        }
        if (best == null) return;
        Vec3d delta = best.getEyePos().subtract(eye);
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        double pitch = -Math.toDegrees(Math.atan2(delta.y,
            Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        double yawError = TargetingRules.wrappedAngle(yaw - mc.player.getYaw());
        double pitchError = pitch - mc.player.getPitch();
        double stepCap = MODULES.enabled(Module.ANTICHEAT_GUARD)
            ? AntiCheatGuard.maximumRotation(SETTINGS.guardPreset, SETTINGS.aimDegreesPerTick)
            : SETTINGS.aimDegreesPerTick;
        mc.player.setYaw(mc.player.getYaw() + (float)TargetingRules.smoothStep(
            yawError, stepCap, SETTINGS.aimCurve));
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + (float)TargetingRules.smoothStep(
            pitchError, stepCap, SETTINGS.aimCurve), -90f, 90f));
    }
}