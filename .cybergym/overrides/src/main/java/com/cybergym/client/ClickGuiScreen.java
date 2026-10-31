package com.cybergym.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Searchable, paginated, clickable GUI. All controls save to current profile. */
public final class ClickGuiScreen extends Screen {
    private static final int BG = 0xF0181A27, WHITE = 0xFFE7EDF6;
    private static final int ACCENT = 0xFF39D6BA, MUTED = 0xFFB1BED4;
    private enum Page { MODULES, SETTINGS, PROFILES }
    private Page page = Page.MODULES;
    private String search = "", feedback = "";
    private TextFieldWidget searchBox, profileName;
    private int offset = 0, x0, y0, w, h;
    private static final int ROW_H = 29;

    public ClickGuiScreen() { super(Text.literal("CyberGym PvP")); }
    private void refresh() { clearChildren(); init(); }
    private void go(Page p) { page = p; offset = 0; refresh(); }
    @Override protected void init() {
        w = Math.min(width - 16, 530);
        h = Math.min(height - 14, 380);
        x0 = (width - w) / 2; y0 = (height - h) / 2;
        int third = (w - 34) / 3;
        make("Modules", x0 + 8, y0 + 28, third, 21, () -> go(Page.MODULES));
        make("Settings", x0 + 13 + third, y0 + 28, third, 21, () -> go(Page.SETTINGS));
        make("Profiles", x0 + 18 + 2 * third, y0 + 28, third, 21, () -> go(Page.PROFILES));
        if (page == Page.MODULES) showModules();
        else if (page == Page.SETTINGS) showSettings();
        else showProfiles();
        make("Close / Esc", x0 + w - 113, y0 + h - 26, 105, 18, this::close);
    }
    private int rows() { return Math.max(2, (h - 151) / ROW_H); }
    private void showModules() {
        searchBox = new TextFieldWidget(textRenderer, x0 + 8, y0 + 56, w - 16, 21,
            Text.literal("Filter modules"));
        searchBox.setMaxLength(50);
        searchBox.setText(search);
        searchBox.setChangedListener(s -> {
            if (!s.equals(search)) {
                search = s; offset = 0; refresh();
                if (searchBox != null) { searchBox.setFocused(true); setFocused(searchBox); }
            }
        });
        addDrawableChild(searchBox);
        String query = search.toLowerCase(Locale.ROOT).trim();
        List<Module> found = Arrays.stream(Module.values()).filter(m ->
            (m.title + " " + m.category + " " + m.description).toLowerCase(Locale.ROOT).contains(query)).toList();
        int count = rows() * 2;
        offset = Math.min(offset, Math.max(0, found.size() - count));
        int colWidth = (w - 30) / 2;
        for (int i = 0; i < count && offset + i < found.size(); i++) {
            Module m = found.get(offset + i);
            int xx = x0 + 8 + (i / rows()) * (colWidth + 6);
            int yy = y0 + 83 + (i % rows()) * ROW_H;
            make((CyberGymClient.modules().enabled(m) ? "[ON] " : "[OFF] ") + m.title,
                xx, yy, colWidth, 24, () -> {
                    CyberGymClient.modules().toggle(m);
                    CyberGymClient.saveSettings();
                    refresh();
                });
        }
        make("<", x0 + 8, y0 + h - 49, 40, 19, () -> {offset = Math.max(0, offset - count); refresh();});
        make(">", x0 + 52, y0 + h - 49, 40, 19,
            () -> {offset = Math.min(Math.max(0, found.size() - count), offset + count); refresh();});
        feedback = found.size() + " matching modules | " + Module.values().length + " total";
    }
    private record Option(String title, Supplier<String> value, Runnable minus, Runnable plus) {}
    private List<Option> options() {
        Settings s = CyberGymClient.settings();
        return List.of(
            new Option("Attack CPS", () -> "" + s.maxCps, () -> s.maxCps--, () -> s.maxCps++),
            new Option("Aim degrees/tick", () -> f(s.aimDegreesPerTick),
                () -> s.aimDegreesPerTick -= 0.2, () -> s.aimDegreesPerTick += 0.2),
            new Option("Aim FOV degrees", () -> "" + s.aimFovDegrees,
                () -> s.aimFovDegrees -= 2, () -> s.aimFovDegrees += 2),
            new Option("Max hit distance", () -> f(s.maxTargetRange),
                () -> s.maxTargetRange -= 0.25, () -> s.maxTargetRange += 0.25),
            new Option("Trainer goal CPS", () -> "" + s.trainerGoalCps,
                () -> s.trainerGoalCps--, () -> s.trainerGoalCps++),
            new Option("Overlay X", () -> "" + s.overlayX, () -> s.overlayX -= 4, () -> s.overlayX += 4),
            new Option("Overlay Y", () -> "" + s.overlayY, () -> s.overlayY -= 4, () -> s.overlayY += 4),
            new Option("Radar radius", () -> "" + s.radarRadius,
                () -> s.radarRadius -= 8, () -> s.radarRadius += 8),
            new Option("Radar diameter", () -> "" + s.radarSize,
                () -> s.radarSize -= 10, () -> s.radarSize += 10),
            new Option("Low HP alert (hearts)", () -> "" + s.lowHealthHearts,
                () -> s.lowHealthHearts--, () -> s.lowHealthHearts++),
            new Option("Entities per overlay", () -> "" + s.entityBudget,
                () -> s.entityBudget -= 4, () -> s.entityBudget += 4)
        );
    }
    private void showSettings() {
        List<Option> all = options();
        int count = Math.max(2, (h - 155) / ROW_H);
        offset = Math.min(offset, Math.max(0, all.size() - count));
        for (int i = offset; i < Math.min(all.size(), offset + count); i++) {
            Option opt = all.get(i);
            int y = y0 + 64 + (i - offset) * ROW_H;
            make("-", x0 + w - 85, y, 34, 22, () -> change(opt.minus));
            make("+", x0 + w - 46, y, 34, 22, () -> change(opt.plus));
        }
        Settings s = CyberGymClient.settings();
        int foot = y0 + h - 104;
        make("Aim: " + s.aimCurve.name(), x0 + 8, foot, (w - 24) / 2, 20, () -> {
            Settings.AimCurve[] values = Settings.AimCurve.values();
            s.aimCurve = values[(s.aimCurve.ordinal() + 1) % values.length];
            CyberGymClient.saveSettings(); refresh();
        });
        make("Cooldown: " + (s.respectCooldown ? "ON" : "OFF"),
            x0 + 16 + (w - 24) / 2, foot, (w - 24) / 2, 20, () -> {
                s.respectCooldown = !s.respectCooldown; CyberGymClient.saveSettings(); refresh();
            });
        make("< Settings", x0 + 8, y0 + h - 73, 100, 19,
            () -> {offset = Math.max(0, offset - count); refresh();});
        make("Settings >", x0 + 113, y0 + h - 73, 100, 19,
            () -> {offset = Math.min(Math.max(0, all.size() - count), offset + count); refresh();});
        feedback = "Settings " + (offset + 1) + "-" + Math.min(offset + count, all.size()) + "/" + all.size();
    }
    private void change(Runnable r) {
        r.run(); CyberGymClient.settings().normalize(); CyberGymClient.saveSettings(); refresh();
    }
    private void showProfiles() {
        int top = y0 + 58;
        profileName = new TextFieldWidget(textRenderer, x0 + 8, top, w - 16, 22,
            Text.literal("Profile name"));
        profileName.setMaxLength(32);
        profileName.setText(CyberGymClient.config().activeName());
        addDrawableChild(profileName);
        make("Save as / create", x0 + 8, top + 30, (w - 24) / 2, 22, () -> {
            String name = profileName.getText();
            try { CyberGymClient.config().saveAs(name); feedback = "Saved: " + name; }
            catch (IOException | IllegalArgumentException ex) { feedback = "Save failed: " + ex.getMessage(); }
            refresh();
        });
        make("Load by name", x0 + 16 + (w - 24) / 2, top + 30, (w - 24) / 2, 22, () -> {
            String name = profileName.getText();
            try { CyberGymClient.config().loadProfile(name); feedback = "Loaded: " + name; }
            catch (IOException | IllegalArgumentException ex) { feedback = "Load failed: " + ex.getMessage(); }
            refresh();
        });
        make("Reset to safe defaults", x0 + 8, top + 59, w - 16, 21, () -> {
            CyberGymClient.modules().reset(); CyberGymClient.settings().reset();
            CyberGymClient.saveSettings(); feedback = "Defaults restored"; refresh();
        });
        try {
            List<String> names = CyberGymClient.config().profiles();
            for (int i = 0; i < Math.min(names.size(), Math.max(0, (h - 207) / 24)); i++) {
                String name = names.get(i);
                make("Load: " + name, x0 + 8, top + 94 + i * 24, w - 16, 21, () -> {
                    try { CyberGymClient.config().loadProfile(name); feedback = "Loaded: " + name; }
                    catch (IOException ex) { feedback = "Load failed: " + ex.getMessage(); }
                    refresh();
                });
            }
        } catch (IOException ex) { feedback = "Profile list failed: " + ex.getMessage(); }
    }
    private void make(String label, int x, int y, int width, int height, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> action.run())
            .dimensions(x, y, width, height).build());
    }
    @Override public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        renderBackground(dc, mouseX, mouseY, delta);
        dc.fill(x0, y0, x0 + w, y0 + h, BG);
        dc.fill(x0, y0, x0 + w, y0 + 3, ACCENT);
        dc.drawTextWithShadow(textRenderer, "CYBERGYM PvP  |  1.21.11  |  v0.7",
            x0 + 10, y0 + 9, ACCENT);
        if (page == Page.SETTINGS) {
            List<Option> all = options();
            int count = Math.max(2, (h - 155) / ROW_H);
            for (int i = offset; i < Math.min(offset + count, all.size()); i++) {
                Option opt = all.get(i);
                dc.drawTextWithShadow(textRenderer, opt.title + ": " + opt.value.get(),
                    x0 + 10, y0 + 70 + (i - offset) * ROW_H, WHITE);
            }
        }
        dc.drawTextWithShadow(textRenderer, feedback.length() > 74 ?
            feedback.substring(0, 74) : feedback, x0 + 8, y0 + h - 66, MUTED);
        dc.drawTextWithShadow(textRenderer, "Right Shift = GUI  |  V sword  B bow  G totem  H apple  J pick",
            x0 + 8, y0 + h - 16, MUTED);
        super.render(dc, mouseX, mouseY, delta);
    }
    private static String f(double n) { return String.format(Locale.ROOT, "%.2f", n); }
    @Override public boolean shouldPause() { return false; }
}