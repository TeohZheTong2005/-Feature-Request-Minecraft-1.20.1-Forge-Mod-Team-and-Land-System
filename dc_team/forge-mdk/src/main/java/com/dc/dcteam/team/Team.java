package com.dc.dcteam.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class Team {
    private static final int MAX_BALANCE = 1000000; // 设置最大余额为100万
    private String name;
    private UUID id;
    private UUID leader;
    private Set<UUID> members;
    private Set<UUID> invitedPlayers;
    private Map<UUID, TeamPermission> memberPermissions;
    private Set<ChunkPos> claimedChunks;
    private int balance;
    private List<TransactionRecord> transactionHistory;

    public Team(String name, UUID leader) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.invitedPlayers = new HashSet<>();
        this.memberPermissions = new HashMap<>();
        this.claimedChunks = new HashSet<>();
        this.balance = 0;
        this.transactionHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean addMember(UUID player) {
        return members.add(player);
    }

    public boolean removeMember(UUID player) {
        if (player.equals(leader)) {
            return false;
        }
        memberPermissions.remove(player);
        return members.remove(player);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public void invitePlayer(UUID player) {
        invitedPlayers.add(player);
    }

    public boolean isInvited(UUID player) {
        return invitedPlayers.contains(player);
    }

    public void removeInvite(UUID player) {
        invitedPlayers.remove(player);
    }

    public void setPermission(UUID player, TeamPermission permission) {
        if (members.contains(player) && !player.equals(leader)) {
            memberPermissions.put(player, permission);
        }
    }

    public TeamPermission getPermission(UUID player) {
        if (player.equals(leader)) {
            return TeamPermission.LEADER;
        }
        return memberPermissions.getOrDefault(player, TeamPermission.MEMBER);
    }

    public boolean addClaimedChunk(ChunkPos chunk) {
        return claimedChunks.add(chunk);
    }

    public boolean removeClaimedChunk(ChunkPos chunk) {
        return claimedChunks.remove(chunk);
    }

    public Set<ChunkPos> getClaimedChunks() {
        return Collections.unmodifiableSet(claimedChunks);
    }

    public int getBalance() {
        return balance;
    }

    public boolean deposit(int amount) {
        if (amount <= 0 || balance + amount > MAX_BALANCE) {
            return false;
        }
        balance += amount;
        addTransaction(null, amount, true);
        return true;
    }

    public boolean withdraw(int amount) {
        if (amount <= 0 || balance < amount) {
            return false;
        }
        balance -= amount;
        addTransaction(null, amount, false);
        return true;
    }

    public void addTransaction(UUID player, int amount, boolean isDeposit) {
        transactionHistory.add(new TransactionRecord(player, amount, isDeposit ? TransactionType.DEPOSIT : TransactionType.WITHDRAW));
    }

    public List<TransactionRecord> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putUUID("id", id);
        tag.putUUID("leader", leader);
        tag.putInt("balance", balance);

        // 保存成员列表
        ListTag membersTag = new ListTag();
        for (UUID member : members) {
            CompoundTag memberTag = new CompoundTag();
            memberTag.putUUID("uuid", member);
            membersTag.add(memberTag);
        }
        tag.put("members", membersTag);

        // 保存邀请列表
        ListTag invitedTag = new ListTag();
        for (UUID invited : invitedPlayers) {
            CompoundTag invitedPlayerTag = new CompoundTag();
            invitedPlayerTag.putUUID("uuid", invited);
            invitedTag.add(invitedPlayerTag);
        }
        tag.put("invited", invitedTag);

        // 保存权限设置
        CompoundTag permissionsTag = new CompoundTag();
        for (Map.Entry<UUID, TeamPermission> entry : memberPermissions.entrySet()) {
            permissionsTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("permissions", permissionsTag);

        // 保存已声明的区块
        ListTag chunksTag = new ListTag();
        for (ChunkPos chunk : claimedChunks) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putInt("x", chunk.x);
            chunkTag.putInt("z", chunk.z);
            chunksTag.add(chunkTag);
        }
        tag.put("chunks", chunksTag);

        // 保存交易记录
        ListTag transactionsTag = new ListTag();
        for (TransactionRecord record : transactionHistory) {
            CompoundTag recordTag = new CompoundTag();
            recordTag.putUUID("player", record.getPlayer());
            recordTag.putInt("amount", record.getAmount());
            recordTag.putString("type", record.getType().name());
            recordTag.putLong("timestamp", record.getTimestamp());
            transactionsTag.add(recordTag);
        }
        tag.put("transactions", transactionsTag);

        return tag;
    }

    public static Team load(CompoundTag tag) {
        String name = tag.getString("name");
        UUID leader = tag.getUUID("leader");
        Team team = new Team(name, leader);
        team.id = tag.getUUID("id");
        team.balance = tag.getInt("balance");
        
        // 加载邀请列表
        ListTag invitedTag = tag.getList("invited", 10);
        invitedTag.forEach(invitedElement -> {
            CompoundTag invitedPlayerTag = (CompoundTag) invitedElement;
            team.invitedPlayers.add(invitedPlayerTag.getUUID("uuid"));
        });

        // 加载成员列表
        ListTag membersTag = tag.getList("members", 10);
        for (int i = 0; i < membersTag.size(); i++) {
            CompoundTag memberTag = membersTag.getCompound(i);
            team.members.add(memberTag.getUUID("uuid"));
        }

        // 加载邀请列表
        ListTag invitedTag = tag.getList("invited", 10);
        for (int i = 0; i < invitedTag.size(); i++) {
            CompoundTag invitedPlayerTag = invitedTag.getCompound(i);
            team.invitedPlayers.add(invitedPlayerTag.getUUID("uuid"));
        }

        // 加载权限设置
        CompoundTag permissionsTag = tag.getCompound("permissions");
        for (String key : permissionsTag.getAllKeys()) {
            UUID memberId = UUID.fromString(key);
            TeamPermission permission = TeamPermission.valueOf(permissionsTag.getString(key));
            team.memberPermissions.put(memberId, permission);
        }

        // 加载已声明的区块
        ListTag chunksTag = tag.getList("chunks", 10);
        for (int i = 0; i < chunksTag.size(); i++) {
            CompoundTag chunkTag = chunksTag.getCompound(i);
            ChunkPos chunk = new ChunkPos(chunkTag.getInt("x"), chunkTag.getInt("z"));
            team.claimedChunks.add(chunk);
        }

        // 加载交易记录
        ListTag transactionsTag = tag.getList("transactions", 10);
        for (int i = 0; i < transactionsTag.size(); i++) {
            CompoundTag recordTag = transactionsTag.getCompound(i);
            UUID player = recordTag.getUUID("player");
            int amount = recordTag.getInt("amount");
            TransactionType type = TransactionType.valueOf(recordTag.getString("type"));
            team.transactionHistory.add(new TransactionRecord(player, amount, type));
        }

        return team;
    }

    public static class TransactionRecord {
        private final UUID player;
        private final int amount;
        private final TransactionType type;
        private final long timestamp;

        public TransactionRecord(UUID player, int amount, TransactionType type) {
            this.player = player;
            this.amount = amount;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getPlayer() {
            return player;
        }

        public int getAmount() {
            return amount;
        }

        public TransactionType getType() {
            return type;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW
    }
}



    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putUUID("id", id);
        tag.putUUID("leader", leader);
        tag.putInt("balance", balance);

        // 保存成员列表
        ListTag membersTag = new ListTag();
        for (UUID member : members) {
            CompoundTag memberTag = new CompoundTag();
            memberTag.putUUID("uuid", member);
            membersTag.add(memberTag);
        }
        tag.put("members", membersTag);

        // 保存邀请列表
        ListTag invitedTag = new ListTag();
        for (UUID invited : invitedPlayers) {
            CompoundTag invitedPlayerTag = new CompoundTag();
            invitedPlayerTag.putUUID("uuid", invited);
            invitedTag.add(invitedPlayerTag);
        }
        tag.put("invited", invitedTag);

        // 保存权限设置
        CompoundTag permissionsTag = new CompoundTag();
        for (Map.Entry<UUID, TeamPermission> entry : memberPermissions.entrySet()) {
            permissionsTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("permissions", permissionsTag);

        // 保存已声明的区块
        ListTag chunksTag = new ListTag();
        for (ChunkPos chunk : claimedChunks) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putInt("x", chunk.x);
            chunkTag.putInt("z", chunk.z);
            chunksTag.add(chunkTag);
        }
        tag.put("chunks", chunksTag);

        // 保存交易记录
        ListTag transactionsTag = new ListTag();
        for (TransactionRecord record : transactionHistory) {
            CompoundTag recordTag = new CompoundTag();
            recordTag.putUUID("player", record.getPlayer());
            recordTag.putInt("amount", record.getAmount());
            recordTag.putString("type", record.getType().name());
            recordTag.putLong("timestamp", record.getTimestamp());
            transactionsTag.add(recordTag);
        }
        tag.put("transactions", transactionsTag);

        return tag;
    }

    public static Team load(CompoundTag tag) {
        String name = tag.getString("name");
        UUID leader = tag.getUUID("leader");
        Team team = new Team(name, leader);
        team.id = tag.getUUID("id");
        team.balance = tag.getInt("balance");
        
        // 加载邀请列表
        ListTag invitedTag = tag.getList("invited", 10);
        invitedTag.forEach(invitedElement -> {
            CompoundTag invitedPlayerTag = (CompoundTag) invitedElement;
            team.invitedPlayers.add(invitedPlayerTag.getUUID("uuid"));
        });

        // 加载成员列表
        ListTag membersTag = tag.getList("members", 10);
        for (int i = 0; i < membersTag.size(); i++) {
            CompoundTag memberTag = membersTag.getCompound(i);
            team.members.add(memberTag.getUUID("uuid"));
        }

        // 加载邀请列表
        ListTag invitedTag = tag.getList("invited", 10);
        for (int i = 0; i < invitedTag.size(); i++) {
            CompoundTag invitedPlayerTag = invitedTag.getCompound(i);
            team.invitedPlayers.add(invitedPlayerTag.getUUID("uuid"));
        }

        // 加载权限设置
        CompoundTag permissionsTag = tag.getCompound("permissions");
        for (String key : permissionsTag.getAllKeys()) {
            UUID memberId = UUID.fromString(key);
            TeamPermission permission = TeamPermission.valueOf(permissionsTag.getString(key));
            team.memberPermissions.put(memberId, permission);
        }

        // 加载已声明的区块
        ListTag chunksTag = tag.getList("chunks", 10);
        for (int i = 0; i < chunksTag.size(); i++) {
            CompoundTag chunkTag = chunksTag.getCompound(i);
            ChunkPos chunk = new ChunkPos(chunkTag.getInt("x"), chunkTag.getInt("z"));
            team.claimedChunks.add(chunk);
        }

        // 加载交易记录
        ListTag transactionsTag = tag.getList("transactions", 10);
        for (int i = 0; i < transactionsTag.size(); i++) {
            CompoundTag recordTag = transactionsTag.getCompound(i);
            UUID player = recordTag.getUUID("player");
            int amount = recordTag.getInt("amount");
            TransactionType type = TransactionType.valueOf(recordTag.getString("type"));
            team.transactionHistory.add(new TransactionRecord(player, amount, type));
        }

        return team;
    }
}