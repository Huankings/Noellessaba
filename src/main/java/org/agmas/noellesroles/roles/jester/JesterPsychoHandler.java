package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
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
    private static final int JESTER_PSYCHO_DURATION_TICKS = GameConstants.getInTicks(0, 48);
    public static final int JESTER_INVULNERABLE_END_TICKS = GameConstants.getInTicks(0, 44);

    private JesterPsychoHandler() {
    }

    public static void init() {
        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.jester")
                .durationTicks(JESTER_PSYCHO_DURATION_TICKS)
                .armour(1)
                .build();
        PsychoModeApi.registerProfile(profile);
    }

    public static boolean tryTriggerFromDeath(PlayerEntity victim, @Nullable PlayerEntity killer) {
        if (killer == null) {
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
