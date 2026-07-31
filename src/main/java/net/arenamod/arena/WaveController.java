package net.arenamod.arena;

import net.arenamod.util.AttributeUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/**
 * Controla la progresión de las 4 rondas de una arena: qué mobs
 * aparecen, con qué escala/vida/daño, y qué pasa cuando se limpia
 * cada grupo.
 */
public class WaveController {

    // ----- Multiplicadores de daño (ajustables) -----
    private static final double SPIDER_DMG_MULT = 3.0;     // "ataque más fuerte de lo normal"
    private static final double HUSK_DMG_MULT = 10.0;      // "x10 de daño"
    private static final double WITCH_DMG_MULT = 3.0;      // "x3 más fuerte"
    private static final double WARDEN_DMG_MULT = 0.5;     // "50% menos ataque"
    private static final double GOLEM_DMG_MULT = 5.0;      // "x5 de daño"
    private static final double PILLAGER_DMG_MULT = 0.4;   // "60% menos ataque"

    public static void startPhase1(ServerWorld world, Arena arena) {
        arena.phase = 1;
        arena.subStage = 0;
        arena.activeMobs.clear();
        spawnMob(world, arena, EntityType.SPIDER, 7.0, 500.0, SPIDER_DMG_MULT, 0, 0);
        ArenaManager.save();
    }

    /** Se llama cuando un mob de la arena muere. Devuelve true si pertenecía a esta arena. */
    public static boolean onMobDeath(ServerWorld world, LivingEntity entity) {
        for (Arena arena : ArenaManager.ARENAS.values()) {
            if (!arena.dimensionId.equals(world.getRegistryKey().getValue().toString())) continue;
            if (arena.activeMobs.remove(entity.getUuid())) {
                if (arena.activeMobs.isEmpty()) {
                    onGroupCleared(world, arena);
                }
                return true;
            }
        }
        return false;
    }

    private static void onGroupCleared(ServerWorld world, Arena arena) {
        switch (arena.phase) {
            case 1 -> ChestUtil.spawnRandomChest(world, arena, LootFactory.round1Loot());
            case 2 -> ChestUtil.spawnRandomChest(world, arena, LootFactory.round2Loot(world));
            case 3 -> {
                if (arena.subStage == 0) {
                    arena.subStage = 1;
                    spawnMob(world, arena, EntityType.WARDEN, 3.0, 750.0, WARDEN_DMG_MULT, 0, 0);
                } else {
                    ChestUtil.spawnRandomChest(world, arena, LootFactory.round3Loot());
                }
            }
            case 4 -> {
                if (arena.subStage == 0) {
                    arena.subStage = 1;
                    for (int i = 0; i < 3; i++) {
                        int off = (i - 1) * 3;
                        spawnMob(world, arena, EntityType.PILLAGER, 4.0, 500.0, PILLAGER_DMG_MULT, off, 0);
                    }
                    giveAxes(world, arena);
                } else if (arena.subStage == 1) {
                    arena.subStage = 2;
                    for (int i = 0; i < 10; i++) {
                        double angle = (Math.PI * 2 * i) / 10.0;
                        int off_x = (int) Math.round(Math.cos(angle) * 3);
                        int off_z = (int) Math.round(Math.sin(angle) * 3);
                        spawnMob(world, arena, EntityType.SILVERFISH, 5.0, 200.0, 1.0, off_x, off_z);
                    }
                } else {
                    finishFinalRound(world, arena);
                }
            }
            default -> { /* fase 5 = completada, no hacer nada */ }
        }
        ArenaManager.save();
    }

    /** Llamado desde el tick del servidor cuando un cofre de ronda ha expirado. */
    public static void advancePhase(ServerWorld world, Arena arena) {
        switch (arena.phase) {
            case 1 -> {
                arena.phase = 2;
                arena.subStage = 0;
                spawnMob(world, arena, EntityType.HUSK, 5.0, 250.0, HUSK_DMG_MULT, 0, 0);
            }
            case 2 -> {
                arena.phase = 3;
                arena.subStage = 0;
                spawnMob(world, arena, EntityType.WITCH, 4.0, 400.0, WITCH_DMG_MULT, -2, 0);
                spawnMob(world, arena, EntityType.WITCH, 4.0, 400.0, WITCH_DMG_MULT, 2, 0);
            }
            case 3 -> {
                arena.phase = 4;
                arena.subStage = 0;
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 * i) / 5.0;
                    int off_x = (int) Math.round(Math.cos(angle) * 4);
                    int off_z = (int) Math.round(Math.sin(angle) * 4);
                    spawnMob(world, arena, EntityType.IRON_GOLEM, 3.0, 1000.0, GOLEM_DMG_MULT, off_x, off_z);
                }
            }
            default -> { /* nada más que avanzar */ }
        }
        ArenaManager.save();
    }

    private static void giveAxes(ServerWorld world, Arena arena) {
        for (var uuid : arena.activeMobs) {
            var entity = world.getEntity(uuid);
            if (entity instanceof PillagerEntity pillager) {
                pillager.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            }
        }
    }

    private static void finishFinalRound(ServerWorld world, Arena arena) {
        // 1 minuto de Oscuridad para todos los jugadores dentro de la arena.
        BlockPos center = arena.spawnPoint();
        for (ServerPlayerEntity player : world.getPlayers()) {
            double dx = player.getX() - center.getX();
            double dz = player.getZ() - center.getZ();
            if (dx * dx + dz * dz <= (double) arena.radius * arena.radius) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20 * 60, 0, false, true, true));
            }
        }
        ChestUtil.spawnFinalChests(world, arena);
        arena.phase = 5;
        arena.subStage = 0;
    }

    // ---------------------------------------------------------------

    private static <T extends net.minecraft.entity.mob.MobEntity> T spawnMob(
            ServerWorld world, Arena arena, EntityType<T> type,
            double scale, double health, double dmgMultiplier, int offsetX, int offsetZ) {
        T entity = type.create(world, SpawnReason.EVENT);
        if (entity == null) return null;
        BlockPos p = arena.spawnPoint();
        entity.refreshPositionAndAngles(p.getX() + 0.5 + offsetX, p.getY(), p.getZ() + 0.5 + offsetZ, 0f, 0f);
        entity.setPersistent();
        AttributeUtil.setScale(entity, scale);
        AttributeUtil.setMaxHealthAndHeal(entity, health);
        AttributeUtil.multiplyAttackDamage(entity, dmgMultiplier);
        world.spawnEntity(entity);
        arena.activeMobs.add(entity.getUuid());
        return entity;
    }
}
