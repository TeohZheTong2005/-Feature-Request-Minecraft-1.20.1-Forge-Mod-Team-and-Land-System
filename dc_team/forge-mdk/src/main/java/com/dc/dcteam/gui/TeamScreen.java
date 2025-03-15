package com.dc.dcteam.gui;

import com.dc.dcteam.team.Team;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

public class TeamScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("dcteam", "textures/gui/team_screen.png");
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 166;

    private EditBox teamNameField;
    private Button createTeamButton;
    private Button invitePlayerButton;
    private Button manageTeamButton;
    private Button claimLandButton;
    private Button depositMoneyButton;

    private int leftPos;
    private int topPos;

    public TeamScreen() {
        super(Component.translatable("gui.dcteam.team.title"));
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - BACKGROUND_WIDTH) / 2;
        topPos = (height - BACKGROUND_HEIGHT) / 2;

        // 创建队伍名称输入框
        teamNameField = new EditBox(font, leftPos + 20, topPos + 30, 160, 20,
                Component.translatable("gui.dcteam.team.name"));
        teamNameField.setMaxLength(16);
        addRenderableWidget(teamNameField);

        // 创建各种按钮
        createTeamButton = Button.builder(Component.translatable("gui.dcteam.team.create"), button -> {
            String teamName = teamNameField.getValue().trim();
            if (teamName.length() < DCTeamConfig.MIN_TEAM_NAME_LENGTH.get() || 
                teamName.length() > DCTeamConfig.MAX_TEAM_NAME_LENGTH.get()) {
                minecraft.player.sendSystemMessage(Component.translatable("message.dcteam.error.invalid_team_name_length"));
                return;
            }
            // 发送创建团队请求到服务器
            TeamPacketHandler.sendToServer(new CreateTeamPacket(teamName));
        }).bounds(leftPos + 20, topPos + 60, 160, 20).build();
        addRenderableWidget(createTeamButton);

        invitePlayerButton = Button.builder(Component.translatable("gui.dcteam.team.invite"), button -> {
            // 打开玩家选择界面
            minecraft.setScreen(new PlayerSelectScreen(this, player -> {
                TeamPacketHandler.sendToServer(new InvitePlayerPacket(player.getUUID()));
            }));
        }).bounds(leftPos + 20, topPos + 85, 160, 20).build();
        addRenderableWidget(invitePlayerButton);

        manageTeamButton = Button.builder(Component.translatable("gui.dcteam.team.manage"), button -> {
            if (minecraft.player != null) {
                // 打开团队管理界面
                minecraft.setScreen(new TeamManageScreen(this, team -> {
                    // 发送团队更新请求到服务器
                    TeamPacketHandler.sendToServer(new UpdateTeamPacket(team));
                }));
            }
        }).bounds(leftPos + 20, topPos + 110, 160, 20).build();
        addRenderableWidget(manageTeamButton);

        claimLandButton = Button.builder(Component.translatable("gui.dcteam.team.claim_land"), button -> {
            if (minecraft.player != null) {
                ChunkPos currentChunk = new ChunkPos(minecraft.player.blockPosition());
                TeamPacketHandler.sendToServer(new ClaimLandPacket(currentChunk));
            }
        }).bounds(leftPos + 20, topPos + 135, 160, 20).build();
        addRenderableWidget(claimLandButton);

        depositMoneyButton = Button.builder(Component.translatable("gui.dcteam.team.deposit"), button -> {
            // 打开存款输入界面
            minecraft.setScreen(new MoneyInputScreen(this, amount -> {
                TeamPacketHandler.sendToServer(new DepositMoneyPacket(amount));
            }));
        }).bounds(leftPos + 20, topPos + 160, 160, 20).build();
        addRenderableWidget(depositMoneyButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染标题
        guiGraphics.drawCenteredString(font, title, leftPos + BACKGROUND_WIDTH / 2, topPos + 10, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}