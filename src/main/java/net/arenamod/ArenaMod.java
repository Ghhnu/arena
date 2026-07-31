package net.arenamod;

import net.arenamod.arena.Arena;
import net.arenamod.arena.ArenaCommand;
import net.arenamod.arena.ArenaManager;
import net.arenamod.arena.ChestUtil;
import net.arenamod.arena.WaveController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

public class ArenaMod implements ModInitializer {

    public static final String MOD_ID = "arenamod";

    @Override
    public void onInitialize() {
        // Registrar el comando /arena
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ArenaCommand.register(dispatcher));

        // Cuando muere un mob, comprobar si pertenecía a una arena.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.getWorld() instanceof ServerWorld world) {
                WaveController.onMobDeath(world, entity);
            }
        });

        // Cada tick del servidor: comprobar cofres que hayan expirado en cada dimensión.
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (Arena arena : ArenaManager.ARENAS.values()) {
                if (!arena.dimensionId.equals(world.getRegistryKey().getValue().toString())) continue;
                if (arena.phase >= 1 && arena.phase <= 4 && arena.waitingChest) {
                    boolean expired = ChestUtil.tickExpire(world, arena);
                    if (expired) {
                        WaveController.advancePhase(world, arena);
                    }
                }
            }
        });

        // Cargar las arenas guardadas al iniciar el servidor.
        ServerLifecycleEvents.SERVER_STARTED.register(ArenaManager::load);

        // Guardar al detener el servidor.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ArenaManager.save());
    }
}
