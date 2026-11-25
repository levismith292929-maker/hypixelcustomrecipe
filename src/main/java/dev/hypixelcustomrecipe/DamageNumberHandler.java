package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class DamageNumberHandler {
    private static final List<FloatingNumber> ACTIVE = new ArrayList<>();
    private static long nextOffset;

    private DamageNumberHandler() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<FloatingNumber> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                FloatingNumber number = iterator.next();
                number.age++;
                if (number.display.isRemoved() || number.age >= 24) {
                    number.display.discard();
                    iterator.remove();
                    continue;
                }

                number.display.setPos(number.x, number.y + number.age * 0.035, number.z);
            }
        });
    }

    public static void show(ServerLevel level, Entity target, float damage, boolean critical) {
        if (damage <= 0.0F) return;

        ArmorStand display = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return;

        String amount = String.format(Locale.ROOT, "%,.0f", damage);
        Component text = Component.literal(amount).withStyle(style ->
                style.withColor(critical ? 0xFFFF55 : 0xFFFFFF).withBold(true));

        // Large bosses such as the Wither can hide name tags placed inside their model.
        // Put their damage numbers above the hitbox so the numbers remain visible.
        double height = target.getBbHeight();
        double y = target.getY() + (height >= 2.5 ? height + 0.35 : height * 0.65 + 0.25);

        // Spread consecutive hits slightly so rapid damage ticks do not occupy one spot.
        double angle = (nextOffset++ % 8) * (Math.PI / 4.0);
        double radius = height >= 2.5 ? 0.38 : 0.16;
        double x = target.getX() + Math.cos(angle) * radius;
        double z = target.getZ() + Math.sin(angle) * radius;

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
