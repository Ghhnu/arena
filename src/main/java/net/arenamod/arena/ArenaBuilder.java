package net.arenamod.arena;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Construye la estructura de la arena en el mundo:
 *  - Una plataforma circular decorada.
 *  - Un muro decorativo circular con pilares y linternas.
 *  - Una "caja" exterior completamente sellada con bloques barrier
 *    (suelo, techo y 4 paredes) que envuelve TANTO la arena como la
 *    sala de espera, para que sea físicamente imposible salir aunque
 *    los jugadores consigan un elytra en la última ronda.
 *  - Una sala de espera con una puerta hecha de barrotes de hierro,
 *    que /arena open y /arena close alternan por aire/barrotes.
 */
public class ArenaBuilder {

    public static final int PADDING = 3;      // hueco entre el muro decorativo y la caja de barriers
    public static final int ROOF_CLEARANCE = 6; // altura libre sobre el muro (para que quepan elytra)
    public static final int WAIT_WIDTH = 7;    // ancho (eje X) de la sala de espera
    public static final int WAIT_DEPTH = 5;    // profundidad (eje Z) de la sala de espera
    public static final int WAIT_HEIGHT = 4;   // altura interior de la sala de espera
    public static final int GATE_WIDTH = 2;    // ancho de la puerta de barrotes

    public static void build(ServerWorld world, Arena arena) {
        int cx = arena.x, cy = arena.y, cz = arena.z;
        int r = arena.radius;
        int wallTop = cy + arena.wallHeight;

        // La sala de espera se coloca al sur (Z negativo) de la arena.
        int gateZ = cz - r;                       // línea del muro donde va la puerta
        int waitCenterZ = gateZ - PADDING - 1 - (WAIT_DEPTH / 2);

        int boxMinX = cx - r - PADDING;
        int boxMaxX = cx + r + PADDING;
        int boxMinZ = Math.min(cz - r - PADDING, waitCenterZ - WAIT_DEPTH / 2 - 2);
        int boxMaxZ = cz + r + PADDING;
        int boxMinY = cy - 3;
        int boxMaxY = wallTop + ROOF_CLEARANCE;

        // 1. Vaciar todo el volumen interior (para no dejar terreno raro dentro).
        clearVolume(world, boxMinX + 1, boxMinY + 1, boxMinZ + 1, boxMaxX - 1, boxMaxY - 1, boxMaxZ - 1);

        // 2. Caja exterior sellada de bloques barrier (contención total).
        buildBarrierBox(world, boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ);

        // 3. Suelo circular decorado de la arena.
        buildFloor(world, cx, cy, cz, r);

        // 4. Muro decorativo circular con pilares y linternas.
        buildWall(world, cx, cy, cz, r, arena.wallHeight);

        // 5. Techo de cristal decorativo (por encima del muro, por debajo del barrier).
        buildGlassRoof(world, cx, cy, cz, r, wallTop + 1);

        // 6. Sala de espera + puerta de barrotes de hierro.
        buildWaitingRoom(world, arena, cx, cy, cz, r, waitCenterZ, gateZ);
    }

    private static void clearVolume(ServerWorld world, int x1, int y1, int z1, int x2, int y2, int z2) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    world.setBlockState(mut.set(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void buildBarrierBox(ServerWorld world, int x1, int y1, int z1, int x2, int y2, int z2) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
        var barrier = Blocks.BARRIER.getDefaultState();
        // Suelo y techo
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                world.setBlockState(mut.set(x, y1, z), barrier);
                world.setBlockState(mut.set(x, y2, z), barrier);
            }
        }
        // Las 4 paredes
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                world.setBlockState(mut.set(x, y, z1), barrier);
                world.setBlockState(mut.set(x, y, z2), barrier);
            }
            for (int z = z1; z <= z2; z++) {
                world.setBlockState(mut.set(x1, y, z), barrier);
                world.setBlockState(mut.set(x2, y, z), barrier);
            }
        }
    }

    private static void buildFloor(ServerWorld world, int cx, int cy, int cz, int r) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > r) continue;
                int x = cx + dx, z = cz + dz;
                var state = floorBlockFor(dist, r, dx, dz);
                world.setBlockState(mut.set(x, cy - 1, z), state);
                // Aseguramos que el nivel de pie esté libre.
                world.setBlockState(mut.set(x, cy, z), Blocks.AIR.getDefaultState());
                world.setBlockState(mut.set(x, cy + 1, z), Blocks.AIR.getDefaultState());
            }
        }
        // Marca central llamativa: aquí es donde aparecen los mobs de cada ronda.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(mut.set(cx + dx, cy - 1, cz + dz), Blocks.RED_NETHER_BRICKS.getDefaultState());
            }
        }
    }

    private static net.minecraft.block.BlockState floorBlockFor(double dist, int r, int dx, int dz) {
        // Anillo dorado justo en el borde para marcar el límite.
        if (dist > r - 1.2) return Blocks.GOLD_BLOCK.getDefaultState();
        int ring = (int) (dist / 3.0);
        return switch (ring % 3) {
            case 0 -> Blocks.POLISHED_BLACKSTONE.getDefaultState();
            case 1 -> Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            default -> Blocks.GILDED_BLACKSTONE.getDefaultState();
        };
    }

    private static void buildWall(ServerWorld world, int cx, int cy, int cz, int r, int wallHeight) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
        int steps = Math.max(64, r * 8);
        for (int i = 0; i < steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            int x = cx + (int) Math.round(r * Math.cos(angle));
            int z = cz + (int) Math.round(r * Math.sin(angle));
            boolean pillar = (i % (steps / 16) == 0);
            for (int h = 0; h <= wallHeight; h++) {
                var block = pillar
                        ? Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState()
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                world.setBlockState(mut.set(x, cy + h, z), block);
            }
            if (pillar) {
                world.setBlockState(mut.set(x, cy + wallHeight + 1, z), Blocks.LANTERN.getDefaultState());
            }
        }
    }

    private static void buildGlassRoof(ServerWorld world, int cx, int cy, int cz, int r, int roofY) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                world.setBlockState(mut.set(cx + dx, roofY, cz + dz), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
            }
        }
    }

    private static void buildWaitingRoom(ServerWorld world, Arena arena, int cx, int cy, int cz, int r,
                                          int waitCenterZ, int gateZ) {
        int halfW = WAIT_WIDTH / 2;
        int minX = cx - halfW, maxX = cx + halfW;
        int minZ = waitCenterZ - WAIT_DEPTH / 2, maxZ = waitCenterZ + WAIT_DEPTH / 2;
        BlockPos.Mutable mut = new BlockPos.Mutable();

        // Suelo, paredes y techo de la sala de espera.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(mut.set(x, cy - 1, z), Blocks.SMOOTH_STONE.getDefaultState());
                world.setBlockState(mut.set(x, cy + WAIT_HEIGHT, z), Blocks.POLISHED_ANDESITE.getDefaultState());
                boolean edge = (x == minX || x == maxX || z == minZ || z == maxZ);
                if (edge) {
                    for (int h = 0; h < WAIT_HEIGHT; h++) {
                        world.setBlockState(mut.set(x, cy + h, z), Blocks.POLISHED_ANDESITE.getDefaultState());
                    }
                } else {
                    for (int h = 0; h < WAIT_HEIGHT; h++) {
                        world.setBlockState(mut.set(x, cy + h, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        // Un par de linternas de pared.
        world.setBlockState(mut.set(minX, cy + 2, waitCenterZ), Blocks.LANTERN.getDefaultState());
        world.setBlockState(mut.set(maxX, cy + 2, waitCenterZ), Blocks.LANTERN.getDefaultState());

        // Pasillo entre la sala de espera y la puerta.
        for (int z = maxZ + 1; z < gateZ; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                world.setBlockState(mut.set(cx + dx, cy - 1, z), Blocks.SMOOTH_STONE.getDefaultState());
                world.setBlockState(mut.set(cx + dx, cy, z), Blocks.AIR.getDefaultState());
                world.setBlockState(mut.set(cx + dx, cy + 1, z), Blocks.AIR.getDefaultState());
            }
        }

        // La puerta: un hueco de GATE_WIDTH x 3 en el muro de la arena, en z = gateZ.
        arena.gateBlocks.clear();
        int gateHalf = GATE_WIDTH / 2;
        for (int dx = -gateHalf; dx <= gateHalf; dx++) {
            for (int h = 0; h < 3; h++) {
                BlockPos pos = new BlockPos(cx + dx, cy + h, gateZ);
                arena.gateBlocks.add(pos);
                // Se crea cerrada (con barrotes) por defecto.
                world.setBlockState(pos, Blocks.IRON_BARS.getDefaultState());
            }
        }
        arena.open = false;
    }

    /** Alterna la puerta entre barrotes de hierro (cerrada) y aire (abierta). */
    public static void setGateOpen(ServerWorld world, Arena arena, boolean open) {
        var state = open ? Blocks.AIR.getDefaultState() : Blocks.IRON_BARS.getDefaultState();
        for (BlockPos pos : arena.gateBlocks) {
            world.setBlockState(pos, state);
        }
        arena.open = open;
    }
}
