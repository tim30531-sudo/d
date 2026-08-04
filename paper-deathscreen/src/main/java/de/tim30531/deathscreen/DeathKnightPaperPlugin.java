package de.tim30531.deathscreen;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DeathKnightPaperPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final String PACK_RESOURCE = "death-knight-pack.zip";
    private static final int AVAILABLE_FRAMES = 40;
    private static final int FIRST_GLYPH = 0xE000;
    private static final Key FRAME_FONT = Key.key("deathscreen", "frames");
    private static final Title.Times FRAME_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofMillis(90),
            Duration.ZERO
    );

    private final Set<UUID> pendingDeaths = new HashSet<>();
    private final Set<UUID> loadedPacks = new HashSet<>();
    private final Map<UUID, AnimationState> animations = new HashMap<>();
    private final Map<UUID, Boolean> oldImmediateRespawn = new HashMap<>();

    private Path packPath;
    private byte[] packSha1;
    private String packUrl = "";
    private EmbeddedPackServer packServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            preparePack();
            restartPackServer();
        } catch (Exception exception) {
            getLogger().severe("Das Resourcepack konnte nicht vorbereitet werden: " + exception.getMessage());
            exception.printStackTrace();
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("deathscreen") != null) {
            getCommand("deathscreen").setExecutor(this);
        }

        for (World world : Bukkit.getWorlds()) {
            enableImmediateRespawn(world);
        }

        getLogger().info("DeathKnightScreen Paper 1.0.0 aktiviert.");
        if (packUrl.isBlank()) {
            getLogger().warning("Noch keine erreichbare Resourcepack-URL konfiguriert. Setze web-server.public-host oder resource-pack.url.");
        } else {
            getLogger().info("Resourcepack-URL: " + packUrl);
            getLogger().info("Resourcepack-SHA1: " + HexFormat.of().formatHex(packSha1));
        }
    }

    @Override
    public void onDisable() {
        for (AnimationState state : animations.values()) {
            state.task.cancel();
            restorePlayer(state);
        }
        animations.clear();

        if (packServer != null) {
            packServer.close();
            packServer = null;
        }

        for (World world : Bukkit.getWorlds()) {
            Boolean oldValue = oldImmediateRespawn.get(world.getUID());
            if (oldValue != null) {
                world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, oldValue);
            }
        }
        oldImmediateRespawn.clear();
    }

    private void preparePack() throws IOException, NoSuchAlgorithmException {
        Files.createDirectories(getDataFolder().toPath());
        packPath = getDataFolder().toPath().resolve(PACK_RESOURCE);
        try (InputStream input = getResource(PACK_RESOURCE)) {
            if (input == null) {
                throw new IOException("Die eingebettete Datei " + PACK_RESOURCE + " fehlt.");
            }
            Files.copy(input, packPath, StandardCopyOption.REPLACE_EXISTING);
        }
        packSha1 = sha1(packPath);
    }

    private void restartPackServer() throws IOException {
        if (packServer != null) {
            packServer.close();
            packServer = null;
        }

        reloadConfig();
        boolean webEnabled = getConfig().getBoolean("web-server.enabled", true);
        String directUrl = getConfig().getString("resource-pack.url", "").trim();
        int port = getConfig().getInt("web-server.port", 8123);

        if (webEnabled) {
            String bindAddress = getConfig().getString("web-server.bind-address", "0.0.0.0").trim();
            packServer = new EmbeddedPackServer(bindAddress, port, packPath);
            packServer.start();
            getLogger().info("Resourcepack-Webserver lauscht auf " + bindAddress + ":" + port + ".");
        }

        packUrl = !directUrl.isBlank() ? directUrl : buildEmbeddedPackUrl(port);
    }

    private String buildEmbeddedPackUrl(int port) {
        String host = getConfig().getString("web-server.public-host", "CHANGE_ME").trim();
        if (host.isBlank() || host.equalsIgnoreCase("CHANGE_ME")) {
            return "";
        }

        String base = host.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$") ? host : "http://" + host;
        base = base.replaceAll("/+$", "");
        try {
            URI uri = new URI(base);
            if (uri.getPort() < 0) {
                base += ":" + port;
            }
        } catch (URISyntaxException ignored) {
            base += ":" + port;
        }
        return base + "/death-knight-pack.zip";
    }

    private static byte[] sha1(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return digest.digest();
    }

    private void enableImmediateRespawn(World world) {
        Boolean current = world.getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN);
        oldImmediateRespawn.putIfAbsent(world.getUID(), current != null && current);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    }

    private void sendPack(Player player) {
        if (packUrl.isBlank() || packSha1 == null) {
            player.sendMessage(Component.text("Das DeathScreen-Resourcepack ist noch nicht erreichbar konfiguriert.", NamedTextColor.RED));
            return;
        }
        player.setResourcePack(packUrl, packSha1);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        enableImmediateRespawn(event.getWorld());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        int delay = Math.max(1, getConfig().getInt("resource-pack.send-delay-ticks", 20));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline()) {
                sendPack(event.getPlayer());
            }
        }, delay);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> loadedPacks.add(player.getUniqueId());
            case DECLINED, FAILED_DOWNLOAD -> {
                loadedPacks.remove(player.getUniqueId());
                if (getConfig().getBoolean("resource-pack.required", true)) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        if (player.isOnline()) {
                            player.kick(Component.text("Dieses Server-Resourcepack wird für die Todesanimation benötigt.", NamedTextColor.RED));
                        }
                    });
                }
            }
            default -> {
                // ACCEPTED und weitere Zwischenzustände benötigen keine Aktion.
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        pendingDeaths.add(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!pendingDeaths.remove(id)) {
            return;
        }

        int delay = Math.max(1, getConfig().getInt("animation.start-delay-ticks", 2));
        Bukkit.getScheduler().runTaskLater(this, () -> playAnimation(event.getPlayer()), delay);
    }

    private void playAnimation(Player player) {
        stopAnimation(player.getUniqueId(), false);

        boolean freeze = getConfig().getBoolean("animation.freeze-player", true);
        AnimationState state = new AnimationState(
                player,
                player.getLocation().clone(),
                player.getWalkSpeed(),
                player.getFlySpeed(),
                player.isInvulnerable(),
                freeze
        );

        if (freeze) {
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
            player.setInvulnerable(true);
        }

        int configuredFrames = Math.max(1, getConfig().getInt("animation.frame-count", AVAILABLE_FRAMES));
        int frameTicks = Math.max(1, getConfig().getInt("animation.frame-ticks", 1));

        BukkitRunnable runnable = new BukkitRunnable() {
            private int step;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopAnimation(player.getUniqueId(), false);
                    cancel();
                    return;
                }

                if (step >= configuredFrames) {
                    player.clearTitle();
                    stopAnimation(player.getUniqueId(), true);
                    cancel();
                    return;
                }

                int frame = configuredFrames == 1
                        ? AVAILABLE_FRAMES - 1
                        : Math.round(step * (AVAILABLE_FRAMES - 1.0f) / (configuredFrames - 1.0f));
                frame = Math.max(0, Math.min(AVAILABLE_FRAMES - 1, frame));

                char glyph = (char) (FIRST_GLYPH + frame);
                Component image = Component.text(String.valueOf(glyph)).font(FRAME_FONT);
                player.showTitle(Title.title(image, Component.empty(), FRAME_TIMES));
                step++;
            }
        };

        BukkitTask task = runnable.runTaskTimer(this, 0L, frameTicks);
        state.task = task;
        animations.put(player.getUniqueId(), state);
    }

    private void stopAnimation(UUID playerId, boolean restore) {
        AnimationState state = animations.remove(playerId);
        if (state == null) {
            return;
        }
        if (!state.task.isCancelled()) {
            state.task.cancel();
        }
        if (restore) {
            restorePlayer(state);
        }
    }

    private void restorePlayer(AnimationState state) {
        Player player = state.player;
        if (!player.isOnline()) {
            return;
        }
        player.clearTitle();
        if (state.freeze) {
            player.setWalkSpeed(state.walkSpeed);
            player.setFlySpeed(state.flySpeed);
            player.setInvulnerable(state.invulnerable);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        AnimationState state = animations.get(event.getPlayer().getUniqueId());
        if (state == null || !state.freeze || event.getTo() == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(from);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && animations.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        pendingDeaths.remove(id);
        loadedPacks.remove(id);
        stopAnimation(id, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/deathscreen test, /deathscreen pack oder /deathscreen reload", NamedTextColor.AQUA));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "test" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Dieser Befehl kann nur im Spiel ausgeführt werden.");
                    return true;
                }
                playAnimation(player);
                sender.sendMessage(Component.text("Todesanimation gestartet.", NamedTextColor.AQUA));
                return true;
            }
            case "pack" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Dieser Befehl kann nur im Spiel ausgeführt werden.");
                    return true;
                }
                sendPack(player);
                sender.sendMessage(Component.text("Resourcepack wurde erneut gesendet.", NamedTextColor.AQUA));
                return true;
            }
            case "reload" -> {
                try {
                    restartPackServer();
                    sender.sendMessage(Component.text("DeathScreen-Konfiguration neu geladen.", NamedTextColor.GREEN));
                } catch (IOException exception) {
                    sender.sendMessage(Component.text("Neuladen fehlgeschlagen: " + exception.getMessage(), NamedTextColor.RED));
                    getLogger().severe("Neuladen fehlgeschlagen: " + exception.getMessage());
                }
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Unbekannter Unterbefehl.", NamedTextColor.RED));
                return true;
            }
        }
    }

    private static final class AnimationState {
        private final Player player;
        @SuppressWarnings("unused")
        private final Location lockedLocation;
        private final float walkSpeed;
        private final float flySpeed;
        private final boolean invulnerable;
        private final boolean freeze;
        private BukkitTask task;

        private AnimationState(Player player, Location lockedLocation, float walkSpeed, float flySpeed,
                               boolean invulnerable, boolean freeze) {
            this.player = player;
            this.lockedLocation = lockedLocation;
            this.walkSpeed = walkSpeed;
            this.flySpeed = flySpeed;
            this.invulnerable = invulnerable;
            this.freeze = freeze;
        }
    }
}
