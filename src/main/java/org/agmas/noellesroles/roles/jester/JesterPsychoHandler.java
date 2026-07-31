package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.Nullable;

/**
 * 狂信者自己的疯魔接入。
 *
 * <p>旧版通过 mixin 进入 {@code GameFunctions.killPlayer} 后手动改 Wathe 的
 * {@code psychoTicks/armour} 字段。现在改为注册专属 profile，再通过 API 启动；
 * 这样持续时间、护盾、授予物品、皮肤、声音和结束回放名称都由 Wathe 统一维护。</p>
 */
public final class JesterPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("jester_psycho");
    public static final int JESTER_INVULNERABLE_END_TICKS = JesterConstants.INVULNERABLE_END_TICKS;

    private JesterPsychoHandler() {
    }

    public static void init() {
        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.jester")
                .durationTicks(JesterConstants.PSYCHO_DURATION_TICKS)
                /*
                 * 注册表里的 profile 保存“只有 1 个杀手时”的默认护盾。
                 * 真正启动疯魔时还会经过下面的 start profile provider，
                 * 根据本局杀手数量临时复制同 id profile 并替换 armour。
                 * 这样 Wathe 仍然只看到 noellesroles:jester_psycho 这一个稳定 profile id，
                 * 回放、皮肤、临时球棒标记和客户端图标都不会因为护盾层数不同而分叉。
                 */
                .armour(JesterConstants.INITIAL_PSYCHO_SHIELD_LAYERS)
                .build();
        PsychoModeApi.registerProfile(profile);

        PsychoModeApi.registerStartProfileProvider(NoellesRolesCore.id("jester/dynamic_psycho_shields"), PsychoModeApi.DEFAULT_PRIORITY + 100, (player, requestedProfile) -> {
            if (!requestedProfile.id().equals(PROFILE_ID)) {
                return null;
            }

            /*
             * 狂信者护盾在“疯魔真正启动的那一刻”读取杀手人数。
             * 不能提前在 init() 里写死，因为 init() 发生在服务器启动阶段，
             * 那时还不知道这一局到底有几个杀手位。
             */
            int shieldLayers = JesterConstants.getPsychoShieldLayers(player);
            return PsychoModeProfile.copyOf(requestedProfile, PROFILE_ID)
                    .armour(shieldLayers)
                    .build();
        });
    }

    public static boolean tryTriggerFromDeath(PlayerEntity victim, @Nullable PlayerEntity killer) {
        if (killer == null) {
            return false;
        }

        /*
         * 狂信者疯魔触发总开关。
         * 当常量配置为 false 时，这里直接放弃启动 profile；
         * JesterDeathProtectionHandler 会继续向后返回 true，Wathe/Noelles 的普通死亡流程
         * 就会照常杀死狂信者，而不会再取消死亡事件。
         */
        if (!JesterConstants.TRIGGER_PSYCHO_WHEN_KILLED_BY_INNOCENT) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.isRole(victim, NoellesRoleRegistry.JESTER)
                || gameWorld.isRole(killer, NoellesRoleRegistry.JESTER)
                || !gameWorld.isInnocent(killer)
                || PsychoModeApi.isActive(victim)) {
            return false;
        }

        if (!PsychoModeApi.start(victim, PROFILE_ID)) {
            return false;
        }

        if (victim instanceof ServerPlayerEntity serverVictim) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("victim", victim.getUuid());
            GameRecordManager.recordGlobalEvent(serverVictim.getServerWorld(), NoellesEventIds.JESTER_PSYCHO_STARTED_EVENT, null, extra);
        }
        return true;
    }
}
