package com.cybergym.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.EnumMap;
import java.util.Map;

/** All callbacks run on Minecraft's client/render thread. */
public final class ModuleManager {
    public interface Handler {
        default void onEnable() {}
        default void onDisable() {}
        default void onUpdate(MinecraftClient mc) {}
        default void onRender(DrawContext dc) {}
    }
    private final Map<Module, Boolean> enabled = new EnumMap<>(Module.class);
    private final Map<Module, Handler> handlers = new EnumMap<>(Module.class);
    public ModuleManager() { reset(); }
    public void register(Module m, Handler h) {
        handlers.put(m, h);
        if (enabled(m)) h.onEnable();
    }
    public void reset() { for (Module m : Module.values()) set(m, m.initiallyEnabled); }
    public boolean enabled(Module m) { return enabled.getOrDefault(m, false); }
    public void set(Module m, boolean value) {
        boolean old = enabled(m);
        enabled.put(m, value);
        if (value != old) {
            Handler handler = handlers.get(m);
            if (handler != null) {
                try {
                    if (value) handler.onEnable(); else handler.onDisable();
                } catch (RuntimeException ex) {
                    enabled.put(m, false);
                    System.err.println("[CyberGym] Module state error " + m + ": " + ex);
                }
            }
        }
    }
    public boolean toggle(Module m) { boolean value = !enabled(m); set(m, value); return enabled(m); }
    public Map<Module, Boolean> snapshot() { return new EnumMap<>(enabled); }
    public void update(MinecraftClient mc) {
        for (Map.Entry<Module, Handler> e : handlers.entrySet()) if (enabled(e.getKey())) {
            try { e.getValue().onUpdate(mc); }
            catch (RuntimeException ex) {
                System.err.println("[CyberGym] Disabling failing feature " + e.getKey() + ": " + ex);
                set(e.getKey(), false);
            }
        }
    }
    public void render(DrawContext dc) {
        for (Map.Entry<Module, Handler> e : handlers.entrySet()) if (enabled(e.getKey())) {
            try { e.getValue().onRender(dc); }
            catch (RuntimeException ex) {
                System.err.println("[CyberGym] Disabling failing renderer " + e.getKey() + ": " + ex);
                set(e.getKey(), false);
            }
        }
    }
}