package dev.hypixelcustomrecipe;

import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenMobs;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public final class VoidriddenMobRenderers {
    private VoidriddenMobRenderers() {
    }

    public static void initialize() {
        EntityRenderers.register(VoidriddenMobs.VOIDRIDDEN_ZOMBIE, context -> new ZombieRenderer(context) {
            @Override
            public Identifier getTextureLocation(ZombieRenderState state) {
                return texture("zombie");
            }
        });
        EntityRenderers.register(VoidriddenMobs.VOIDRIDDEN_SKELETON, context -> new SkeletonRenderer(context) {
            @Override
            public Identifier getTextureLocation(SkeletonRenderState state) {
                return texture("skeleton");
            }
        });
        EntityRenderers.register(VoidriddenMobs.VOIDRIDDEN_ENDERMAN, context -> new EndermanRenderer(context) {
            @Override
            public Identifier getTextureLocation(EndermanRenderState state) {
                return texture("enderman");
            }
        });
        EntityRenderers.register(VoidriddenMobs.VOIDRIDDEN_SPIDER, context -> new SpiderRenderer(context) {
            @Override
            public Identifier getTextureLocation(LivingEntityRenderState state) {
                return texture("spider");
            }
        });
        EntityRenderers.register(VoidriddenMobs.VOIDBALL, ThrownItemRenderer::new);
        EntityRenderers.register(VoidriddenMobs.VOIDRIDDEN_CREEPER, context -> new CreeperRenderer(context) {
            @Override
            public Identifier getTextureLocation(CreeperRenderState state) {
                return texture("creeper");
            }
        });
    }

    private static Identifier texture(String mob) {
        return Identifier.fromNamespaceAndPath(
                HypixelCustomRecipe.MOD_ID, "textures/entity/voidridden/" + mob + ".png"
        );
    }
}
