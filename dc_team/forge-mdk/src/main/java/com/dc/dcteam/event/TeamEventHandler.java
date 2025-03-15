package com.dc.dcteam.event;

import com.dc.dcteam.config.DCTeamConfig;
import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class TeamEventHandler {
    // 使用ConcurrentHashMap缓存区块所有权信息
    private static final Map<ChunkPos, Team> CHUNK_OWNER_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = event.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            Team chunkOwner = getChunkOwner(serverLevel, chunkPos);

            if (chunkOwner != null) {
                ServerPlayer player = (ServerPlayer) event.getPlayer();
                if (!chunkOwner.isMember(player.getUUID()) ||
                    !chunkOwner.getPermission(player.getUUID()).canModifyBlocks()) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.literal("§c你没有权限在此区块破坏方块！"));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getEntity() instanceof ServerPlayer player) {
            BlockPos pos = event.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            Team chunkOwner = getChunkOwner(serverLevel, chunkPos);

            if (chunkOwner != null) {
                if (!chunkOwner.isMember(player.getUUID()) ||
                    !chunkOwner.getPermission(player.getUUID()).canModifyBlocks()) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.literal("§c你没有权限在此区块放置方块！"));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel() instanceof ServerLevel serverLevel &&
            event.getSpawnType() != MobSpawnType.SPAWN_EGG &&
            DCTeamConfig.DISABLE_MOB_SPAWNING_IN_CLAIMS.get()) {

            ChunkPos chunkPos = new ChunkPos(event.getEntity().blockPosition());
            Team chunkOwner = getChunkOwner(serverLevel, chunkPos);

            if (chunkOwner != null) {
                event.setCanceled(true);
            }
        }
    }

    // 使用缓存获取区块所有者
    private static Team getChunkOwner(ServerLevel level, ChunkPos pos) {
        return CHUNK_OWNER_CACHE.computeIfAbsent(pos, p -> TeamManager.get(level).getChunkOwner(p));
    }

    // 清除区块所有权缓存
    public static void clearChunkOwnerCache(ChunkPos pos) {
        CHUNK_OWNER_CACHE.remove(pos);
    }

    // 清除所有缓存
    public static void clearAllCache() {
        CHUNK_OWNER_CACHE.clear();
    }
}