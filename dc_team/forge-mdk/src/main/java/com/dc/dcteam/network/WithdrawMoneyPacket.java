package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import com.dc.dccoin.api.CoinAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WithdrawMoneyPacket {
    private final int amount;

    public WithdrawMoneyPacket(int amount) {
        this.amount = amount;
    }

    public WithdrawMoneyPacket(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(amount);
    }

    private void addPlayerMoney(ServerPlayer player, int amount) {
        CoinAPI.addBalance(player.getUUID(), amount);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeamByMember(player.getUUID());

                // 检查玩家是否在团队中且有权限取款
                if (team == null || !team.getPermission(player.getUUID()).canManageTeamSettings()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_withdraw"));
                    return;
                }

                // 检查取款金额是否有效
                if (amount <= 0) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.invalid_withdraw_amount"));
                    return;
                }

                // 检查团队账户是否有足够的金钱
                if (team.getBalance() < amount) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.insufficient_team_funds"));
                    return;
                }

                // 从团队账户扣除金钱
                if (team.withdraw(amount, player.getUUID())) {
                    // 将金钱添加到玩家账户
                    addPlayerMoney(player, amount);
                    teamManager.setDirty();

                    // 发送成功消息
                    player.sendSystemMessage(Component.translatable("message.dcteam.success.money_withdrawn", amount));

                    // 同步团队数据到所有客户端
                    TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
                } else {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.withdraw_failed"));
                }
            }
        });
        return true;
    }
}