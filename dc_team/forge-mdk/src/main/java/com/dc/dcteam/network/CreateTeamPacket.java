package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

public class CreateTeamPacket {
    private final String teamName;

    public CreateTeamPacket(String teamName) {
        this.teamName = teamName;
    }

    public CreateTeamPacket(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(teamName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                
                // 检查玩家是否已经在一个团队中
                if (teamManager.getTeamByMember(player.getUUID()) != null) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.already_in_team"));
                    return;
                }

                // 验证团队名称
                if (StringUtils.isBlank(teamName)) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.team_name_empty"));
                    return;
                }
                
                if (teamName.length() < 3 || teamName.length() > 16) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.team_name_length"));
                    return;
                }

                // 检查团队名称是否包含特殊字符
                if (!teamName.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5_-]+$")) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.team_name_invalid_chars"));
                    return;
                }

                // 检查团队名称是否已存在
                if (teamManager.getTeamByName(teamName) != null) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.team_name_exists"));
                    return;
                }

                // 创建新团队
                Team team = teamManager.createTeam(teamName, player.getUUID());
                if (team != null) {
                    // 发送成功消息
                    player.sendSystemMessage(Component.translatable("message.dcteam.success.team_created"));
                    // 同步团队数据到所有客户端
                    TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
                } else {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.team_creation_failed"));
                }
            }
        });
        return true;
    }
}