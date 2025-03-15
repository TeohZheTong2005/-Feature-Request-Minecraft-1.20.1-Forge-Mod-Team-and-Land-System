package com.dc.dcteam.team;

import com.dc.dcteam.economy.EconomyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TeamBank {
    private final Team team;
    private int balance;

    public TeamBank(Team team) {
        this.team = team;
        this.balance = 0;
    }

    public boolean deposit(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return false;
        }

        EconomyManager economyManager = EconomyManager.getInstance();
        if (economyManager.withdrawCoins(player, amount)) {
            balance += amount;
            return true;
        }
        return false;
    }

    public boolean withdraw(ServerPlayer player, int amount) {
        if (amount <= 0 || balance < amount) {
            return false;
        }

        if (!team.getPermission(player.getUUID()).canWithdrawMoney()) {
            return false;
        }

        EconomyManager economyManager = EconomyManager.getInstance();
        economyManager.depositCoins(player, amount);
        balance -= amount;
        return true;
    }

    public int getBalance() {
        return balance;
    }

    public void save(CompoundTag tag) {
        tag.putInt("balance", balance);
    }

    public void load(CompoundTag tag) {
        balance = tag.getInt("balance");
    }
}