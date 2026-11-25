package dev.hypixelcustomrecipe.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("setText")
    void hypixelcustomrecipe$setText(Component text);

    @Invoker("setTextOpacity")
    void hypixelcustomrecipe$setTextOpacity(byte opacity);

    @Invoker("setBackgroundColor")
    void hypixelcustomrecipe$setBackgroundColor(int color);

    @Invoker("setFlags")
    void hypixelcustomrecipe$setFlags(byte flags);
}
