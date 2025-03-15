package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClaimChunkPacket {
    private final int chunkX;
    private final int chunkZ;

    public ClaimChunkPacket(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public ClaimChunkPacket(FriendlyByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeamByMember(player.getUUID());

                // 检查玩家是否在团队中且有权限管理领地
                if (team == null || !team.getPermission(player.getUUID()).canManageLands()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_claim"));
                    return;
                }

                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

                // 检查区块是否已被其他团队认领
                Team claimingTeam = teamManager.getTeamByChunk(chunkPos);
                if (claimingTeam != null) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.chunk_already_claimed"));
                    return;
                }

                // 检查是否超出最大认领区块数量
                int maxChunks = 100; // 可以根据需要调整或设置为配置项
                if (team.getClaimedChunks().size() >= maxChunks) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.max_chunks_reached"));
                    return;
                }

                // 认领区块
                team.claimChunk(chunkPos);
                teamManager.setDirty();

                // 发送成功消息
                player.sendSystemMessage(Component.translatable("message.dcteam.success.chunk_claimed"));

                // 同步团队数据到所有客户端
                TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
            }
        });
        return true;
    }
}