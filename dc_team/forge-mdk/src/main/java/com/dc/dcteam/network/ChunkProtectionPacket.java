package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChunkProtectionPacket {
    private final int chunkX;
    private final int chunkZ;
    private final boolean isProtected;

    public ChunkProtectionPacket(int chunkX, int chunkZ, boolean isProtected) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.isProtected = isProtected;
    }

    public ChunkProtectionPacket(FriendlyByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.isProtected = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeBoolean(isProtected);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeamByMember(player.getUUID());

                // 检查玩家是否在团队中且有权限管理领地
                if (team == null || !team.getPermission(player.getUUID()).canManageLands()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_protect"));
                    return;
                }

                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

                // 检查区块是否属于该团队
                if (teamManager.getTeamByChunk(chunkPos) != team) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.not_team_chunk"));
                    return;
                }

                // 设置区块保护状态
                if (isProtected) {
                    team.protectChunk(chunkPos);
                    player.sendSystemMessage(Component.translatable("message.dcteam.success.chunk_protected"));
                } else {
                    team.unprotectChunk(chunkPos);
                    player.sendSystemMessage(Component.translatable("message.dcteam.success.chunk_unprotected"));
                }

                teamManager.setDirty();
                // 同步团队数据到所有客户端
                TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
            }
        });
        return true;
    }
}