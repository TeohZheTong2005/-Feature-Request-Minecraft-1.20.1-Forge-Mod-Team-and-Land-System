package com.dc.dcteam.event;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LandProtectionListener {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) event.getLevel();
            ChunkPos chunkPos = new ChunkPos(event.getPos());
            Player player = event.getPlayer();

            Team owner = TeamManager.get(level).getChunkOwner(chunkPos);
            if (owner != null && player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                if (!owner.isMember(serverPlayer.getUUID()) || 
                    !owner.getPermission(serverPlayer.getUUID()).canModifyBlocks()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof ServerPlayer) {
            ServerLevel level = (ServerLevel) event.getLevel();
            ChunkPos chunkPos = new ChunkPos(event.getPos());
            ServerPlayer player = (ServerPlayer) event.getEntity();

            Team owner = TeamManager.get(level).getChunkOwner(chunkPos);
            if (owner != null) {
                if (!owner.isMember(player.getUUID()) || 
                    !owner.getPermission(player.getUUID()).canModifyBlocks()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) event.getLevel();
            ChunkPos chunkPos = new ChunkPos(event.getPos());
            Player player = event.getEntity();

            Team owner = TeamManager.get(level).getChunkOwner(chunkPos);
            if (owner != null && player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                if (!owner.isMember(serverPlayer.getUUID()) || 
                    !owner.getPermission(serverPlayer.getUUID()).canInteractWithBlocks()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Start event) {
        if (event.getLevel() instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) event.getLevel();
            BlockPos explosionPos = new BlockPos(
                (int) event.getExplosion().getPosition().x,
                (int) event.getExplosion().getPosition().y,
                (int) event.getExplosion().getPosition().z
            );
            ChunkPos chunkPos = new ChunkPos(explosionPos);

            // 如果爆炸发生在被保护的领地内，取消爆炸
            if (TeamManager.get(level).getChunkOwner(chunkPos) != null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Monster) {
            ServerLevel level = (ServerLevel) event.getLevel();
            ChunkPos chunkPos = new ChunkPos(event.getEntity().blockPosition());

            // 在被保护的领地内禁止怪物生成
            if (TeamManager.get(level).getChunkOwner(chunkPos) != null) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityAttack(EntityEvent.EntityInteract event) {
        if (event.getLevel() instanceof ServerLevel && 
            event.getEntity() instanceof ServerPlayer && 
            event.getTarget() instanceof Player) {
            
            ServerLevel level = (ServerLevel) event.getLevel();
            ChunkPos chunkPos = new ChunkPos(event.getTarget().blockPosition());
            ServerPlayer attacker = (ServerPlayer) event.getEntity();
            Player target = (Player) event.getTarget();

            Team owner = TeamManager.get(level).getChunkOwner(chunkPos);
            if (owner != null) {
                // 在领地内禁止PVP，除非攻击者和目标都是团队成员
                if (!owner.isMember(attacker.getUUID()) || !owner.isMember(target.getUUID())) {
                    event.setCanceled(true);
                }
            }
        }
    }
}