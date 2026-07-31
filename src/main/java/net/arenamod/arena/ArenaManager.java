package net.arenamod.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda todas las arenas creadas en este mundo. Se persisten en un
 * archivo JSON dentro de la carpeta de configuración del servidor,
 * así que sobreviven a un reinicio (el estado exacto de la oleada en
 * curso no se recupera, pero la estructura, la puerta y la fase sí).
 */
public class ArenaManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Map<String, Arena> ARENAS = new LinkedHashMap<>();
    private static int counter = 0;

    private static Path saveFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("arenamod").resolve("arenas.json");
    }

    public static Arena create(ServerWorld world, BlockPos pos) {
        counter++;
        Arena arena = new Arena();
        arena.name = "arena" + counter;
        arena.dimensionId = world.getRegistryKey().getValue().toString();
        arena.x = pos.getX();
        arena.y = pos.getY();
        arena.z = pos.getZ();
        ARENAS.put(arena.name, arena);
        save();
        return arena;
    }

    /** Busca la arena más cercana a una posición dentro de la misma dimensión. */
    public static Optional<Arena> findNearest(ServerWorld world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        Arena best = null;
        double bestDist = Double.MAX_VALUE;
        for (Arena arena : ARENAS.values()) {
            if (!arena.dimensionId.equals(dim)) continue;
            double dx = arena.x - pos.getX();
            double dy = arena.y - pos.getY();
            double dz = arena.z - pos.getZ();
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = arena;
            }
        }
        return Optional.ofNullable(best);
    }

    public static void save() {
        try {
            Path file = saveFile();
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(ARENAS, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(MinecraftServer server) {
        try {
            Path file = saveFile();
            if (!Files.exists(file)) return;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Arena> loaded = GSON.fromJson(reader, ARENAS.getClass());
                ARENAS.clear();
                if (loaded != null) {
                    ARENAS.putAll(loaded);
                    for (Arena arena : ARENAS.values()) {
                        if (arena.activeMobs == null) arena.activeMobs = new java.util.ArrayList<>();
                        String num = arena.name.replace("arena", "");
                        try {
                            int n = Integer.parseInt(num);
                            if (n > counter) counter = n;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
