package id.javrock.antifreecam;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AntiFreecamCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavrockAntiFreecamPlugin plugin;
    private final FreecamMonitorService monitorService;

    public AntiFreecamCommand(JavrockAntiFreecamPlugin plugin, FreecamMonitorService monitorService) {
        this.plugin = plugin;
        this.monitorService = monitorService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("javrockantifreecam.admin")) {
            sender.sendMessage(LEGACY.deserialize("&cᴋᴀᴍᴜ ᴛɪᴅᴀᴋ ᴍᴇᴍɪʟɪᴋɪ ɪᴢɪɴ ᴜɴᴛᴜᴋ ᴍᴇɴɢɢᴜɴᴀᴋᴀɴ ᴘᴇʀɪɴᴛᴀʜ ɪɴɪ."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            String prefix = plugin.getConfig().getString("messages.prefix", "&9&lᴊᴀᴠʀᴏᴄᴋ &8» ");
            String reloaded = plugin.getConfig().getString("messages.reloaded", "&bᴋᴏɴꜰɪɢᴜʀᴀꜱɪ ʙᴇʀʜᴀꜱɪʟ ᴅɪᴍᴜᴀᴛ ᴜʟᴀɴɢ.");
            sender.sendMessage(LEGACY.deserialize(prefix + reloaded));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("status") && sender instanceof Player player) {
            boolean enclosed = monitorService.isEnclosedOrInCave(player.getLocation());
            boolean blocked = monitorService.isCameraBlocked(player.getLocation());
            sender.sendMessage(LEGACY.deserialize(
                    "&9&lᴊᴀᴠʀᴏᴄᴋ &8» &fꜱᴛᴀᴛᴜꜱ ʟᴏᴋᴀꜱɪ: &bᴄᴀᴠᴇ/ᴛᴇʀᴛᴜᴛᴜᴘ=" + enclosed + " &8| &bʙʟᴏᴋ=" + blocked
            ));
            return true;
        }

        sender.sendMessage(LEGACY.deserialize("&9&lᴊᴀᴠʀᴏᴄᴋ &8» &fɢᴜɴᴀᴋᴀɴ: &3/" + label + " reload &8| &3/" + label + " status"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("javrockantifreecam.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "status").stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
