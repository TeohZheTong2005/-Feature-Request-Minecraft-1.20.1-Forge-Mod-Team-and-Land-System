package com.dc.dcteam.network;

import com.dc.dcteam.DCTeamMod;
import com.dc.dcteam.team.Team;
import com.dc.dcteam.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;

public class TeamPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DCTeamMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // 创建队伍请求
        INSTANCE.messageBuilder(CreateTeamPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreateTeamPacket::encode)
                .decoder(CreateTeamPacket::new)
                .consumerMainThread(CreateTeamPacket::handle)
                .add();

        // 邀请玩家请求
        INSTANCE.messageBuilder(InvitePlayerPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(InvitePlayerPacket::encode)
                .decoder(InvitePlayerPacket::new)
                .consumerMainThread(InvitePlayerPacket::handle)
                .add();

        // 加入队伍请求
        INSTANCE.messageBuilder(JoinTeamPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JoinTeamPacket::encode)
                .decoder(JoinTeamPacket::new)
                .consumerMainThread(JoinTeamPacket::handle)
                .add();

        // 领地声明请求
        INSTANCE.messageBuilder(ClaimLandPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClaimLandPacket::encode)
                .decoder(ClaimLandPacket::new)
                .consumerMainThread(ClaimLandPacket::handle)
                .add();

        // 存款请求
        INSTANCE.messageBuilder(DepositMoneyPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DepositMoneyPacket::encode)
                .decoder(DepositMoneyPacket::new)
                .consumerMainThread(DepositMoneyPacket::handle)
                .add();

        // 同步团队数据到客户端
        INSTANCE.messageBuilder(SyncTeamDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTeamDataPacket::encode)
                .decoder(SyncTeamDataPacket::new)
                .consumerMainThread(SyncTeamDataPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
}