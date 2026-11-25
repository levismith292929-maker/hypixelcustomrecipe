package dev.hypixelcustomrecipe;

import dev.hypixelcustomrecipe.mixin.DisplayAccessor;
import dev.hypixelcustomrecipe.mixin.TextDisplayAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

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

                // Fade during the final part of the animation.
                if (number.age >= 14) {
                    int opacity = Math.max(0, 255 - (number.age - 14) * 25);
                    ((TextDisplayAccessor) number.display).hypixelcustomrecipe$setTextOpacity((byte) opacity);
                }
            }
        });
    }

    public static void show(ServerLevel level, Entity target, float damage, boolean critical) {
        if (damage <= 0.0F) return;

        Display.TextDisplay display = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return;

        String amount = String.format(Locale.ROOT, "%,.0f", damage);
        Component text = Component.literal(amount).withStyle(style ->
                style.withColor(critical ? 0xFFFF55 : 0xFFFFFF).withBold(true));

        double height = target.getBbHeight();
        // Large bosses get their numbers above the hitbox, not inside their model.
        double y = target.getY() + (height >= 2.5 ? height + 0.35 : height * 0.65 + 0.25);

        // Give rapid hits separate positions so their numbers remain readable.
        double angle = (nextOffset++ % 8) * (Math.PI / 4.0);
        double radius = height >= 2.5 ? 0.48 : 0.18;
        double x = target.getX() + Math.cos(angle) * radius;
        double z = target.getZ() + Math.sin(angle) * radius;

        display.setPos(x, y, z);
        display.setNoGravity(true);
        ((TextDisplayAccessor) display).hypixelcustomrecipe$setText(text);
        // Text display flags: shadow + see-through. See-through keeps numbers visible
        // even when the target is behind blocks or other entities.
        ((TextDisplayAccessor) display).hypixelcustomrecipe$setFlags((byte) 3);
        ((TextDisplayAccessor) display).hypixelcustomrecipe$setBackgroundColor(0);
        ((DisplayAccessor) display).hypixelcustomrecipe$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        if (level.addFreshEntity(display)) {
            ACTIVE.add(new FloatingNumber(display, x, y, z));
        }
    }

    private static final class FloatingNumber {
        private final Display.TextDisplay display;
        private final double x;
        private final double y;
        private final double z;
        private int age;

        private FloatingNumber(Display.TextDisplay display, double x, double y, double z) {
            this.display = display;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
