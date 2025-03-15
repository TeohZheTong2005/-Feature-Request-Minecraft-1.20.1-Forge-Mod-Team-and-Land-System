package com.dc.dcteam.team;

public enum TeamPermission {
    LEADER(true, true, true, true, true, true, true, true),    // 队长权限：完全控制权限
    ADMIN(true, true, false, false, false, true, true, true),  // 管理员权限：可以邀请玩家、管理领地
    MEMBER(false, false, false, false, false, true, true, true), // 普通成员权限：基本操作权限
    GUEST(false, false, false, false, false, false, false, true); // 访客权限：仅限查看

    private final boolean canInvite;
    private final boolean canManageLand;
    private final boolean canManageMembers;
    private final boolean canManagePermissions;
    private final boolean canManageSettings;
    private final boolean canDeposit;
    private final boolean canUseFacilities;
    private final boolean canView;

    TeamPermission(boolean canInvite, boolean canManageLand, boolean canManageMembers,
                   boolean canManagePermissions, boolean canManageSettings,
                   boolean canDeposit, boolean canUseFacilities, boolean canView) {
        this.canInvite = canInvite;
        this.canManageLand = canManageLand;
        this.canManageMembers = canManageMembers;
        this.canManagePermissions = canManagePermissions;
        this.canManageSettings = canManageSettings;
        this.canDeposit = canDeposit;
        this.canUseFacilities = canUseFacilities;
        this.canView = canView;
    }

    public boolean canInvitePlayers() {
        return this.canInvite;
    }

    public boolean canManageLands() {
        return this.canManageLand;
    }

    public boolean canManageMembers() {
        return this.canManageMembers;
    }

    public boolean canManagePermissions() {
        return this.canManagePermissions;
    }

    public boolean canManageTeamSettings() {
        return this.canManageSettings;
    }

    public boolean canDepositMoney() {
        return this.canDeposit;
    }

    public boolean canUseTeamFacilities() {
        return this.canUseFacilities;
    }

    public boolean canView() {
        return this.canView;
    }

    public boolean hasPermission(TeamPermission required) {
        return this.ordinal() <= required.ordinal();
    }
}