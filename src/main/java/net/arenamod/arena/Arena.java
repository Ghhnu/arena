package net.arenamod.arena;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa una arena creada con /arena x y z.
 * Guarda toda la información necesaria para saber en qué fase va,
 * qué mobs siguen vivos y dónde está la puerta de la sala de espera.
 */
public class Arena {

    public String name;
    public String dimensionId;   // p.ej. "minecraft:overworld"

    // Centro de la arena (a nivel de suelo, el punto que dio el jugador)
    public int x, y, z;
    public int radius = 16;
    public int wallHeight = 8;

    // Estado
    public boolean open = false;
    public int phase = 0;           // 0 = recién creada, 1-4 = fase activa, 5 = completada
    public int subStage = 0;        // sub-etapa dentro de una fase (usado en fase 3 y 4)
    public boolean waitingChest = false; // true mientras hay un cofre esperando a expirar

    // Puerta de la sala de espera (bloques de barrotes de hierro que se alternan con aire)
    public List<BlockPos> gateBlocks = new ArrayList<>();

    // Mobs vivos de la oleada actual
    public transient List<UUID> activeMobs = new ArrayList<>();

    // Cofre de recompensa actualmente en el suelo
    public BlockPos chestPos;
    public long chestExpireTick = -1;

    // Punto de aparición de los mobs (centro de la arena, a la altura del suelo)
    public BlockPos spawnPoint() {
        return new BlockPos(x, y, z);
    }

    public String key() {
        return name;
    }
}
