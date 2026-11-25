package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class DamageNumberHandler {
    private static final List<FloatingNumber> ACTIVE = new ArrayList<>();

    private DamageNumberHandler() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<FloatingNumber> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                FloatingNumber number = iterator.next();
                number.age++;
                if (number.display.isRemoved() || number.age >= 20) {
                    number.display.discard();
                    iterator.remove();
                    continue;
                }

                number.display.setPos(number.x, number.y + number.age * 0.025, number.z);
            }
        });
    }

    public static void show(ServerLevel level, Entity target, float damage, boolean critical) {
        if (damage <= 0.0F) return;

        ArmorStand display = (ArmorStand) EntityType.byString("minecraft:armor_stand")
                .map(type -> type.create(level, EntitySpawnReason.TRIGGERED)).orElse(null);
        if (display == null) return;

        String amount = String.format(Locale.ROOT, "%,.0f", damage);
        Component text = Component.literal(amount).withStyle(style ->
                style.withColor(critical ? 0xFFFF55 : 0xFFFFFF).withBold(critical));

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.65 + 0.25;
        double z = target.getZ();

        display.setPos(x, y, z);
        display.setNoGravity(true);
        display.setInvisible(true);
        display.setCustomName(text);
        display.setCustomNameVisible(true);
        display.setSilent(true);

        if (level.addFreshEntity(display)) {
            ACTIVE.add(new FloatingNumber(display, x, y, z));
        }
    }

    private static final class FloatingNumber {
        private final ArmorStand display;
        private final double x;
        private final double y;
        private final double z;
        private int age;

        private FloatingNumber(ArmorStand display, double x, double y, double z) {
            this.display = display;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
