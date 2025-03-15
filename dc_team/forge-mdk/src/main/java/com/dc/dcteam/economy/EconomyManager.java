package com.dc.dcteam.economy;

import com.dc.dccoin.api.DCCoinAPI;
import net.minecraft.server.level.ServerPlayer;

/**
 * 经济系统管理器
 * 用于与dc_coin模组进行交互，处理团队的经济相关操作
 */
public class EconomyManager {
    private static EconomyManager instance;

    private EconomyManager() {}

    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }

    /**
     * 检查玩家是否有足够的金币
     * @param player 玩家
     * @param amount 金额
     * @return 是否有足够的金币
     */
    public boolean hasEnoughCoins(ServerPlayer player, int amount) {
        return DCCoinAPI.getInstance().getPlayerBalance(player) >= amount;
    }

    /**
     * 从玩家扣除金币
     * @param player 玩家
     * @param amount 金额
     * @return 是否扣除成功
     */
    public boolean withdrawCoins(ServerPlayer player, int amount) {
        if (hasEnoughCoins(player, amount)) {
            return DCCoinAPI.getInstance().subtractFromPlayer(player, amount);
        }
        return false;
    }

    /**
     * 给玩家添加金币
     * @param player 玩家
     * @param amount 金额
     */
    public void depositCoins(ServerPlayer player, int amount) {
        DCCoinAPI.getInstance().addToPlayer(player, amount);
    }
}