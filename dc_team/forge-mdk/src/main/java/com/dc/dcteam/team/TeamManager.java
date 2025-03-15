package com.dc.dcteam.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class TeamManager extends SavedData {
    private static final String DATA_NAME = "dcteam_teams";
    private final Map<UUID, Team> teams;
    private final Map<ChunkPos, UUID> claimedChunks;

    public TeamManager() {
        teams = new HashMap<>();
        claimedChunks = new HashMap<>();
    }

    public static TeamManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            TeamManager::load,
            TeamManager::new,
            DATA_NAME
        );
    }

    public Team createTeam(String name, UUID leader) {
        Team team = new Team(name, leader);
        teams.put(team.getId(), team);
        setDirty();
        return team;
    }

    public void deleteTeam(UUID teamId) {
        Team team = teams.remove(teamId);
        if (team != null) {
            // 移除所有已声明的区块
            Set<ChunkPos> teamChunks = new HashSet<>(team.getClaimedChunks());
            teamChunks.forEach(chunk -> {
                claimedChunks.remove(chunk);
                team.removeClaimedChunk(chunk);
            });
            setDirty();
        }
    }

    public Team getTeam(UUID teamId) {
        return teams.get(teamId);
    }

    public Team getTeamByMember(UUID playerId) {
        return teams.values().stream()
                .filter(team -> team.isMember(playerId))
                .findFirst()
                .orElse(null);
    }

    private static final int MAX_CHUNKS_PER_TEAM = 100; // 每个团队最多可以声明100个区块

    public boolean claimChunk(Team team, ChunkPos chunk, UUID playerId) {
        // 检查玩家是否有权限声明区块
        if (!team.getPermission(playerId).canManageChunks()) {
            return false;
        }

        // 检查团队是否已达到区块上限
        if (team.getClaimedChunks().size() >= MAX_CHUNKS_PER_TEAM) {
            return false;
        }

        // 检查区块是否已被其他团队声明
        if (!claimedChunks.containsKey(chunk) && team.addClaimedChunk(chunk)) {
            claimedChunks.put(chunk, team.getId());
            setDirty();
            return true;
        }
        return false;
    }

    public boolean unclaimChunk(Team team, ChunkPos chunk, UUID playerId) {
        // 检查玩家是否有权限取消声明区块
        if (!team.getPermission(playerId).canManageChunks()) {
            return false;
        }

        UUID teamId = claimedChunks.get(chunk);
        if (teamId != null && teamId.equals(team.getId()) && team.removeClaimedChunk(chunk)) {
            claimedChunks.remove(chunk);
            setDirty();
            return true;
        }
        return false;
    }

    public Team getChunkOwner(ChunkPos chunk) {
        UUID teamId = claimedChunks.get(chunk);
        return teamId != null ? teams.get(teamId) : null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag teamsList = new ListTag();
        teams.values().forEach(team -> {
            CompoundTag teamTag = team.save();
            teamsList.add(teamTag);
        });
        tag.put("teams", teamsList);
        return tag;
    }

    public static TeamManager load(CompoundTag tag) {
        TeamManager manager = new TeamManager();
        ListTag teamsList = tag.getList("teams", 10); // 10 is the NBT tag type for CompoundTag

        teamsList.forEach(teamTag -> {
            Team team = Team.load((CompoundTag) teamTag);
            manager.teams.put(team.getId(), team);
            // 同步团队的已声明区块到全局映射
            team.getClaimedChunks().forEach(chunk -> 
                manager.claimedChunks.put(chunk, team.getId()));
        });

        return manager;
    }
}