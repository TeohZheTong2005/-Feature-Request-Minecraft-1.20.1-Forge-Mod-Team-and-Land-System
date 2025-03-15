package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class JoinTeamPacket {
    private final UUID teamId;

    public JoinTeamPacket(UUID teamId) {
        this.teamId = teamId;
    }

    public JoinTeamPacket(FriendlyByteBuf buf) {
        this.teamId = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(teamId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeam(teamId);

                // 检查玩家是否已经在一个团队中
                if (teamManager.getTeamByMember(player.getUUID()) != null) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.already_in_team"));
                    return;
                }

                // 检查团队是否存在且玩家是否被邀请
                if (team == null || !team.isInvited(player.getUUID())) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.invalid_invitation"));
                    return;
                }

                // 检查团队是否已达到最大成员数量
                int maxMembers = 5; // 限制团队最大成员数为5人
                if (team.getMembers().size() >= maxMembers) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.team_full"));
                    return;
                }

                // 加入团队
                team.addMember(player.getUUID());
                team.removeInvite(player.getUUID());
                teamManager.setDirty();

                // 发送成功消息
                player.sendSystemMessage(Component.translatable("message.dcteam.success.team_joined", team.getName()));

                // 同步团队数据到所有客户端
                TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
            }
        });
        return true;
    }
}