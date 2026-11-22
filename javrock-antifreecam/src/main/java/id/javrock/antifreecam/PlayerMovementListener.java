package id.javrock.antifreecam;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerMovementListener implements Listener {

    private final JavrockAntiFreecamPlugin plugin;
    private final FreecamMonitorService monitorService;

    public PlayerMovementListener(JavrockAntiFreecamPlugin plugin, FreecamMonitorService monitorService) {
        this.plugin = plugin;
        this.monitorService = monitorService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        monitorService.recordJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        monitorService.removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().distanceSquared(event.getTo()) < 0.0001) {
            return;
        }
        monitorService.handleMove(player, event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                || event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            monitorService.recordJoin(event.getPlayer());
        }
    }
}
