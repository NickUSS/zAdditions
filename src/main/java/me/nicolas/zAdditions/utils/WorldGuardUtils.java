package me.nicolas.zAdditions.utils;

import me.nicolas.zAdditions.ZAdditions;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.logging.Level;

/**
 * Utilidades para la integración con WorldGuard
 * Compatible con WorldGuard 7.0.13 y WorldEdit 7.3.10
 */
public class WorldGuardUtils {
    private static boolean initialized = false;
    private static boolean worldGuardEnabled = false;

    // Objetos para acceso mediante reflexión
    private static Object worldGuardPlugin;
    private static Object worldGuard;
    private static Object regionContainer;

    // Clases de WorldGuard
    private static Class<?> worldGuardPluginClass;
    private static Class<?> worldGuardClass;
    private static Class<?> localPlayerClass;
    private static Class<?> regionContainerClass;
    private static Class<?> regionManagerClass;
    private static Class<?> applicableRegionSetClass;
    private static Class<?> vectorClass;
    private static Class<?> protectedRegionClass;

    // Clases de WorldEdit Adapter
    private static Class<?> bukitAdapterClass;

    /**
     * Inicializa la integración con WorldGuard
     * @return true si WorldGuard está disponible y habilitado
     */
    public static boolean initialize() {
        if (initialized) return worldGuardEnabled;

        initialized = true;

        try {
            // Intentar obtener el plugin WorldGuard
            Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
            if (plugin == null) {
                Bukkit.getLogger().info("[ZAdditions] WorldGuard no encontrado.");
                return false;
            }

            // Cargar clases necesarias
            worldGuardPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            localPlayerClass = Class.forName("com.sk89q.worldguard.LocalPlayer");
            regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
            regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
            applicableRegionSetClass = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            bukitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");

            // Obtener instancias
            worldGuardPlugin = worldGuardPluginClass.cast(plugin);
            worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);

            // Obtener el contenedor de regiones
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
            regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            worldGuardEnabled = true;
            Bukkit.getLogger().info("[ZAdditions] WorldGuard " + plugin.getDescription().getVersion() + " integrado correctamente.");
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ZAdditions] Error al inicializar WorldGuard: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si WorldGuard está habilitado
     * @return true si está habilitado
     */
    public static boolean isEnabled() {
        return worldGuardEnabled;
    }

    /**
     * Comprueba si un bloque está dentro de alguna región protegida
     * @param block El bloque a comprobar
     * @return true si está en una región protegida
     */
    public static boolean isInProtectedRegion(Block block) {
        if (!worldGuardEnabled) return false;

        try {
            // Convertir Bukkit World a WorldEdit World
            Object worldEditWorld = bukitAdapterClass.getMethod("adapt", World.class)
                    .invoke(null, block.getWorld());

            // Obtener el manejador de regiones para este mundo
            Object regionManager = regionContainerClass.getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
                    .invoke(regionContainer, worldEditWorld);

            if (regionManager == null) return false;

            // Convertir la ubicación del bloque a vector
            Object vector = vectorClass.getMethod("at", double.class, double.class, double.class)
                    .invoke(null, block.getX(), block.getY(), block.getZ());

            // Obtener regiones aplicables
            Object applicableRegions = regionManagerClass.getMethod("getApplicableRegions", vectorClass)
                    .invoke(regionManager, vector);

            // Verificar si hay regiones usando getRegions() en lugar de isEmpty()
            Object regions = applicableRegionSetClass.getMethod("getRegions").invoke(applicableRegions);

            // Verificar si la colección está vacía
            if (regions instanceof Collection) {
                return !((Collection<?>) regions).isEmpty();
            }
            return false;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ZAdditions] Error al comprobar región protegida: " + e.getMessage());
            return true; // En caso de error, considerar protegido por seguridad
        }
    }

    /**
     * Comprueba si un jugador es dueño de la región en una ubicación
     * Este es el método principal para la integración con ChunkMiners
     * @param player El jugador
     * @param location La ubicación
     * @return true si es dueño o si no hay región
     */
    public static boolean isOwner(Player player, Location location) {
        if (!worldGuardEnabled) return true;

        // Si el jugador tiene permiso para saltarse las verificaciones
        if (player.hasPermission("zadditions.chunkminer.bypass.worldguard")) {
            return true;
        }

        try {
            // Convertir Bukkit World a WorldEdit World
            Object worldEditWorld = bukitAdapterClass.getMethod("adapt", World.class)
                    .invoke(null, location.getWorld());

            // Obtener el manejador de regiones para este mundo
            Object regionManager = regionContainerClass.getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
                    .invoke(regionContainer, worldEditWorld);

            if (regionManager == null) return true;

            // Convertir la ubicación a vector
            Object vector = vectorClass.getMethod("at", double.class, double.class, double.class)
                    .invoke(null, location.getX(), location.getY(), location.getZ());

            // Obtener regiones aplicables
            Object applicableRegions = regionManagerClass.getMethod("getApplicableRegions", vectorClass)
                    .invoke(regionManager, vector);

            // Obtener colección de regiones
            Object regions = applicableRegionSetClass.getMethod("getRegions").invoke(applicableRegions);

            // Si no hay regiones, se permite
            if (regions instanceof Collection) {
                Collection<?> regionCollection = (Collection<?>) regions;

                if (regionCollection.isEmpty()) {
                    return true;
                }

                // Obtener LocalPlayer
                Object localPlayer = worldGuardPluginClass.getMethod("wrapPlayer", Player.class)
                        .invoke(worldGuardPlugin, player);

                // Verificar si es dueño en todas las regiones
                for (Object region : regionCollection) {
                    // Verificar si el jugador es dueño de la región
                    Boolean isOwner = (Boolean) protectedRegionClass.getMethod("isOwner", localPlayerClass)
                            .invoke(region, localPlayer);

                    if (!isOwner) {
                        return false;
                    }
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[ZAdditions] Error al comprobar propiedad de región: " + e.getMessage(), e);
            return false; // En caso de error, no permitir
        }
    }

    /**
     * Comprueba si un bloque está en región global (sin protección)
     * @param block El bloque a comprobar
     * @return true si el bloque está en región global (sin protección)
     */
    public static boolean isInGlobalRegion(Block block) {
        if (!worldGuardEnabled) return true;

        try {
            // Convertir Bukkit World a WorldEdit World
            Object worldEditWorld = bukitAdapterClass.getMethod("adapt", World.class)
                    .invoke(null, block.getWorld());

            // Obtener el manejador de regiones para este mundo
            Object regionManager = regionContainerClass.getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
                    .invoke(regionContainer, worldEditWorld);

            if (regionManager == null) return true;

            // Convertir la ubicación del bloque a vector
            Object vector = vectorClass.getMethod("at", double.class, double.class, double.class)
                    .invoke(null, block.getX(), block.getY(), block.getZ());

            // Obtener regiones aplicables
            Object applicableRegions = regionManagerClass.getMethod("getApplicableRegions", vectorClass)
                    .invoke(regionManager, vector);

            // Obtener colección de regiones
            Object regions = applicableRegionSetClass.getMethod("getRegions").invoke(applicableRegions);

            // Si es una colección vacía, está en región global
            if (regions instanceof Collection) {
                return ((Collection<?>) regions).isEmpty();
            }

            return true; // Por defecto, consideramos que está en región global
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ZAdditions] Error al comprobar región global: " + e.getMessage());
            return false; // En caso de error, no considerar como región global por seguridad
        }
    }
}