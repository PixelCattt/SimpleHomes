package com.pixelcatt.simplehomes;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;


public class TabCompleter implements org.bukkit.command.TabCompleter {

    private final SimpleHomes plugin;
    private final HomeManager manager;


    public TabCompleter(SimpleHomes plugin, HomeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player)) return Collections.emptyList();

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "sethome":
                return autocompleteUnsetHomes(uuid, args);

            case "home", "delhome":
                return autocompleteExistingHomes(uuid, args);

            case "homeadmin":
                return autocompleteHomeAdmin((Player) sender, args);

            default:
                return Collections.emptyList();
        }
    }

    private List<String> autocompleteUnsetHomes(UUID uuid, String[] args) {
        if (args.length == 1) {
            int maxHomes = manager.getMaxHomes(uuid);
            Set<Integer> existing = manager.getHomeNumbers(uuid);

            List<String> possible = new ArrayList<>();
            for (int i = 1; i <= Math.min(maxHomes, 50); i++) {
                if (!existing.contains(i)) {
                    possible.add(String.valueOf(i));
                }
            }
            return filterByPrefix(possible, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> autocompleteExistingHomes(UUID uuid, String[] args) {
        if (args.length == 1) {
            int maxHomes = manager.getMaxHomes(uuid);
            Set<Integer> existing = manager.getHomeNumbers(uuid);

            List<String> possible = existing.stream()
                    .filter(n -> n >= 1 && n <= Math.min(maxHomes, 50))
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.toList());

            return filterByPrefix(possible, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> autocompleteHomeAdmin(Player sender, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();

            if (sender.hasPermission("simplehomes.homeadmin.sethome")) subcommands.add("sethome");
            if (sender.hasPermission("simplehomes.homeadmin.home")) subcommands.add("home");
            if (sender.hasPermission("simplehomes.homeadmin.info")) subcommands.add("info");
            if (sender.hasPermission("simplehomes.homeadmin.delhome")) subcommands.add("delhome");
            if (sender.hasPermission("simplehomes.homeadmin.maxhomes")) subcommands.add("maxhomes");

            return filterByPrefix(subcommands, args[0]);
        }
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();

            if (!sender.hasPermission("simplehomes.homeadmin." + subcommand)) {
                return Collections.emptyList();
            }

            List<String> playerNames = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (!playerNames.contains(name)) {
                    playerNames.add(name);
                }
            }

            List<String> offlinePlayerNames = manager.getOfflinePlayerNameList();
            for (String name : offlinePlayerNames) {
                if (!playerNames.contains(name)) {
                    playerNames.add(name);
                }
            }

            return filterByPrefix(playerNames, prefix);
        }
        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            String targetName = args[1];
            Player target = Bukkit.getPlayerExact(targetName);
            UUID targetUUID;

            if (!sender.hasPermission("simplehomes.homeadmin." + subcommand)) {
                return Collections.emptyList();
            }

            if (target != null) {
                targetUUID = target.getUniqueId();
            } else if (manager.getOfflinePlayerUUID(targetName) != null) {
                targetUUID = manager.getOfflinePlayerUUID(targetName);
            } else {
                return Collections.emptyList();
            }

            if (subcommand.equals("maxhomes")) {
                int maxHomes = manager.getMaxHomes(targetUUID);
                return Collections.singletonList(String.valueOf(maxHomes));
            } else if (Arrays.asList("sethome", "home", "info", "delhome").contains(subcommand)) {
                int maxHomes = manager.getMaxHomes(targetUUID);
                Set<Integer> existing = manager.getHomeNumbers(targetUUID);
                List<String> possible = new ArrayList<>();

                if (subcommand.equals("sethome")) {
                    for (int i = 1; i <= Math.min(maxHomes, 50); i++) {
                        if (!existing.contains(i)) {
                            possible.add(String.valueOf(i));
                        }
                    }
                } else {
                    possible = existing.stream()
                            .filter(n -> n >= 1 && n <= Math.min(maxHomes, 50))
                            .map(String::valueOf)
                            .sorted()
                            .collect(Collectors.toList());
                }

                return filterByPrefix(possible, args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterByPrefix(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) return options;
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .limit(50)
                .collect(Collectors.toList());
    }
}