package com.flummidill.simplehomes;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.PrintWriter;
import java.io.StringWriter;


public class SimpleHomes extends JavaPlugin {

    private HomeManager manager;
    private JoinListener joinListener;


    @Override
    public void onEnable() {
        getLogger().info("~ Created by Flummidill ~");

        // Initialize Home-Manager
        getLogger().info("Initializing Home-Manager...");
        manager = new HomeManager(this);

        // Initialize Event Listeners
        getLogger().info("Initializing Event Listeners...");
        initializeEventListeners();

        // Load Configuration
        getLogger().info("Loading Configuration...");
        loadConfig();

        // Register Commands
        getLogger().info("Registering Commands...");
        registerCommands();

        // Check for Updates
        getLogger().info("Checking for Updates...");
        checkForUpdates();
    }


    public void initializeEventListeners() {
        joinListener = new JoinListener(this, this.manager);
        getServer().getPluginManager().registerEvents(joinListener, this);
    }

    private void loadConfig() {
        int maxHomes = getConfig().getInt("max-homes", 3);
        boolean adminTeleportDelay = getConfig().getBoolean("admin-tp-delay", true);
        String configVersion = getConfig().getString("config-version", "1.0.0");
        String currentVersion = getDescription().getVersion();

        saveResource("config.yml", true);
        reloadConfig();
        FileConfiguration config = getConfig();

        if (maxHomes < 1 || maxHomes > 50) {
            getLogger().warning("Configuration Error: \"max-homes\" was configured incorrectly and reset to \"3\".");
            maxHomes = 3;
        }
        config.set("max-homes", maxHomes);

        if (!(adminTeleportDelay == true || adminTeleportDelay == false)) {
            getLogger().warning("Configuration Error: \"admin-tp-delay\" was configured incorrectly and reset to \"true\".");
            adminTeleportDelay = true;
        }
        manager.adminTeleportDelay = adminTeleportDelay;
        config.set("admin-tp-delay", adminTeleportDelay);


        if ("1.0.0".equals(configVersion)) {
            getLogger().info("Configuration Update: \"config-version\" has been updated to \"" + currentVersion + "\".");
            manager.updatePlugin();
            configVersion = currentVersion;
        } else if (isNewerVersion(configVersion, "1.0.0")) {
            if (isOlderVersion(configVersion, currentVersion)) {
                getLogger().info("Configuration Update: \"config-version\" has been updated to \"" + currentVersion + "\".");
                configVersion = currentVersion;
            }
        } else {
            getLogger().warning("Configuration Error: \"config-version\" was configured incorrectly and reset to \"" + currentVersion + "\".");
            configVersion = currentVersion;
        }
        config.set("config-version", configVersion);

        saveConfig();
    }

    private void registerCommands() {
        CommandHandler commandHandler = new CommandHandler(this, this.manager);
        TabCompleter tabCompleter = new TabCompleter(this, this.manager);

        getCommand("sethome").setExecutor(commandHandler);
        getCommand("home").setExecutor(commandHandler);
        getCommand("delhome").setExecutor(commandHandler);
        getCommand("homeadmin").setExecutor(commandHandler);

        getCommand("sethome").setTabCompleter(tabCompleter);
        getCommand("home").setTabCompleter(tabCompleter);
        getCommand("delhome").setTabCompleter(tabCompleter);
        getCommand("homeadmin").setTabCompleter(tabCompleter);
    }

    private void checkForUpdates() {
        String[] latestVersion = getLatestVersion().split("\\|", 2);
        String currentVersion = getDescription().getVersion();

        if (!"error".equals(latestVersion[0])) {
            if (isNewerVersion(latestVersion[0], currentVersion)) {
                getLogger().warning("A new Version of SimpleHomes is available: " + latestVersion[0]);
                joinListener.setUpdateAvailable(true);
            } else {
                getLogger().info("No new Updates available.");
            }
        } else {
            getLogger().warning("Failed to Check for Updates!\n" + latestVersion[1]);
        }
    }

    public String getLatestVersion() {
        String apiUrl = "https://api.modrinth.com/v2/project/simple_homes/version";

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONArray jsonArray = new JSONArray(response.body());
                    if (!jsonArray.isEmpty()) {
                        JSONObject latestVersion = jsonArray.getJSONObject(0);
                        return latestVersion.getString("version_number");
                    } else {
                        return "error|No Version Data Found: Project has no Versions on Modrinth";
                    }
                } else {
                    return "error|No Version Data Found: Failed to Connect to Modrinth API";
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Failed to check for Updates!");

                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));
                return "error|" + stackTrace;
            }
        }
    }

    public boolean isNewerVersion(String comparingVersion, String referenceVersion) {
        String[] comparingVersionParts = comparingVersion.split("\\.");
        String[] referenceVersionParts = referenceVersion.split("\\.");

        for (int i = 0; i < 3; i++) {
            int comparingVersionPart = i < comparingVersionParts.length ? Integer.parseInt(comparingVersionParts[i]) : 0;
            int referenceVersionPart = i < referenceVersionParts.length ? Integer.parseInt(referenceVersionParts[i]) : 0;

            if (comparingVersionPart > referenceVersionPart) {
                return true;
            } else if (comparingVersionPart < referenceVersionPart) {
                return false;
            }
        }

        return false;
    }

    public boolean isOlderVersion(String comparingVersion, String referenceVersion) {
        String[] comparingVersionParts = comparingVersion.split("\\.");
        String[] referenceVersionParts = referenceVersion.split("\\.");

        for (int i = 0; i < 3; i++) {
            int comparingVersionPart = i < comparingVersionParts.length ? Integer.parseInt(comparingVersionParts[i]) : 0;
            int referenceVersionPart = i < referenceVersionParts.length ? Integer.parseInt(referenceVersionParts[i]) : 0;

            if (comparingVersionPart < referenceVersionPart) {
                return true;
            } else if (comparingVersionPart > referenceVersionPart) {
                return false;
            }
        }

        return false;
    }
}