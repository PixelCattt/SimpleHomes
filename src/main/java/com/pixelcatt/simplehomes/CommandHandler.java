package com.pixelcatt.simplehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;


public class CommandHandler implements CommandExecutor {

    private final SimpleHomes plugin;
    private final HomeManager manager;

    private final Map<UUID, TeleportTask> teleportTasks = new HashMap<>();
    private final Map<UUID, TeleportTaskAdmin> teleportTasksAdmin = new HashMap<>();


    public CommandHandler(SimpleHomes plugin, HomeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly Players can run this Command!");
            return true;
        }

        Player player = (Player) sender;

        String cmd = command.getName().toLowerCase();

        manager.saveOfflinePlayer(player.getUniqueId(), player.getName());

        switch (cmd) {
            case "sethome":
                if (!player.hasPermission("simplehomes.sethome")) {
                    player.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                if (args.length != 1) {
                    player.sendMessage("Usage: /sethome <number>");
                    return true;
                }
                return handleSetHome(player, args[0]);

            case "home":
                if (!player.hasPermission("simplehomes.home")) {
                    player.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                if (args.length != 1) {
                    player.sendMessage("Usage: /home <number>");
                    return true;
                }
                return handleHome(player, args[0]);

            case "delhome":
                if (!player.hasPermission("simplehomes.delhome")) {
                    player.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                if (args.length != 1) {
                    player.sendMessage("Usage: /delhome <number>");
                    return true;
                }
                return handleDelHome(player, args[0]);

            case "homeadmin":
                if (!player.hasPermission("simplehomes.homeadmin.show")) {
                    player.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                if (args.length != 3) {
                    player.sendMessage("Usage: /homeadmin <sethome|home|info|delhome|maxhomes> <player> <number>");
                    return true;
                }
                return handleHomeAdmin(player, args);

            default:
                return true;
        }
    }

    private boolean handleSetHome(Player player, String arg) {
        UUID uuid = player.getUniqueId();
        int maxHomes = manager.getMaxHomes(uuid);
        int homeNum;

        try {
            homeNum = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            player.sendMessage("§cHome number must be a valid integer.");
            return false;
        }

        if (!player.hasPermission("simplehomes.bypasshomelimit")) {
            if (homeNum < 1 || homeNum > maxHomes) {
                player.sendMessage("§cYou can only set homes from 1 to " + maxHomes + ".");
                return false;
            }
        }

        if (manager.homeExists(uuid, homeNum)) {
            player.sendMessage("§cHome " + homeNum + " already exists.");
            return false;
        }

        manager.setHome(uuid, homeNum, player.getLocation());
        player.sendMessage("§aHome " + homeNum + " set!");

        return true;
    }

    private boolean handleHome(Player player, String arg) {
        UUID uuid = player.getUniqueId();
        int maxHomes = manager.getMaxHomes(uuid);
        int homeNum;

        try {
            homeNum = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            player.sendMessage("§cHome number must be a valid integer.");
            return false;
        }

        if (!player.hasPermission("simplehomes.bypasshomelimit")) {
            if (homeNum < 1 || homeNum > maxHomes) {
                player.sendMessage("§cYou can only use homes from 1 to " + maxHomes + ".");
                return false;
            }
        }

        if (!manager.homeExists(uuid, homeNum)) {
            player.sendMessage("§cHome " + homeNum + " does not exist.");
            return false;
        }

        // Cancel any ongoing Teleport Tasks for Player
        cancelTeleport(player);

        TeleportTask task = new TeleportTask(plugin, player, manager.getHome(uuid, homeNum), 5, homeNum);
        task.start();

        teleportTasks.put(player.getUniqueId(), task);

        return true;
    }

    private boolean handleDelHome(Player player, String arg) {
        UUID uuid = player.getUniqueId();
        int maxHomes = manager.getMaxHomes(uuid);
        int homeNum;

        if (arg.equals("*")) {
            manager.deleteAllHomes(uuid);
            player.sendMessage("§aAll your Homes have been deleted!");
        } else {
            try {
                homeNum = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                player.sendMessage("§cHome must be a Number.");
                return false;
            }

            if (!player.hasPermission("simplehomes.bypasshomelimit")) {
                if (homeNum < 1 || homeNum > maxHomes) {
                    player.sendMessage("§cYou can only delete homes from 1 to " + maxHomes + ".");
                    return false;
                }
            }

            if (!manager.homeExists(uuid, homeNum)) {
                player.sendMessage("§cHome " + homeNum + " does not exist.");
                return false;
            }

            manager.deleteHome(uuid, homeNum);
            player.sendMessage("§aHome " + homeNum + " deleted!");
        }

        return true;
    }

    private boolean handleHomeAdmin(Player sender, String[] args) {
        String action = args[0].toLowerCase();
        String targetName = args[1];
        String numberStr = args[2];

        switch (action) {
            case "sethome":
                if (!sender.hasPermission("simplehomes.homeadmin.sethome")) {
                    sender.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                break;

            case "home":
                if (!sender.hasPermission("simplehomes.homeadmin.home")) {
                    sender.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                break;

            case "info":
                if (!sender.hasPermission("simplehomes.homeadmin.info")) {
                    sender.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                break;

            case "delhome":
                if (!sender.hasPermission("simplehomes.homeadmin.delhome")) {
                    sender.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                break;

            case "maxhomes":
                if (!sender.hasPermission("simplehomes.homeadmin.maxhomes")) {
                    sender.sendMessage("§cYou don’t have permission to use this command.");
                    return true;
                }
                break;

            default:
                sender.sendMessage("§cUsage: /homeadmin <sethome|home|info|delhome|maxhomes> <player> <number>");
                return false;
        }

        UUID targetUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        if (targetName.equals("*")) {
            if (!action.equals("delhome")) {
                sender.sendMessage("§cYou can only use Player * with the delhome Command.");
                return false;
            }
        } else {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                targetUUID = target.getUniqueId();
            } else if (manager.getOfflinePlayerUUID(targetName) != null) {
                targetUUID = manager.getOfflinePlayerUUID(targetName);
            } else {
                sender.sendMessage("§cPlayer '" + targetName + "' not found.");
                return false;
            }

            targetName = manager.getOfflinePlayerName(targetUUID);
        }

        int number = -1;
        if (numberStr.equals("*")) {
            if (!action.equals("delhome")) {
                sender.sendMessage("§cYou can only use Number * with the delhome Command.");
                return false;
            }
        } else {
            try {
                number = Integer.parseInt(numberStr);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cHome must be a Number.");
                return false;
            }
        }

        if (targetUUID.equals(UUID.fromString("00000000-0000-0000-0000-000000000000")) && !action.equals("delhome")) {
            sender.sendMessage("§cYou can only use Player * with the delhome Command.");
            return false;
        }

        if (number == -1 && !action.equals("delhome")) {
            sender.sendMessage("§cYou can only use Number * with the delhome Command.");
            return false;
        }

        switch (action) {
            case "sethome":
                if (manager.homeExists(targetUUID, number)) {
                    sender.sendMessage("§cHome " + number + " already exists for " + targetName + ".");
                    return false;
                } else {
                    manager.setHome(targetUUID, number, sender.getLocation());
                    sender.sendMessage("§aSet Home " + number + " of " + targetName + ".");
                }
                break;

            case "home":
                Location loc = manager.getHome(targetUUID, number);
                if (loc == null) {
                    sender.sendMessage("§cHome " + number + " does not exist for " + targetName + ".");
                    return false;
                }
                
                // Cancel any ongoing Teleport Tasks for Player
                cancelTeleport(sender);
                TeleportTaskAdmin task = new TeleportTaskAdmin(plugin, manager, sender, loc, 5, number, targetName);
                task.start();
                teleportTasksAdmin.put(sender.getUniqueId(), task);
                break;

            case "info":
                Location homeLoc = manager.getHome(targetUUID, number);
                if (homeLoc == null) {
                    sender.sendMessage("§cHome " + number + " does not exist for " + targetName + ".");
                    return false;
                }

                String worldName;
                switch (homeLoc.getWorld().getName()) {
                    case "world":
                        worldName = "The Overworld";
                        break;
                    case "world_nether":
                        worldName = "The Nether";
                        break;
                    case "world_the_end":
                        worldName = "The End";
                        break;
                    default:
                        worldName = homeLoc.getWorld().getName();
                        break;
                }

                sender.sendMessage("§aInformation for Home " + number + " of " + targetName + ":" +
                        "\n§9World: §c" + worldName +
                        "\n§9X: §c" + Math.floor(homeLoc.getX()) +
                        "\n§9Y: §c" + Math.floor(homeLoc.getY()) +
                        "\n§9Z: §c" + Math.floor(homeLoc.getZ())
                );
                break;

            case "delhome":
                if (targetName.equals("*")) {
                    if (numberStr.equals("*")) {
                        manager.deleteEveryHome();
                        sender.sendMessage("§aDeleted all Homes of all Players.");
                    } else {
                        manager.deleteAllHomesWithNumber(number);
                        sender.sendMessage("§aDeleted all Homes with Number " + number + ".");
                    }
                    break;
                }

                if (numberStr.equals("*")) {
                    manager.deleteAllHomes(targetUUID);
                    sender.sendMessage("§aDeleted all Homes of " + targetName + ".");
                    break;
                }

                if (!manager.homeExists(targetUUID, number)) {
                    sender.sendMessage("§cHome " + number + " does not exist for " + targetName + ".");
                    return false;
                }
                manager.deleteHome(targetUUID, number);
                sender.sendMessage("§aDeleted Home " + number + " of " + targetName + ".");
                break;

            case "maxhomes":
                if (number < 1 || number > 50) {
                    sender.sendMessage("§cNumber must be between 1 and 50.");
                    return false;
                }
                manager.setMaxHomes(targetUUID, number);
                sender.sendMessage("§aSet max homes for " + targetName + " to " + number + ".");
                break;

            default:
                sender.sendMessage("§cUsage: /homeadmin <sethome|home|info|delhome|maxhomes> <player> <number>");
                return false;
        }

        return true;
    }

    private void cancelTeleport(Player player) {
        TeleportTask task = teleportTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        TeleportTaskAdmin taskAdmin = teleportTasksAdmin.remove(player.getUniqueId());
        if (taskAdmin != null) {
            taskAdmin.cancel();
        }
    }
}