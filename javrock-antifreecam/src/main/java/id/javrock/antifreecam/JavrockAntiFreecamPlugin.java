package id.javrock.antifreecam;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class JavrockAntiFreecamPlugin extends JavaPlugin {

    private FreecamMonitorService monitorService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.monitorService = new FreecamMonitorService(this);
        this.monitorService.start();

        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this, monitorService), this);

        PluginCommand command = getCommand("javrockantifreecam");
        if (command != null) {
            AntiFreecamCommand executor = new AntiFreecamCommand(this, monitorService);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("JavrockAntiFreecam aktif untuk Paper 1.21.11.");
    }

    @Override
    public void onDisable() {
        if (monitorService != null) {
            monitorService.stop();
        }
        getLogger().info("JavrockAntiFreecam dinonaktifkan.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (monitorService != null) {
            monitorService.reloadSettings();
        }
    }
}
