package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import com.dc.dccoin.api.CoinAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeamBankPacket {
    private final int amount;
    private final boolean isDeposit;

    public TeamBankPacket(int amount, boolean isDeposit) {
        this.amount = amount;
        this.isDeposit = isDeposit;
    }

    public TeamBankPacket(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
        this.isDeposit = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(amount);
        buf.writeBoolean(isDeposit);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeamByMember(player.getUUID());

                // 检查玩家是否在团队中
                if (team == null) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.not_in_team"));
                    return;
                }

                // 检查玩家是否有权限管理团队资金
                if (!team.getPermission(player.getUUID()).canManageBank()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_bank"));
                    return;
                }

                // 检查金额是否有效
                if (amount <= 0) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.invalid_amount"));
                    return;
                }

                // 检查团队银行余额上限
                int maxBalance = 1000000; // 设置最大余额为100万
                
                if (isDeposit) {
                    // 检查玩家余额是否足够
                    if (!CoinAPI.hasEnoughCoins(player, amount)) {
                        player.sendSystemMessage(Component.translatable("message.dcteam.error.insufficient_player_balance"));
                        return;
                    }
                    
                    // 检查是否超过团队银行余额上限
                    if (team.getBalance() + amount > maxBalance) {
                        player.sendSystemMessage(Component.translatable("message.dcteam.error.bank_balance_limit"));
                        return;
                    }
                    
                    // 扣除玩家货币并存入团队银行
                    if (CoinAPI.removeCoins(player, amount)) {
                        team.deposit(amount);
                        // 记录交易
                        team.addTransaction(player.getUUID(), amount, true);
                        player.sendSystemMessage(Component.translatable("message.dcteam.success.deposit", amount));
                    }
                } else {
                    // 检查团队银行余额是否足够
                    if (team.getBalance() < amount) {
                        player.sendSystemMessage(Component.translatable("message.dcteam.error.insufficient_balance"));
                        return;
                    }
                    
                    // 从团队银行取出并添加到玩家账户
                    if (CoinAPI.addCoins(player, amount)) {
                        team.withdraw(amount);
                        // 记录交易
                        team.addTransaction(player.getUUID(), amount, false);
                        player.sendSystemMessage(Component.translatable("message.dcteam.success.withdraw", amount));
                    }
                }

                teamManager.setDirty();
                // 同步团队数据到所有客户端
                TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
            }
        });
        return true;
    }
}