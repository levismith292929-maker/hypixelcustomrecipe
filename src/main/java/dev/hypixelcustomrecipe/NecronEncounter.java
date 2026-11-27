package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

public final class NecronEncounter {
    private static final double MAX_HP = 55_000_000.0D;
    private static final double ENTITY_HP = 1024.0D;
    private static final double HP_PER_ENTITY_HP = MAX_HP / ENTITY_HP;
    private static final int HALF_ARENA = 50;
    private static final int ARENA_HEIGHT = 20;
    private static final int MINION_INTERVAL = 60;
    private static final int LIGHTNING_INTERVAL = 180;
    private static final int SWORD_INTERVAL = 240;
    private static final int LASER_INTERVAL = 1200;
    private static final int LASER_DURATION = 240;
    private static WitherBoss necron;
    private static ServerLevel level;
    private static BlockPos center;
    private static boolean testMode;
    private static boolean laserActive;
    private static float lastHealth;
    private static int minionTimer, lightningTimer, swordTimer, laserTimer, laserTicks;
    private static double laserAngle;
    private static final Map<UUID, Double> damage = new HashMap<>();
    private static final Set<UUID> participants = new HashSet<>();
    private static final Map<UUID, Integer> radiationCooldown = new HashMap<>();
    private static final List<UUID> minions = new ArrayList<>();
    private static final List<Blade> blades = new ArrayList<>();
    private static final List<Radiation> radiation = new ArrayList<>();
    private static final ServerBossEvent BAR = new ServerBossEvent(UUID.randomUUID(),
            Component.literal("Necron"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    private NecronEncounter() {}

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (necron != null && entity == necron
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (necron != null && entity.getUUID().equals(necron.getUUID())) finish();
        });
        ServerTickEvents.END_SERVER_TICK.register(NecronEncounter::tick);
    }

    public static boolean isActive() { return necron != null && necron.isAlive(); }
    public static boolean isNecron(LivingEntity entity) { return necron != null && entity == necron; }
    public static boolean isLaserActive() { return isActive() && laserActive; }

    public static boolean canPlaceBlock(ServerPlayer player, BlockPos pos) {
        if (!isActive() || player.level() != level || !insideArena(pos)) return true;
        return laserActive;
    }

    private static boolean insideArena(BlockPos pos) {
        return pos.getX() >= center.getX() - HALF_ARENA && pos.getX() < center.getX() + HALF_ARENA
                && pos.getZ() >= center.getZ() - HALF_ARENA && pos.getZ() < center.getZ() + HALF_ARENA
                && pos.getY() > center.getY() && pos.getY() <= center.getY() + ARENA_HEIGHT;
    }

    public static boolean summonForTesting(ServerPlayer player) {
        if (isActive() || MaxorEncounter.isActive() || StormEncounter.isActive() || GoldorEncounter.isActive()) return false;
        return start(player, (ServerLevel) player.level(), true);
    }

    private static boolean start(ServerPlayer starter, ServerLevel world, boolean testing) {
        WitherBoss boss = EntityTypes.WITHER.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) return false;
        center = starter.blockPosition();
        level = world;
        clearArena();
        boss.setPos(center.getX() + 0.5D, center.getY() + 4.0D, center.getZ() + 0.5D);
        boss.setCustomName(Component.literal("Necron").withStyle(s -> s.withColor(0xAA0000).withBold(true)));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(ENTITY_HP);
        boss.setHealth((float) ENTITY_HP);
        boss.setTarget(starter);
        if (!world.addFreshEntity(boss)) { clear(); return false; }
        necron = boss;
        testMode = testing;
        lastHealth = (float) ENTITY_HP;
        minionTimer = MINION_INTERVAL;
        lightningTimer = LIGHTNING_INTERVAL;
        swordTimer = SWORD_INTERVAL;
        laserTimer = LASER_INTERVAL;
        laserTicks = 0;
        laserActive = false;
        laserAngle = 0;
        damage.clear(); participants.clear(); radiationCooldown.clear(); minions.clear(); blades.clear(); radiation.clear();
        BAR.setProgress(1.0F);
        BAR.setName(Component.literal("Necron: 55,000,000 / 55,000,000 HP"));
        BAR.addPlayer(starter);
        world.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
        starter.sendSystemMessage(Component.literal(testing ? "NECRON TEST SUMMONED!" : "NECRON HAS AWAKENED!")
                .withStyle(s -> s.withColor(0xAA0000).withBold(true)));
        starter.sendSystemMessage(Component.literal("Elytras are disabled during this encounter.")
                .withStyle(s -> s.withColor(0xFF5555)));
        return true;
    }

    private static void clearArena() {
        // Preserve the floor; clear a 100x100x20 volume above it for the encounter.
        for (int x = -HALF_ARENA; x < HALF_ARENA; x++) {
            for (int z = -HALF_ARENA; z < HALF_ARENA; z++) {
                for (int y = 1; y <= ARENA_HEIGHT; y++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (necron == null || level == null || !necron.isAlive() || necron.isRemoved()) return;
        if (allFightersDead(server)) {
            abortBecauseAllFightersDied();
            return;
        }
        float hp = necron.getHealth();
        if (hp > lastHealth) necron.setHealth(lastHealth); else lastHealth = hp;
        ServerPlayer target = nearestPlayer();
        if (target != null) {
            necron.setTarget(target);
            if (laserActive) {
                necron.getNavigation().stop();
                necron.setDeltaMovement(Vec3.ZERO);
                necron.setPos(center.getX() + 0.5D, center.getY() + 4.0D, center.getZ() + 0.5D);
            } else {
                // Necron's movement leaves permanent radiation marks behind.
                if (necron.tickCount % 8 == 0) addRadiation(necron.position());
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level) { BAR.removePlayer(player); continue; }
            if (player.distanceToSqr(Vec3.atCenterOf(center)) <= 110.0D * 110.0D) {
                BAR.addPlayer(player);
            } else BAR.removePlayer(player);
            if (player.isFallFlying()) player.stopFallFlying();
        }
        radiationCooldown.replaceAll((id, ticks) -> ticks - 1);
        radiationCooldown.values().removeIf(ticks -> ticks <= 0);
        applyRadiation();
        tickBlades();
        minions.removeIf(id -> { Entity e=level.getEntity(id); return e==null || !e.isAlive(); });
        if (--minionTimer <= 0) { minionTimer=MINION_INTERVAL; spawnMinions(); }
        if (--lightningTimer <= 0) { lightningTimer=LIGHTNING_INTERVAL; lightningBarrage(); }
        if (--swordTimer <= 0) { swordTimer=SWORD_INTERVAL; launchSwords(); }
        if (laserActive) tickLaser();
        else if (--laserTimer <= 0) beginLaser();
        float progress=Math.max(0.0F,Math.min(1.0F,necron.getHealth()/(float)ENTITY_HP));
        BAR.setProgress(progress);
        long shown=Math.max(0L,Math.round(progress*MAX_HP));
        BAR.setName(Component.literal("Necron: "+String.format(Locale.ROOT,"%,d",shown)+" / 55,000,000 HP"));
    }

    private static ServerPlayer nearestPlayer() {
        ServerPlayer nearest=null; double best=120.0D*120.0D;
        for(ServerPlayer p:level.getServer().getPlayerList().getPlayers()){
            if(p.level()!=level || !p.isAlive()) continue;
            double d=p.distanceToSqr(necron); if(d<best){best=d;nearest=p;}
        }
        return nearest;
    }

    private static void spawnMinions() {
        for(int i=0;i<12 && minions.size()<72;i++){
            WitherSkeleton mob=EntityTypes.WITHER_SKELETON.create(level,EntitySpawnReason.MOB_SUMMONED);
            if(mob==null) continue;
            double a=level.getRandom().nextDouble()*Math.PI*2, r=4+level.getRandom().nextDouble()*8;
            mob.setPos(necron.getX()+Math.cos(a)*r,center.getY()+1,necron.getZ()+Math.sin(a)*r);
            mob.setPersistenceRequired();
            ServerPlayer p=nearestPlayer(); if(p!=null) mob.setTarget(p);
            if(level.addFreshEntity(mob)) minions.add(mob.getUUID());
        }
    }

    private static void lightningBarrage() {
        level.playSound(null,center,SoundEvents.LIGHTNING_BOLT_THUNDER,SoundSource.HOSTILE,4.0F,0.65F);
        for(ServerPlayer p:level.getServer().getPlayerList().getPlayers()){
            if(p.level()!=level || !p.isAlive() || !insideArena(p.blockPosition())) continue;
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,p.blockPosition().getX(),p.blockPosition().getZ());
            var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
            if(bolt!=null){bolt.setVisualOnly(true);bolt.setPos(p.getX(),Math.max(y,center.getY()),p.getZ());level.addFreshEntity(bolt);}
            SkyblockPlayerState state=SkyblockPlayerState.get(p);
            float amount=(float)(state.maximumHealth()*0.15D);
            if(state.applyTrueDamage(amount)){p.setHealth(0);p.die(level.damageSources().lightningBolt());}
            DamageNumberHandler.show(level,p,amount,false);
        }
    }

    private static void launchSwords() {
        for(ServerPlayer p:level.getServer().getPlayerList().getPlayers()){
            if(p.level()!=level || !p.isAlive() || !insideArena(p.blockPosition())) continue;
            for(int i=0;i<6;i++){
                double a=level.getRandom().nextDouble()*Math.PI*2;
                double r=level.getRandom().nextDouble()*4.0D;
                BlockPos pos=new BlockPos((int)Math.floor(p.getX()+Math.cos(a)*r),
                        Math.max(center.getY()+1,p.blockPosition().getY()),(int)Math.floor(p.getZ()+Math.sin(a)*r));
                blades.add(new Blade(pos,spawnBlade(pos),0));
            }
        }
    }

    private static Display.ItemDisplay spawnBlade(BlockPos pos) {
        Display.ItemDisplay d=EntityTypes.ITEM_DISPLAY.create(level,EntitySpawnReason.COMMAND);
        if(d==null)return null;
        d.setItemStack(new ItemStack(Items.NETHERITE_SWORD)); d.setItemTransform(ItemDisplayContext.FIXED);
        d.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(4,4,4),new Quaternionf()));
        d.setPos(necron.getX(),necron.getY()+2,necron.getZ());
        Vec3 end=Vec3.atCenterOf(pos); Vec3 dir=end.subtract(d.position()).normalize();
        Quaternionf q=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f((float)dir.x,(float)dir.y,(float)dir.z));
        d.setTransformation(new Transformation(new Vector3f(),q,new Vector3f(4,4,4),new Quaternionf()));
        level.addFreshEntity(d); return d;
    }

    private static void tickBlades() {
        Iterator<Blade> it=blades.iterator();
        while(it.hasNext()){
            Blade b=it.next();
            if(b.display==null){it.remove();continue;}
            if(b.age<4){
                Vec3 start=necron.position().add(0,2,0), end=Vec3.atCenterOf(b.pos);
                double t=(b.age+1)/4.0D; Vec3 at=start.lerp(end,t);
                b.display.setPos(at.x,at.y,at.z); b.age++;
            }else if(b.age<604){
                AABB box=new AABB(b.pos).inflate(2.0D);
                for(ServerPlayer p:level.getEntitiesOfClass(ServerPlayer.class,box)){
                    if(p.isAlive()){p.setHealth(0);p.die(level.damageSources().generic());}
                }
                b.age++;
            }else{b.display.discard();it.remove();}
        }
    }

    private static void beginLaser() {
        laserActive=true; laserTicks=LASER_DURATION; laserAngle=0;
        necron.getNavigation().stop(); necron.setDeltaMovement(Vec3.ZERO);
        necron.setPos(center.getX()+0.5D,center.getY()+4.0D,center.getZ()+0.5D);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON'S LASER! Build at least 4 blocks high! Building is now enabled.")
                .withStyle(s->s.withColor(0xFF5555).withBold(true)),false);
        level.playSound(null,center,SoundEvents.BEACON_ACTIVATE,SoundSource.HOSTILE,4.0F,0.5F);
    }

    private static void tickLaser() {
        laserAngle += Math.PI*2.0D/160.0D;
        double radius=HALF_ARENA-1.0D;
        double x=center.getX()+0.5D+Math.cos(laserAngle)*radius;
        double z=center.getZ()+0.5D+Math.sin(laserAngle)*radius;
        // A bright, beacon-like vertical column sweeps around the arena.
        for(double y=center.getY()+1;y<=center.getY()+ARENA_HEIGHT;y+=0.45D){
            level.sendParticles(ParticleTypes.END_ROD,x,y,z,2,0.015D,0.02D,0.015D,0.0D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,x,y,z,1,0.02D,0.02D,0.02D,0.0D);
        }
        if(laserTicks%4==0)addRadiation(new Vec3(x,center.getY()+1,z));
        AABB hit=new AABB(x-0.8D,center.getY()+1,z-0.8D,x+0.8D,center.getY()+ARENA_HEIGHT,z+0.8D);
        for(ServerPlayer p:level.getEntitiesOfClass(ServerPlayer.class,hit)){
            if(p.getY() < center.getY()+5.0D){
                SkyblockPlayerState state=SkyblockPlayerState.get(p);
                float hurt=(float)(state.maximumHealth()*0.20D);
                if(state.applyTrueDamage(hurt)){p.setHealth(0);p.die(level.damageSources().magic());}
                DamageNumberHandler.show(level,p,hurt,false);
            }
        }
        laserTicks--;
        if(laserTicks<=0){
            laserActive=false; laserTimer=LASER_INTERVAL;
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                    "Necron's laser has ended. Building is disabled again.")
                    .withStyle(s->s.withColor(0xAA00AA)),false);
            level.playSound(null,center,SoundEvents.BEACON_DEACTIVATE,SoundSource.HOSTILE,3.0F,0.7F);
        }
    }

    private static void addRadiation(Vec3 pos) {
        if(radiation.size()>12000) radiation.remove(0);
        radiation.add(new Radiation(pos, new HashSet<>()));
        level.sendParticles(ParticleTypes.ASH,pos.x,pos.y+0.1D,pos.z,5,0.25D,0.12D,0.25D,0.005D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,pos.x,pos.y+0.1D,pos.z,2,0.2D,0.1D,0.2D,0.0D);
    }

    private static void applyRadiation() {
        for(Radiation patch:radiation){
            AABB area=new AABB(patch.pos.x-0.65D,patch.pos.y-0.65D,patch.pos.z-0.65D,
                    patch.pos.x+0.65D,patch.pos.y+0.65D,patch.pos.z+0.65D);
            for(ServerPlayer p:level.getEntitiesOfClass(ServerPlayer.class,area)){
                if(!patch.hit.add(p.getUUID()) || radiationCooldown.containsKey(p.getUUID())) continue;
                SkyblockPlayerState.get(p).addNecronRadiationStack();
                radiationCooldown.put(p.getUUID(),40);
                p.sendSystemMessage(Component.literal("RADIATION! Maximum Health permanently reduced by 5%.")
                        .withStyle(s->s.withColor(0xAA00AA).withBold(true)));
                level.sendParticles(ParticleTypes.LARGE_SMOKE,p.getX(),p.getY()+1,p.getZ(),12,0.35D,0.4D,0.35D,0.02D);
            }
        }
    }

    public static void recordDirectDamage(LivingEntity target,ServerPlayer attacker,double entityDamage) {
        if(necron==null || target!=necron || entityDamage<=0)return;
        damage.merge(attacker.getUUID(),entityDamage*HP_PER_ENTITY_HP,Double::sum);
        participants.add(attacker.getUUID());
    }

    private static boolean allFightersDead(MinecraftServer server) {
        if (participants.isEmpty()) return false;
        for (UUID id : participants) {
            ServerPlayer fighter = server.getPlayerList().getPlayer(id);
            if (fighter != null && fighter.isAlive()) return false;
        }
        return true;
    }

    private static void abortBecauseAllFightersDied() {
        if (level == null) return;
        BAR.removeAllPlayers();
        for (UUID id : minions) { Entity e = level.getEntity(id); if (e != null) e.discard(); }
        for (Blade b : blades) if (b.display != null) b.display.discard();
        if (necron != null) necron.discard();
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("All players fighting Necron have been defeated. The encounter has ended."), false);
        clear();
    }

    private static void finish() {
        if(level==null)return;
        BAR.removeAllPlayers();
        for(UUID id:minions){Entity e=level.getEntity(id);if(e!=null)e.discard();}
        for(Blade b:blades)if(b.display!=null)b.display.discard();
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Necron has been defeated!"),false);
        level.playSound(null,center,SoundEvents.WITHER_DEATH,SoundSource.HOSTILE,4.0F,0.5F);
        clear();
    }

    private static void clear() {
        necron=null;level=null;center=null;testMode=false;laserActive=false;
        damage.clear();participants.clear();radiationCooldown.clear();minions.clear();blades.clear();radiation.clear();
    }

    private static final class Blade {
        final BlockPos pos; final Display.ItemDisplay display; int age;
        Blade(BlockPos pos,Display.ItemDisplay display,int age){this.pos=pos;this.display=display;this.age=age;}
    }
    private static final class Radiation {
        final Vec3 pos; final Set<UUID> hit;
        Radiation(Vec3 pos,Set<UUID> hit){this.pos=pos;this.hit=hit;}
    }
}
