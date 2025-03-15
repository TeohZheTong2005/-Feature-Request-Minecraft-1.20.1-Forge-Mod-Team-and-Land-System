package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import com.dc.dccoin.api.CoinAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DepositMoneyPacket {
    private final int amount;

    public DepositMoneyPacket(int amount) {
        this.amount = amount;
    }

    public DepositMoneyPacket(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(amount);
    }

    private boolean hasEnoughMoney(ServerPlayer player, int amount) {
        return CoinAPI.getBalance(player.getUUID()) >= amount;
    }

    private void deductPlayerMoney(ServerPlayer player, int amount) {
        CoinAPI.deductBalance(player.getUUID(), amount);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TeamManager teamManager = TeamManager.get(player.serverLevel());
                Team team = teamManager.getTeamByMember(player.getUUID());

                // 检查玩家是否在团队中且有权限存款
                if (team == null || !team.getPermission(player.getUUID()).canDepositMoney()) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.no_permission_deposit"));
                    return;
                }

                // 检查存款金额是否有效
                if (amount <= 0) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.invalid_deposit_amount"));
                    return;
                }

                // 检查玩家是否有足够的金钱
                if (!player.getAbilities().instabuild && !hasEnoughMoney(player, amount)) {
                    player.sendSystemMessage(Component.translatable("message.dcteam.error.insufficient_funds"));
                    return;
                }

                // 从玩家账户扣除金钱
                if (!player.getAbilities().instabuild) {
                    deductPlayerMoney(player, amount);
                }

                // 将金钱存入团队账户
                team.deposit(amount);
                teamManager.setDirty();

                // 发送成功消息
                player.sendSystemMessage(Component.translatable("message.dcteam.success.money_deposited", amount));

                // 同步团队数据到所有客户端
                TeamPacketHandler.sendToAll(new SyncTeamDataPacket(team));
            }
        });
        return true;
    }
}