package net.arenamod.arena;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Genera las pilas de objetos (ItemStack) que van dentro de cada cofre
 * de recompensa. Los nombres de las "plantillas de armadura" (diseños)
 * se corresponden con las Smithing Templates reales de Minecraft:
 *   - "diseño de armadura del End"      -> Eye Armor Trim   (se obtiene en las End Cities)
 *   - "diseño de armadura de Vex"       -> Vex Armor Trim    (Mansión del Bosque)
 *   - "diseño de armadura silenciosa"   -> Silence Armor Trim (Ciudad Antigua)
 */
public class LootFactory {

    // ---------- RONDA 1: recompensa tras la araña gigante ----------
    public static List<ItemStack> round1Loot() {
        return List.of(
                new ItemStack(Items.NETHERITE_INGOT, 2),
                new ItemStack(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                new ItemStack(Items.CROSSBOW, 1),
                new ItemStack(Items.ARROW, 16)
        );
    }

    // ---------- RONDA 2: recompensa tras el husk (zombi del desierto) ----------
    public static List<ItemStack> round2Loot(ServerWorld world) {
        ItemStack lanza = new ItemStack(Items.TRIDENT, 1);
        lanza.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Lanza de Diamante").formatted(Formatting.AQUA));
        return List.of(
                new ItemStack(Items.DIAMOND, 32),
                lanza,
                new ItemStack(Items.BOW, 1),
                new ItemStack(Items.ARROW, 64),
                new ItemStack(Items.TOTEM_OF_UNDYING, 7),
                new ItemStack(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 1)
        );
    }

    // ---------- RONDA 3: recompensa tras las brujas + warden ----------
    public static List<ItemStack> round3Loot() {
        return List.of(
                new ItemStack(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                new ItemStack(Items.SCULK_SENSOR, 12),
                new ItemStack(Items.NETHERITE_SCRAP, 12),
                new ItemStack(Items.MACE, 1),
                new ItemStack(Items.WIND_CHARGE, 64),
                new ItemStack(Items.TOTEM_OF_UNDYING, 7)
        );
    }

    // ---------- RONDA 4: los 3 cofres finales ----------
    public static List<ItemStack> round4ChestA(ServerWorld world) {
        return List.of(
                unbreakableElytra(world, 100),
                new ItemStack(Items.MACE, 2),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 32)
        );
    }

    public static List<ItemStack> round4ChestB(ServerWorld world) {
        return List.of(
                unbreakableElytra(world, 100),
                new ItemStack(Items.TRIDENT, 3),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64)
        );
    }

    public static List<ItemStack> round4ChestC(ServerWorld world) {
        return List.of(
                new ItemStack(Items.BARRIER, 3),
                new ItemStack(Items.LIGHT, 1),
                unbreakableElytra(world, 100),
                heartPotion()
        );
    }

    /** Crea un elytra con el nivel de Irrompibilidad indicado (por defecto en vanilla el máximo es 3, aquí se fuerza a 100 tal y como se pidió). */
    public static ItemStack unbreakableElytra(ServerWorld world, int unbreakingLevel) {
        ItemStack stack = new ItemStack(Items.ELYTRA);
        applyEnchantment(world, stack, Enchantments.UNBREAKING, unbreakingLevel);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Elytra del Campeón (Irrompibilidad " + unbreakingLevel + ")").formatted(Formatting.LIGHT_PURPLE));
        return stack;
    }

    /** Poción especial que otorga 100 corazones (200 de vida) de forma instantánea y sostenida. */
    public static ItemStack heartPotion() {
        ItemStack stack = new ItemStack(Items.POTION);
        List<StatusEffectInstance> effects = List.of(
                // +180 de vida extra sostenidos (Health Boost nivel alto) durante 20 minutos
                new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 20 * 60 * 20, 44, false, true, true),
                // Curación instantánea grande al bebérsela para rellenar toda esa vida de golpe
                new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 9, false, true, true)
        );
        stack.set(DataComponentTypes.POTION_CONTENTS,
                new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0xFF5555), effects, java.util.Optional.empty()));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Poción de 100 Corazones").formatted(Formatting.RED));
        return stack;
    }

    private static void applyEnchantment(ServerWorld world, ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> key, int level) {
        RegistryEntry<Enchantment> entry = world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getEntry(key)
                .orElseThrow();
        stack.addEnchantment(entry, level);
    }
}
