package com.dc.dcteam.economy;

import com.dc.dcteam.team.Team;
import com.dc.dccoin.api.CoinAPI;
import net.minecraft.server.level.ServerPlayer;

/**
 * 团队银行系统管理器
 * 用于处理团队的存款和取款操作
 */
public class TeamBankManager {
    private static TeamBankManager instance;
    private static final int MAX_BALANCE = 1000000; // 设置最大余额为100万

    private TeamBankManager() {}

    public static TeamBankManager getInstance() {
        if (instance == null) {
            instance = new TeamBankManager();
        }
        return instance;
    }

    /**
     * 将金币存入团队银行
     * @param player 存款的玩家
     * @param team 目标团队
     * @param amount 存款金额
     * @return 是否存款成功
     */
    public boolean depositToTeam(ServerPlayer player, Team team, int amount) {
        if (amount <= 0) return false;
        if (team.getBalance() + amount > MAX_BALANCE) return false;
        
        if (CoinAPI.hasEnoughCoins(player, amount)) {
            if (CoinAPI.removeCoins(player, amount)) {
                team.deposit(amount);
                team.addTransaction(player.getUUID(), amount, true);
                return true;
            }
        }
        return false;
    }

    /**
     * 从团队银行取出金币
     * @param player 取款的玩家
     * @param team 目标团队
     * @param amount 取款金额
     * @return 是否取款成功
     */
    public boolean withdrawFromTeam(ServerPlayer player, Team team, int amount) {
        if (amount <= 0) return false;
        if (team.getBalance() < amount) return false;
        
        if (CoinAPI.addCoins(player, amount)) {
            team.withdraw(amount);
            team.addTransaction(player.getUUID(), amount, false);
            return true;
        }
        return false;
    }

    /**
     * 获取团队余额
     * @param team 目标团队
     * @return 团队余额
     */
    public int getTeamBalance(Team team) {
        return team.getBalance();
    }

    /**
     * 获取团队银行余额上限
     * @return 余额上限
     */
    public static int getMaxBalance() {
        return MAX_BALANCE;
    }
}