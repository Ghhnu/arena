package net.arenamod.arena;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ArenaCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("arena")
                .requires(src -> src.hasPermissionLevel(2))
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("y", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                        .executes(ArenaCommand::createArena))))
                .then(literal("open").executes(ctx -> setOpen(ctx.getSource(), true)))
                .then(literal("close").executes(ctx -> setOpen(ctx.getSource(), false)))
        );
    }

    private static int createArena(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");

        if (!(src.getWorld() instanceof ServerWorld world)) {
            src.sendError(Text.literal("Este comando solo se puede usar en un mundo del servidor."));
            return 0;
        }

        src.sendFeedback(() -> Text.literal("Construyendo la arena... esto puede tardar un segundo.")
                .formatted(Formatting.GRAY), false);

        Arena arena = ArenaManager.create(world, new BlockPos(x, y, z));
        ArenaBuilder.build(world, arena);
        WaveController.startPhase1(world, arena);
        ArenaManager.save();

        src.sendFeedback(() -> Text.literal("✔ Arena '" + arena.name + "' creada en " + x + ", " + y + ", " + z + ".")
                .formatted(Formatting.GREEN), true);
        src.sendFeedback(() -> Text.literal("La araña gigante de la ronda 1 ya está esperando en el centro. Usa /arena open para dejar entrar a los jugadores.")
                .formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int setOpen(ServerCommandSource src, boolean open) {
        if (!(src.getWorld() instanceof ServerWorld world)) {
            src.sendError(Text.literal("Este comando solo se puede usar en un mundo del servidor."));
            return 0;
        }
        BlockPos pos = BlockPos.ofFloored(src.getPosition());
        Optional<Arena> found = ArenaManager.findNearest(world, pos);
        if (found.isEmpty()) {
            src.sendError(Text.literal("No se ha encontrado ninguna arena cerca."));
            return 0;
        }
        Arena arena = found.get();
        ArenaBuilder.setGateOpen(world, arena, open);
        ArenaManager.save();

        if (open) {
            src.sendFeedback(() -> Text.literal("🔓 La arena '" + arena.name + "' está ahora ABIERTA. Se ha retirado la puerta de barrotes de hierro.")
                    .formatted(Formatting.GREEN), true);
        } else {
            src.sendFeedback(() -> Text.literal("🔒 La arena '" + arena.name + "' está ahora CERRADA. Se ha colocado la puerta de barrotes de hierro.")
                    .formatted(Formatting.RED), true);
        }
        return 1;
    }
}
