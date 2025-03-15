package com.dc.dcteam.network;

import com.dc.dcteam.team.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SyncTeamDataPacket {
    private final UUID teamId;
    private final String teamName;
    private final UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> invitedPlayers;
    private final Map<UUID, String> memberPermissions;
    private final Set<ChunkPos> claimedChunks;
    private final int balance;

    public SyncTeamDataPacket(Team team) {
        this.teamId = team.getId();
        this.teamName = team.getName();
        this.leader = team.getLeader();
        this.members = team.getMembers();
        this.invitedPlayers = new HashSet<>();
        team.getMembers().forEach(member -> {
            if (team.isInvited(member)) {
                invitedPlayers.add(member);
            }
        });
        // 同步所有被邀请的玩家，而不仅仅是已经是成员的玩家
        team.getTransactionHistory().forEach(record -> {
            if (record.getPlayer() != null && team.isInvited(record.getPlayer())) {
                invitedPlayers.add(record.getPlayer());
            }
        });
        this.memberPermissions = new HashMap<>();
        team.getMembers().forEach(member ->
            memberPermissions.put(member, team.getPermission(member).name()));
        this.claimedChunks = team.getClaimedChunks();
        this.balance = team.getBalance();
    }

    public SyncTeamDataPacket(FriendlyByteBuf buf) {
        this.teamId = buf.readUUID();
        this.teamName = buf.readUtf();
        this.leader = buf.readUUID();

        int memberCount = buf.readInt();
        this.members = new HashSet<>();
        for (int i = 0; i < memberCount; i++) {
            members.add(buf.readUUID());
        }

        int invitedCount = buf.readInt();
        this.invitedPlayers = new HashSet<>();
        for (int i = 0; i < invitedCount; i++) {
            invitedPlayers.add(buf.readUUID());
        }

        int permissionCount = buf.readInt();
        this.memberPermissions = new HashMap<>();
        for (int i = 0; i < permissionCount; i++) {
            memberPermissions.put(buf.readUUID(), buf.readUtf());
        }

        int chunkCount = buf.readInt();
        this.claimedChunks = new HashSet<>();
        for (int i = 0; i < chunkCount; i++) {
            claimedChunks.add(new ChunkPos(buf.readInt(), buf.readInt()));
        }

        this.balance = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(teamId);
        buf.writeUtf(teamName);
        buf.writeUUID(leader);

        buf.writeInt(members.size());
        members.forEach(buf::writeUUID);

        buf.writeInt(invitedPlayers.size());
        invitedPlayers.forEach(buf::writeUUID);

        buf.writeInt(memberPermissions.size());
        memberPermissions.forEach((uuid, permission) -> {
            buf.writeUUID(uuid);
            buf.writeUtf(permission);
        });

        buf.writeInt(claimedChunks.size());
        claimedChunks.forEach(chunk -> {
            buf.writeInt(chunk.x);
            buf.writeInt(chunk.z);
        });

        buf.writeInt(balance);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端更新团队数据
            ClientTeamManager.getInstance().updateTeamData(
                teamId, teamName, leader, members,
                invitedPlayers, memberPermissions, claimedChunks, balance
            );
        });
        return true;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<UUID> getInvitedPlayers() {
        return Collections.unmodifiableSet(invitedPlayers);
    }

    public Map<UUID, String> getMemberPermissions() {
        return Collections.unmodifiableMap(memberPermissions);
    }

    public Set<ChunkPos> getClaimedChunks() {
        return Collections.unmodifiableSet(claimedChunks);
    }

    public int getBalance() {
        return balance;
    }
}