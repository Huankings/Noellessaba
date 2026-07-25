package org.agmas.noellesroles.roles.initiate;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 初学者中途转职用的随机角色选择与公告发送。
 *
 * <p>这里刻意不读取 StupidExpress 原 config：迁入 NoellesRoles 后，初学者的 fallback
 * 改为由“本次死亡的击杀者阵营 / 是否无来源死亡”决定。</p>
 */
public final class InitiateRoleSelector {
    private InitiateRoleSelector() {
    }

    public static @NotNull Role selectRandomKillerRole() {
        List<Role> roles = collectEnabledModdedRoles(Faction.KILLER);
        if (roles.isEmpty()) {
            roles.add(WatheRoles.KILLER);
        }
        Collections.shuffle(roles);
        return roles.getFirst();
    }

    public static @NotNull Role selectRandomGoodOrVigilanteRole() {
        List<Role> roles = new ArrayList<>();
        roles.addAll(collectEnabledModdedRoles(Faction.CIVILIAN));
        roles.addAll(collectEnabledModdedRoles(Faction.VIGILANTE));
        if (roles.isEmpty()) {
            roles.add(WatheRoles.CIVILIAN);
            roles.add(WatheRoles.VIGILANTE);
        }
        Collections.shuffle(roles);
        return roles.getFirst();
    }

    public static @NotNull Role selectRandomNeutralRole() {
        List<Role> roles = collectEnabledModdedRoles(Faction.NEUTRAL);
        roles.remove(NoellesRoleRegistry.INITIATE);
        if (roles.isEmpty()) {
            roles.add(NoellesRoleRegistry.AMNESIAC);
        }
        Collections.shuffle(roles);
        return roles.getFirst();
    }

    public static void transformInitiate(@NotNull ServerPlayerEntity player, @NotNull GameWorldComponent gameWorld, @NotNull Role role) {
        removeInitiateKnife(player);

        /*
         * 中途转职必须走 GameWorldComponent#addRole 和 ModdedRoleAssigned：
         * 前者写入 Wathe 当前身份映射，后者让扩展职业自己的开局物品、组件重置和冷却处理生效。
         */
        gameWorld.addRole(player, role);
        ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, role);

        if (role.equals(WatheRoles.VIGILANTE)) {
            player.giveItemStack(WatheItems.REVOLVER.getDefaultStack());
        }
        sendRoleAnnouncement(player, gameWorld, role);
    }

    public static void removeInitiateKnife(@NotNull ServerPlayerEntity player) {
        /*
         * 原 StupidExpress 会在初学者身份结束时清掉匕首。
         * 这里按物品类型移除全部匕首，避免购买多把或其它交付路径留下可继续使用的考核武器。
         */
        removeKnivesFromList(player.getInventory().main);
        removeKnivesFromList(player.getInventory().offHand);
        removeKnivesFromList(player.getInventory().armor);
        player.getInventory().markDirty();
    }

    private static void removeKnivesFromList(DefaultedList<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isOf(WatheItems.KNIFE)) {
                stacks.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static @NotNull List<Role> collectEnabledModdedRoles(@NotNull Faction faction) {
        List<Role> roles = new ArrayList<>(WatheRoles.ROLES);
        roles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role)
                || role.getFaction() != faction
                || isRoleDisabled(role));
        return roles;
    }

    private static boolean isRoleDisabled(@NotNull Role role) {
        /*
         * Harpy 新版禁用表使用完整 Identifier；旧代码和部分本地配置可能只写 path。
         * 两种都兼容，避免玩家已有配置在迁移后突然失效。
         */
        return HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString())
                || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().getPath());
    }

    private static void sendRoleAnnouncement(@NotNull ServerPlayerEntity player, @NotNull GameWorldComponent gameWorld, @NotNull Role role) {
        RoleAnnouncementTexts.RoleAnnouncementText announcement = resolveAnnouncement(role);
        ServerPlayNetworking.send(
                player,
                new AnnounceWelcomePayload(
                        RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(announcement),
                        gameWorld.getAllKillerTeamPlayers().size(),
                        0
                )
        );
    }

    private static @NotNull RoleAnnouncementTexts.RoleAnnouncementText resolveAnnouncement(@NotNull Role role) {
        if (Harpymodloader.VANNILA_ROLES.contains(role)) {
            if (role.equals(WatheRoles.KILLER)) {
                return RoleAnnouncementTexts.KILLER;
            }
            if (role.equals(WatheRoles.VIGILANTE)) {
                return RoleAnnouncementTexts.VIGILANTE;
            }
            if (role.equals(WatheRoles.LOOSE_END)) {
                return RoleAnnouncementTexts.LOOSE_END;
            }
            return RoleAnnouncementTexts.CIVILIAN;
        }

        RoleAnnouncementTexts.RoleAnnouncementText announcement = Harpymodloader.autogeneratedAnnouncements.get(role);
        if (announcement != null) {
            return announcement;
        }

        /*
         * 理论上 Harpy.refreshRoles 会提前生成扩展职业公告。
         * 如果某个软兼容职业因为加载顺序暂时没公告，使用中立公告兜底并提示本人，不让转职流程崩溃。
         */
        playerSafeFallbackLog(role);
        return RoleAnnouncementTexts.NEUTRAL;
    }

    private static void playerSafeFallbackLog(@NotNull Role role) {
        org.agmas.noellesroles.registry.NoellesRolesCore.LOGGER.warn("初学者转职到 {} 时未找到 Harpy 公告，已使用中立公告兜底。", role.identifier());
    }
}
