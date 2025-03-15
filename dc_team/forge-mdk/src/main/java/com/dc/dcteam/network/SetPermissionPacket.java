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

public class SetPermissionPacket {
    private final UUID targetPlayer;
    private final String permission;

    public SetPermissionPacket(UUID targetPlayer, String permission) {
        this.targetPlayer = targetPlayer;
        this.permission = permission;
    }

    public SetPermissionPacket(FriendlyByteBuf buf) {
        this.targetPlayer = buf.readUUID();
        this.permission = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayer);
        buf.writeUtf(permission);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                TeamManager teamManager = TeamManager.get(sender.serverLevel());
                Team team = teamManager.getTeamByMember(sender.getUUID());

                // 检查发送者是否在团队中
                if (team == null) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.not_in_team"));
                    return;
                }

                // 检查发送者是否有权限管理权限
                if (!team.getPermission(sender.getUUID()).canManagePermissions()) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_manage"));
                    return;
                }

                // 检查目标玩家是否在团队中
                if (!team.getMembers().contains(targetPlayer)) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.player_not_in_team"));
                    return;
                }

                // 检查是否试图修改团队领袖的权限
                if (targetPlayer.equals(team.getLeader())) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.cannot_modify_leader"));
                    return;
                }

                try {
                    TeamPermission newPermission = TeamPermission.valueOf(permission);
                    // 不允许设置为LEADER权限
                    if (newPermission == TeamPermission.LEADER) {
                        sender.sendSystemMessage(Component.translatable("message.dcteam.error.cannot_set_leader"));
                        return;
                    }

                    // 设置新权限
                    team.setPermission(targetPlayer, newPermission);
                    teamManager.setDirty();

                    // 发送成功消息
                    sender.sendSystemMessage(Component.translatable("message.dcteam.success.permission_updated"));

                    // 同步团队数据到所有客户端
                    TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
                } catch (IllegalArgumentException e) {
                    sender.sendSystemMessage(Component.translatable("message.dcteam.error.invalid_permission"));
                }
            }
        });
        return true;
    }
}