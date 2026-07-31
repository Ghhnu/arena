package net.arenamod.arena;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Random;

/** Coloca y retira los cofres de recompensa de cada ronda. */
public class ChestUtil {

    public static final int LIFETIME_TICKS = 15 * 20; // 15 segundos
    private static final Random RANDOM = new Random();

    /** Elige un punto accesible aleatorio dentro de la arena y coloca ahí un cofre con los objetos dados. */
    public static void spawnRandomChest(ServerWorld world, Arena arena, List<ItemStack> items) {
        BlockPos pos = pickAccessiblePoint(world, arena);
        world.setBlockState(pos, Blocks.CHEST.getDefaultState());
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            for (int i = 0; i < items.size() && i < chest.size(); i++) {
                chest.setStack(i, items.get(i));
            }
        }
        arena.chestPos = pos;
        arena.chestExpireTick = world.getTime() + LIFETIME_TICKS;
        arena.waitingChest = true;

        world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1f, 1f);
        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                40, 0.3, 0.3, 0.3, 0.1);
    }

    private static BlockPos pickAccessiblePoint(ServerWorld world, Arena arena) {
        int safeRadius = Math.max(3, arena.radius - 3);
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = RANDOM.nextDouble() * safeRadius;
            int x = arena.x + (int) Math.round(Math.cos(angle) * dist);
            int z = arena.z + (int) Math.round(Math.sin(angle) * dist);
            BlockPos floor = new BlockPos(x, arena.y - 1, z);
            BlockPos foot = new BlockPos(x, arena.y, z);
            // Accesible = suelo sólido y hueco libre encima.
            if (!world.getBlockState(floor).isAir() && world.getBlockState(foot).isAir()) {
                return foot;
            }
        }
        // Si no encontramos nada tras varios intentos, usamos el centro.
        return arena.spawnPoint();
    }

    /** Elimina el cofre si ya ha pasado su tiempo de vida. Devuelve true si lo ha eliminado. */
    public static boolean tickExpire(ServerWorld world, Arena arena) {
        if (arena.chestPos == null || !arena.waitingChest) return false;
        if (world.getTime() < arena.chestExpireTick) return false;
        world.setBlockState(arena.chestPos, Blocks.AIR.getDefaultState());
        world.playSound(null, arena.chestPos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.6f, 1.4f);
        arena.chestPos = null;
        arena.chestExpireTick = -1;
        arena.waitingChest = false;
        return true;
    }

    /** Coloca los 3 cofres finales en puntos distintos y accesibles. */
    public static void spawnFinalChests(ServerWorld world, Arena arena) {
        placeFixedLoot(world, pickAccessiblePoint(world, arena), LootFactory.round4ChestA(world));
        placeFixedLoot(world, pickAccessiblePoint(world, arena), LootFactory.round4ChestB(world));
        placeFixedLoot(world, pickAccessiblePoint(world, arena), LootFactory.round4ChestC(world));
    }

    private static void placeFixedLoot(ServerWorld world, BlockPos pos, List<ItemStack> items) {
        world.setBlockState(pos, Blocks.CHEST.getDefaultState());
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            for (int i = 0; i < items.size() && i < chest.size(); i++) {
                chest.setStack(i, items.get(i));
            }
        }
        world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1f, 0.8f);
    }
}
