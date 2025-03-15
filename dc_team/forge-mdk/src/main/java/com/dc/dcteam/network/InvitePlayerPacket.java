package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import com.dc.dcteam.team.TeamPermission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class InvitePlayerPacket {
    private final String playerName;

    public InvitePlayerPacket(String playerName) {
        this.playerName = playerName;
    }

    public InvitePlayerPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                TeamManager teamManager = TeamManager.get(sender.serverLevel());
                Team team = teamManager.getTeamByMember(sender.getUUID());

                // 检查发送者是否有权限邀请玩家
                if (team == null || !team.getPermission(sender.getUUID()).canInvitePlayers()) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_invite"));
                    return;
                }

                // 获取目标玩家
                ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayerByName(playerName);
                if (targetPlayer == null) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.player_not_found"));
                    return;
                }

                // 检查目标玩家是否已经在一个团队中
                if (teamManager.getTeamByMember(targetPlayer.getUUID()) != null) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.player_already_in_team"));
                    return;
                }

                // 检查邀请冷却时间
                if (team.hasInviteCooldown(targetPlayer.getUUID())) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.invite_cooldown"));
                    return;
                }

                // 发送邀请
                team.invitePlayer(targetPlayer.getUUID());
                team.setInviteCooldown(targetPlayer.getUUID()); // 设置邀请冷却时间
                sender.sendSystemMessage(Component.translatable("message.dcteam.success.invitation_sent", targetPlayer.getName()));
                targetPlayer.sendSystemMessage(Component.translatable("message.dcteam.info.team_invitation", sender.getName(), team.getName()));
                teamManager.setDirty();
            }
        });
        return true;
    }
}