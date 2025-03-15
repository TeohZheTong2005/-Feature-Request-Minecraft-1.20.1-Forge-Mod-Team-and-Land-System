package com.dc.dcteam.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DCTeamConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_TEAM_NAME_LENGTH;
    public static final ForgeConfigSpec.IntValue MIN_TEAM_NAME_LENGTH;
    public static final ForgeConfigSpec.IntValue INITIAL_LAND_PRICE;
    public static final ForgeConfigSpec.IntValue MAX_TEAM_LANDS;
    public static final ForgeConfigSpec.IntValue LAND_PRICE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue LAND_CLAIM_REFUND_PERCENTAGE;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MOB_SPAWNING_IN_CLAIMS;
    public static final ForgeConfigSpec.IntValue CLAIM_NOTIFICATION_DURATION;

    static {
        BUILDER.push("Team Settings");
        MIN_TEAM_NAME_LENGTH = BUILDER
                .comment("团队名称的最小长度 (范围：2-6个字符，过短的名称可能难以识别)")
                .defineInRange("minTeamNameLength", 2, 2, 6);
        MAX_TEAM_NAME_LENGTH = BUILDER
                .comment("团队名称的最大长度 (范围：6-16个字符，过长的名称可能影响显示)")
                .defineInRange("maxTeamNameLength", 12, 6, 16);
        BUILDER.pop();

        BUILDER.push("Land Claim Settings");
        INITIAL_LAND_PRICE = BUILDER
                .comment("声明一个区块的初始价格 (范围：50-5000，较低的价格有利于新团队发展)")
                .defineInRange("initialLandPrice", 200, 50, 5000);
        MAX_TEAM_LANDS = BUILDER
                .comment("每个团队最大可声明的区块数量 (范围：1-500，0将被视为50，过多的区块可能影响服务器性能)")
                .defineInRange("maxTeamLands", 50, 1, 500);
        LAND_PRICE_MULTIPLIER = BUILDER
                .comment("每次声明新区块时价格的增长倍数 (范围：1.0-5.0，建议使用1.2-2.0，过高的倍数可能导致价格增长过快)")
                .defineInRange("landPriceMultiplier", 1.5, 1.0, 5.0);
        LAND_CLAIM_REFUND_PERCENTAGE = BUILDER
                .comment("取消声明区块时的退款百分比 (范围：0-90，建议50-70，过高的退款比例可能被滥用)")
                .defineInRange("landClaimRefundPercentage", 60, 0, 90);
        DISABLE_MOB_SPAWNING_IN_CLAIMS = BUILDER
                .comment("是否在已声明的区块中禁止敌对生物生成 (true=禁止生成，可以保护团队领地的安全)")
                .define("disableMobSpawningInClaims", true);
        CLAIM_NOTIFICATION_DURATION = BUILDER
                .comment("区块声明通知的显示时间（秒）(范围：1-10，建议3-5秒，过长可能影响游戏体验)")
                .defineInRange("claimNotificationDuration", 4, 1, 10);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}