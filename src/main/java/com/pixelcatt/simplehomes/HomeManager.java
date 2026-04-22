package com.pixelcatt.simplehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.io.File;
import java.sql.*;
import java.util.*;


public class HomeManager {

    private final SimpleHomes plugin;

    private final File dbFile;
    private Connection connection;

    public boolean adminTeleportDelay = false;


    public HomeManager(SimpleHomes plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "homes.db");
        openConnection();
        createTables();
    }


    private void openConnection() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to SQLite database!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Homes Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS homes (" +
                    "uuid TEXT NOT NULL," +
                    "home_num INTEGER NOT NULL," +
                    "world TEXT NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "pitch DOUBLE NOT NULL," +
                    "yaw DOUBLE NOT NULL," +
                    "PRIMARY KEY(uuid, home_num))");

            // Home-Limits Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS home_limits (" +
                    "uuid TEXT NOT NULL," +
                    "home_limit INTEGER NOT NULL," +
                    "PRIMARY KEY(uuid))");

            // Offline-Players Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS offline_players (" +
                    "uuid TEXT NOT NULL," +
                    "plr_name TEXT NOT NULL UNIQUE," +
                    "PRIMARY KEY(uuid))");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables in SQLite database.");
            e.printStackTrace();
        }
    }

    public void updatePlugin() {
        try (Statement stmt = connection.createStatement()) {
            plugin.getLogger().info("Updating DB-Table offline_players to Version 1.1.0");

            // Delete Old Table
            stmt.executeUpdate("DROP TABLE IF EXISTS offline_players");

            // Create New Table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS offline_players (" +
                "uuid TEXT NOT NULL," +
                "plr_name TEXT NOT NULL UNIQUE," +
                "PRIMARY KEY(uuid))");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables in SQLite database.");
            e.printStackTrace();
        }
    }

    public int getMaxHomes(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT home_limit FROM home_limits WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int lim = rs.getInt("home_limit");
                return Math.min(Math.max(lim, 1), 50);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return plugin.getConfig().getInt("max-homes", 3);
    }

    public void setMaxHomes(UUID uuid, int limit) {
        limit = Math.min(Math.max(limit, 1), 50);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO home_limits(uuid, home_limit) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET home_limit=excluded.home_limit")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean homeExists(UUID uuid, int homeNum) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM homes WHERE uuid = ? AND home_num = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, homeNum);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Set<Integer> getHomeNumbers(UUID uuid) {
        Set<Integer> homes = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT home_num FROM homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                homes.add(rs.getInt("home_num"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }

    public void setHome(UUID uuid, int homeNum, Location loc) {
        Player player = Bukkit.getPlayer(uuid);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO homes(uuid, home_num, world, x, y, z, pitch, yaw) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid, home_num) DO NOTHING")) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, homeNum);
            if (!(loc.getWorld() == null)) {
                ps.setString(3, loc.getWorld().getName());
            } else {
                if (!(player == null)) {
                    player.sendMessage("§cFailed to set Home!");
                }
                return;
            }

            // Center of Block
            ps.setDouble(4, Math.floor(loc.getX()) + 0.5);
            ps.setDouble(5, Math.floor(loc.getY()) + 0.5);
            ps.setDouble(6, Math.floor(loc.getZ()) + 0.5);

            double pitch = 0f;
            double yaw = 0f;

            if (loc.getYaw() >= 135 && loc.getYaw() <= -135) {
                // NORTH
                yaw = 180f;
            } else if (loc.getYaw() < 135 && loc.getYaw() > 45) {
                // WEST
                yaw = 90f;
            } else if (loc.getYaw() <= 45 && loc.getYaw() >= -45) {
                // SOUTH
                yaw = 0f;
            } else if (loc.getYaw() < -45 && loc.getYaw() > -135) {
                // EAST
                yaw = -90f;
            }

            ps.setDouble(7, pitch);
            ps.setDouble(8, yaw);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteHome(UUID uuid, int homeNum) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes WHERE uuid = ? AND home_num = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, homeNum);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllHomes(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllHomesWithNumber(int homeNum) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes WHERE home_num = ?")) {
            ps.setInt(1, homeNum);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEveryHome() {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getHome(UUID uuid, int homeNum) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM homes WHERE uuid = ? AND home_num = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, homeNum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world,
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        (float) rs.getDouble("yaw"),
                        (float) rs.getDouble("pitch"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveOfflinePlayer(UUID uuid, String playerName) {
        try {
            // Check If UUID already exists
            boolean uuidExists = false;
            PreparedStatement ps1 = connection.prepareStatement(
                "SELECT 1 FROM offline_players WHERE uuid = ? LIMIT 1");
            ps1.setString(1, uuid.toString());
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                uuidExists = true;
            }

            // Check If the Player Name already exists
            boolean nameExists = false;
            PreparedStatement ps2 = connection.prepareStatement(
                "SELECT 1 FROM offline_players WHERE plr_name = ? LIMIT 1");
            ps2.setString(1, playerName);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                nameExists = true;
            }


            // Delete Row with UUID if uuidExists = true
            if (uuidExists) {
                PreparedStatement ps3 = connection.prepareStatement(
                    "DELETE FROM offline_players WHERE uuid = ?");
                ps3.setString(1, uuid.toString());
                ps3.executeUpdate();
            }

            // Delete Row with Name if nameExists = true
            if (nameExists) {
                PreparedStatement ps4 = connection.prepareStatement(
                    "DELETE FROM offline_players WHERE plr_name = ?");
                ps4.setString(1, playerName);
                ps4.executeUpdate();
            }


            // Insert new UUID and Name
            PreparedStatement ps5 = connection.prepareStatement(
                    "REPLACE INTO offline_players(uuid, plr_name) VALUES (?, ?)");
            ps5.setString(1, uuid.toString());
            ps5.setString(2, playerName);
            ps5.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getOfflinePlayerNameList() {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT DISTINCT plr_name FROM offline_players")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("plr_name");
                if (name != null) {
                    list.add(name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public UUID getOfflinePlayerUUID(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM offline_players WHERE plr_name = ?")) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String uuid = rs.getString("uuid");
                if (uuid != null) {
                    return UUID.fromString(uuid);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getOfflinePlayerName(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM offline_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("plr_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setAdminTeleportDelay (boolean value) {
        this.adminTeleportDelay = value;
    }
}