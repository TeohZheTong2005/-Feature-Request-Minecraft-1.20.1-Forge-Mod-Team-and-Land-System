package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClaimLandPacket {
    private final ChunkPos chunk;

    public ClaimLandPacket(ChunkPos chunk) {
        this.chunk = chunk;
    }

    public ClaimLandPacket(FriendlyByteBuf buf) {
        this.chunk = new ChunkPos(buf.readInt(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunk.x);
        buf.writeInt(chunk.z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeamByMember(player.getUUID());

                // 检查玩家是否在团队中且有权限管理领地
                if (team == null || !team.getPermission(player.getUUID()).canManageLands()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission"));
                    return;
                }

                // 检查区块是否已被声明
                if (teamManager.getChunkOwner(chunk) != null) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.chunk_already_claimed"));
                    return;
                }

                // 检查团队是否达到最大领地数量限制
                if (DCTeamConfig.MAX_TEAM_LANDS.get() > 0 && 
                    team.getClaimedChunks().size() >= DCTeamConfig.MAX_TEAM_LANDS.get()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.max_lands_reached"));
                    return;
                }

                // 计算领地价格
                int landPrice = DCTeamConfig.INITIAL_LAND_PRICE.get() * 
                    (int)Math.pow(DCTeamConfig.LAND_PRICE_MULTIPLIER.get(), team.getClaimedChunks().size());

                // 检查玩家是否有足够的金币
                if (!EconomyManager.getInstance().hasEnoughCoins(player, landPrice)) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.not_enough_coins"));
                    return;
                }

                // 扣除金币并声明区块
                if (EconomyManager.getInstance().withdrawCoins(player, landPrice) && teamManager.claimChunk(team, chunk)) {
                    // 同步团队数据到所有客户端
                    TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
                    player.sendSystemMessage(Component.translatable("message.dcteam.success.land_claimed"));
                } else {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.claim_failed"));
                }
            }
        });
        return true;
    }
}