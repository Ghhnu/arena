package net.arenamod.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;

/**
 * Funciones de ayuda para cambiar el tamaño (escala), la vida máxima
 * y el daño de ataque de un mob cuando lo generamos para una oleada.
 */
public class AttributeUtil {

    /** Cambia el tamaño visual/físico del mob (1.0 = tamaño normal). */
    public static void setScale(LivingEntity entity, double scale) {
        EntityAttributeInstance inst = entity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (inst != null) {
            inst.setBaseValue(scale);
        }
    }

    /** Multiplica el daño de ataque cuerpo a cuerpo del mob por un factor. */
    public static void multiplyAttackDamage(LivingEntity entity, double multiplier) {
        EntityAttributeInstance inst = entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (inst != null) {
            double base = inst.getBaseValue();
            inst.setBaseValue(base * multiplier);
        }
    }

    /** Pone una vida máxima concreta y cura al mob a esa vida por completo. */
    public static void setMaxHealthAndHeal(LivingEntity entity, double maxHealth) {
        EntityAttributeInstance inst = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (inst != null) {
            inst.setBaseValue(maxHealth);
        }
        entity.setHealth((float) maxHealth);
    }

    /** Aumenta (o reduce) el radio de detección de seguimiento, útil para jefes grandes. */
    public static void setFollowRange(LivingEntity entity, double range) {
        EntityAttributeInstance inst = entity.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (inst != null) {
            inst.setBaseValue(range);
        }
    }
}
