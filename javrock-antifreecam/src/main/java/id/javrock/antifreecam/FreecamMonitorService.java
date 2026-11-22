package id.javrock.antifreecam;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreecamMonitorService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavrockAntiFreecamPlugin plugin;
    private final Map<UUID, Location> safeAnchors = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActionTime = new ConcurrentHashMap<>();

    private int checkIntervalTicks;
    private double maxPacketDistance;
    private long actionCooldownMs;
    private boolean blindInEnclosedSpace;
    private boolean allowAboveCave;
    private boolean caveDetectionEnabled;
    private int ceilingCheckHeight;
    private int undergroundYThreshold;
    private boolean snapBack;
    private boolean applyDarkness;
    private boolean sendWarning;

    private BukkitTask task;

    public FreecamMonitorService(JavrockAntiFreecamPlugin plugin) {
        this.plugin = plugin;
        reloadSettings();
    }

    public void reloadSettings() {
        this.checkIntervalTicks = Math.max(1, plugin.getConfig().getInt("settings.check-interval-ticks", 2));
        this.maxPacketDistance = Math.max(0.5, plugin.getConfig().getDouble("settings.max-packet-distance", 2.2));
        this.actionCooldownMs = Math.max(100L, plugin.getConfig().getLong("settings.action-cooldown-ms", 600L));
        this.blindInEnclosedSpace = plugin.getConfig().getBoolean("settings.blind-in-enclosed-space", true);
        this.allowAboveCave = plugin.getConfig().getBoolean("settings.allow-above-cave", true);
        this.caveDetectionEnabled = plugin.getConfig().getBoolean("settings.cave-detection.enabled", true);
        this.ceilingCheckHeight = Math.max(1, plugin.getConfig().getInt("settings.cave-detection.ceiling-check-height", 8));
        this.undergroundYThreshold = plugin.getConfig().getInt("settings.cave-detection.underground-y-threshold", 58);
        this.snapBack = plugin.getConfig().getBoolean("settings.punishment.snap-back", true);
        this.applyDarkness = plugin.getConfig().getBoolean("settings.punishment.apply-darkness", true);
        this.sendWarning = plugin.getConfig().getBoolean("settings.punishment.send-warning", true);
    }

    public void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAllPlayers, checkIntervalTicks, checkIntervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void recordJoin(Player player) {
        safeAnchors.put(player.getUniqueId(), player.getLocation().clone());
    }

    public void removePlayer(UUID uuid) {
        safeAnchors.remove(uuid);
        lastActionTime.remove(uuid);
    }

    public void handleMove(Player player, Location from, Location to) {
        if (shouldIgnore(player)) {
            safeAnchors.put(player.getUniqueId(), to.clone());
            return;
        }

        if (from.getWorld() != null && to.getWorld() != null && !from.getWorld().equals(to.getWorld())) {
            safeAnchors.put(player.getUniqueId(), to.clone());
            return;
        }

        double distance = from.distance(to);
        boolean enclosedOrCave = isEnclosedOrInCave(to);

        if (!enclosedOrCave && distance <= maxPacketDistance * 2.5) {
            safeAnchors.put(player.getUniqueId(), to.clone());
            clearDarknessEffects(player);
            return;
        }

        boolean abnormalJump = distance > maxPacketDistance && !player.isGliding() && !player.isRiptiding();
        if (enclosedOrCave && (abnormalJump || player.getGameMode() == GameMode.SPECTATOR || isCameraBlocked(to))) {
            enforceCaveRestriction(player);
        } else if (enclosedOrCave && blindInEnclosedSpace) {
            applyScreenDarkness(player);
        }
    }

    private void tickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (shouldIgnore(player)) {
                safeAnchors.put(player.getUniqueId(), player.getLocation().clone());
                continue;
            }

            Location loc = player.getLocation();
            boolean enclosedOrCave = isEnclosedOrInCave(loc);

            if (enclosedOrCave) {
                if (blindInEnclosedSpace || applyDarkness) {
                    applyScreenDarkness(player);
                }
                if (player.getGameMode() == GameMode.SPECTATOR || isCameraBlocked(loc)) {
                    enforceCaveRestriction(player);
                }
            } else {
                safeAnchors.put(player.getUniqueId(), loc.clone());
                clearDarknessEffects(player);
            }
        }
    }

    private void enforceCaveRestriction(Player player) {
        if (allowAboveCave) {
            Location anchor = safeAnchors.get(player.getUniqueId());
            if (anchor != null && anchor.getWorld() != null && anchor.getWorld().equals(player.getWorld()) && snapBack) {
                Location target = anchor.clone();
                target.setYaw(player.getLocation().getYaw());
                target.setPitch(player.getLocation().getPitch());
                player.teleportAsync(target);
            }
        }

        if (applyDarkness) {
            applyScreenDarkness(player);
        }

        long now = System.currentTimeMillis();
        Long last = lastActionTime.get(player.getUniqueId());
        if (last == null || (now - last) >= actionCooldownMs) {
            lastActionTime.put(player.getUniqueId(), now);
            if (sendWarning) {
                String prefix = plugin.getConfig().getString("messages.prefix", "&9&lᴊᴀᴠʀᴏᴄᴋ &8» ");
                String msg = plugin.getConfig().getString(
                        "messages.blocked-in-cave",
                        "&cꜰʀᴇᴇᴄᴀᴍ ᴛɪᴅᴀᴋ ᴅɪɪᴢɪɴᴋᴀɴ ᴅɪ ᴅᴀʟᴀᴍ ᴄᴀᴠᴇ ᴀᴛᴀᴜ ʀᴜᴀɴɢ ᴛᴇʀᴛᴜᴛᴜᴘ!"
                );
                player.sendMessage(LEGACY.deserialize(prefix + msg));
            }
        }
    }

    private void applyScreenDarkness(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 45, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 45, 1, false, false, false));

        String actionbar = plugin.getConfig().getString(
                "messages.actionbar-dark",
                "&8[&9&lᴊᴀᴠʀᴏᴄᴋ&8] &7ʟᴀʏᴀʀ ɢᴇʟᴀᴘ: ᴋᴀᴍᴇʀᴀ ᴛᴇʀᴛᴜᴛᴜᴘ ʙʟᴏᴋ / ᴅɪ ᴅᴀʟᴀᴍ ᴄᴀᴠᴇ"
        );
        player.sendActionBar(LEGACY.deserialize(actionbar));
    }

    private void clearDarknessEffects(Player player) {
        PotionEffect blindness = player.getPotionEffect(PotionEffectType.BLINDNESS);
        if (blindness != null && blindness.getDuration() <= 60) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        PotionEffect darkness = player.getPotionEffect(PotionEffectType.DARKNESS);
        if (darkness != null && darkness.getDuration() <= 60) {
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    public boolean isEnclosedOrInCave(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        if (isCameraBlocked(loc)) {
            return true;
        }
        if (!caveDetectionEnabled) {
            return false;
        }

        World world = loc.getWorld();
        int blockX = loc.getBlockX();
        int eyeY = (int) Math.floor(loc.getY() + 1.5);
        int blockZ = loc.getBlockZ();

        int solidAbove = 0;
        int maxY = Math.min(world.getMaxHeight() - 1, eyeY + ceilingCheckHeight);
        for (int y = eyeY; y <= maxY; y++) {
            Block b = world.getBlockAt(blockX, y, blockZ);
            if (b.getType().isSolid() && b.getType().isOccluding()) {
                solidAbove++;
            }
        }

        Block eyeBlock = world.getBlockAt(blockX, eyeY, blockZ);
        byte skyLight = eyeBlock.getLightFromSky();

        boolean hasCeiling = solidAbove >= 2;
        boolean lowSkyLight = skyLight <= 3;
        boolean belowThreshold = loc.getY() < undergroundYThreshold;

        return hasCeiling && (lowSkyLight || belowThreshold);
    }

    public boolean isCameraBlocked(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int feetY = loc.getBlockY();
        int eyeY = (int) Math.floor(loc.getY() + 1.6);

        Material feetMat = world.getBlockAt(x, feetY, z).getType();
        Material eyeMat = world.getBlockAt(x, eyeY, z).getType();

        return (feetMat.isSolid() && feetMat.isOccluding()) || (eyeMat.isSolid() && eyeMat.isOccluding());
    }

    private boolean shouldIgnore(Player player) {
        if (player.hasPermission("javrockantifreecam.bypass")) {
            return true;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE;
    }
}
