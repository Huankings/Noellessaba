package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

import java.util.List;
import java.util.UUID;

/**
 * 影子小丑阶段状态机的服务端主逻辑。
 *
 * <p>这里集中处理“进入某阶段时一定要同步发生”的副作用：
 * 发/收物品、清任务、记录回放、发 actionbar、转狂信者、第四阶段主题等。
 * 这样死亡、任务、能力键和胜利规则只需要调用明确的方法，不会在各处重复写阶段副作用。</p>
 */
public final class ShadowJesterManager {
    private ShadowJesterManager() {
    }

    public static void tickWorld(ServerWorld world) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(world);
        tickPendingOfflineDeaths(world, component);
        if (!component.hasPair()) {
            return;
        }

        ServerPlayerEntity first = player(world, component.first());
        ServerPlayerEntity second = player(world, component.second());
        refreshConfirmedDeathIfDebugRevived(component, first);
        refreshConfirmedDeathIfDebugRevived(component, second);

        if (component.tickVowRequest()) {
            sendActionbar(first, "message.noellesroles.shadow_jester.vow_expired");
            sendActionbar(second, "message.noellesroles.shadow_jester.vow_expired");
        }

        tickOnlinePartner(world, component, first);
        tickOnlinePartner(world, component, second);
        handleMissingPartnerInEarlyPhases(world, component, first, second);
        handleMissingPartnerAfterVow(world, component, first, second);
        maybeEnterPhaseFour(world, component);
    }

    private static void tickOnlinePartner(ServerWorld world, ShadowJesterComponent component, ServerPlayerEntity player) {
        if (!isActiveAlive(player)) {
            return;
        }
        ShadowJesterTaskHandler.tickTaskRefill(player, component);

        Identifier pendingDeath = component.consumePendingOfflineDeath(player.getUuid());
        if (pendingDeath != null) {
            GameFunctions.killPlayer(player, true, null, pendingDeath);
        }
    }

    private static void tickPendingOfflineDeaths(ServerWorld world, ShadowJesterComponent component) {
        /*
         * 离线待处理死亡不能依赖当前仍有影子小丑配对。
         * 第一/第二阶段一方离线时，在线者会立刻转狂信者并拆掉 pair；
         * 但离线者重连后仍应按 mental_breakdown / broken_heart 补走死亡流程，避免用掉线逃避机制。
         */
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!isActiveAlive(player)) {
                continue;
            }
            Identifier pendingDeath = component.consumePendingOfflineDeath(player.getUuid());
            if (pendingDeath != null) {
                GameFunctions.killPlayer(player, true, null, pendingDeath);
            }
        }
    }

    private static void handleMissingPartnerInEarlyPhases(
            ServerWorld world,
            ShadowJesterComponent component,
            ServerPlayerEntity first,
            ServerPlayerEntity second
    ) {
        if (!ShadowJesterConstants.CONVERT_TO_JESTER_WHEN_EARLY_PARTNER_MISSING) {
            /*
             * 调试模式：关闭“tick 扫描发现另一半不活跃就转狂信”。
             * 这样管理员手动切创造/旁观或临时断线时，不会破坏影子小丑阶段状态；
             * 真正有死因的死亡仍会走 ShadowJesterDeathHandler 的 confirmed death 分支。
             */
            return;
        }
        if (!component.hasPair()) {
            return;
        }
        UUID firstUuid = component.first();
        UUID secondUuid = component.second();
        if (firstUuid == null || secondUuid == null) {
            return;
        }

        /*
         * 第一/第二阶段仍在“选择前”的脆弱关系：
         * 任意一方死亡或离线，另一方不再能共同胜利，立即转为狂信者，并清掉自己剩余任务。
         */
        boolean firstEarly = component.getPhase(firstUuid).id() <= ShadowJesterPhase.CHOICE.id();
        boolean secondEarly = component.getPhase(secondUuid).id() <= ShadowJesterPhase.CHOICE.id();
        if (!firstEarly || !secondEarly) {
            return;
        }

        boolean firstAlive = isActiveAlive(first);
        boolean secondAlive = isActiveAlive(second);
        if (firstAlive && !secondAlive) {
            transformToJester(first, true);
            if (second == null) {
                component.markPendingOfflineDeath(secondUuid, NoellesDeathReasons.MENTAL_BREAKDOWN_DEATH_REASON);
            }
            component.removePairKeepPendingDeaths();
        } else if (secondAlive && !firstAlive) {
            transformToJester(second, true);
            if (first == null) {
                component.markPendingOfflineDeath(firstUuid, NoellesDeathReasons.MENTAL_BREAKDOWN_DEATH_REASON);
            }
            component.removePairKeepPendingDeaths();
        }
    }

    private static void handleMissingPartnerAfterVow(
            ServerWorld world,
            ShadowJesterComponent component,
            ServerPlayerEntity first,
            ServerPlayerEntity second
    ) {
        if (!ShadowJesterConstants.KILL_BOUND_PARTNER_WHEN_PARTNER_MISSING) {
            /*
             * 调试模式：关闭“tick 扫描发现另一半不活跃就殉情”。
             * 这只影响离线、创造旁观等没有明确死因的状态；真正死亡仍由 DeathHandler 负责联动。
             */
            return;
        }
        if (!component.hasPair()) {
            return;
        }
        UUID firstUuid = component.first();
        UUID secondUuid = component.second();
        if (firstUuid == null || secondUuid == null
                || !component.getPhase(firstUuid).atLeast(ShadowJesterPhase.VOW_BOUND)
                || !component.getPhase(secondUuid).atLeast(ShadowJesterPhase.VOW_BOUND)) {
            return;
        }

        /*
         * 第三阶段以后影子小丑已经正式绑定，共进退优先于任务/选择逻辑。
         * 这里把“离线”和“已经死亡/进入时间狭缝”都视为不能继续并肩作战：
         * - 真实死亡会由 DeathHandler 立即杀死另一半；
         * - 若死亡后被时停者拉进狭缝，GameFunctions 会把玩家临时算作存活，
         *   所以这里额外排除 TimekeeperPlayerComponent#isInTimeRift，避免殉情被狭缝延迟。
         */
        boolean firstAlive = isActiveAlive(first);
        boolean secondAlive = isActiveAlive(second);
        if (firstAlive && !secondAlive) {
            killBondedSurvivor(first, secondUuid, second == null ? component : null);
        } else if (secondAlive && !firstAlive) {
            killBondedSurvivor(second, firstUuid, first == null ? component : null);
        }
    }

    private static void killBondedSurvivor(
            ServerPlayerEntity survivor,
            UUID missingPartnerUuid,
            ShadowJesterComponent offlinePartnerComponent
    ) {
        if (offlinePartnerComponent != null) {
            /*
             * 离线的一方无法立刻进入死亡流程。先记录待处理死因，
             * 等其重新上线并同步到服务端玩家对象后再补一次 broken_heart，避免掉线规避绑定代价。
             */
            offlinePartnerComponent.markPendingOfflineDeath(missingPartnerUuid, NoellesDeathReasons.BROKEN_HEART_DEATH_REASON);
        }

        NbtCompound extra = new NbtCompound();
        extra.putUuid("broken_heart_partner", missingPartnerUuid);
        GameFunctions.killPlayer(survivor, true, null, NoellesDeathReasons.BROKEN_HEART_DEATH_REASON, extra);
    }

    public static void enterPhaseTwo(ServerPlayerEntity player) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getServerWorld());
        if (!component.contains(player.getUuid()) || component.getPhase(player.getUuid()) != ShadowJesterPhase.TASKS) {
            return;
        }

        component.setPhase(player.getUuid(), ShadowJesterPhase.CHOICE);
        player.getInventory().offerOrDrop(WatheItems.KNIFE.getDefaultStack());
        recordStage(player, ShadowJesterConstants.PHASE_TWO_TEXT_KEY, ShadowJesterConstants.PHASE_TWO_DEFINITION_KEY);
    }

    public static void handleAbilityKey(ServerPlayerEntity player, int targetId) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getServerWorld());
        UUID partnerUuid = component.getPartner(player.getUuid());
        if (partnerUuid == null || component.getPhase(player.getUuid()) != ShadowJesterPhase.CHOICE) {
            return;
        }

        ServerPlayerEntity target = null;
        if (targetId > 0 && player.getServerWorld().getEntityById(targetId) instanceof ServerPlayerEntity serverTarget) {
            target = serverTarget;
        }
        if (target == null || !target.getUuid().equals(partnerUuid) || player.squaredDistanceTo(target) > ShadowJesterConstants.VOW_TARGET_RANGE_SQUARED) {
            return;
        }
        if (component.getPhase(target.getUuid()) != ShadowJesterPhase.CHOICE) {
            sendActionbar(player, "message.noellesroles.shadow_jester.partner_not_ready");
            return;
        }

        if (component.isRequestTo(player.getUuid()) && component.isRequestFrom(target.getUuid())) {
            acceptVow(player, target);
            return;
        }
        component.startVowRequest(player.getUuid(), target.getUuid());
        sendActionbar(player, "message.noellesroles.shadow_jester.vow_sent", target.getDisplayName());
        sendActionbar(target, "message.noellesroles.shadow_jester.vow_received", player.getDisplayName());
    }

    private static void acceptVow(ServerPlayerEntity acceptor, ServerPlayerEntity requester) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(acceptor.getServerWorld());
        component.clearVowRequest(true);
        component.setPhase(acceptor.getUuid(), ShadowJesterPhase.VOW_BOUND);
        component.setPhase(requester.getUuid(), ShadowJesterPhase.VOW_BOUND);
        removeOneItem(acceptor, WatheItems.KNIFE);
        removeOneItem(requester, WatheItems.KNIFE);
        acceptor.getInventory().offerOrDrop(WatheItems.REVOLVER.getDefaultStack());
        requester.getInventory().offerOrDrop(WatheItems.LOCKPICK.getDefaultStack());
        sendActionbar(acceptor, "message.noellesroles.shadow_jester.vow_accepted_self", requester.getDisplayName());
        sendActionbar(requester, "message.noellesroles.shadow_jester.vow_accepted_other", acceptor.getDisplayName());
        recordStage(acceptor, ShadowJesterConstants.PHASE_THREE_TEXT_KEY, ShadowJesterConstants.PHASE_THREE_DEFINITION_KEY);
        recordStage(requester, ShadowJesterConstants.PHASE_THREE_TEXT_KEY, ShadowJesterConstants.PHASE_THREE_DEFINITION_KEY);
    }

    public static void transformToJester(ServerPlayerEntity player, boolean clearTasks) {
        if (clearTasks) {
            ShadowJesterTaskHandler.clearAllTasks(player);
        }
        removeOneItem(player, WatheItems.KNIFE);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getServerWorld());
        gameWorld.addRole(player, NoellesRoleRegistry.JESTER);
        ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, NoellesRoleRegistry.JESTER);
        PlayerPoisonComponent.KEY.get(player).reset();
        sendWelcome(player, NoellesRoleRegistry.JESTER);
    }

    public static void enterPhaseFour(ServerWorld world, ShadowJesterMusicTheme theme) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(world);
        if (!component.hasPair() || component.getPhaseFourTheme() != ShadowJesterMusicTheme.NONE) {
            return;
        }
        if (component.areBothPairMembersConfirmedOrPendingDeath()) {
            /*
             * 这里只拦“双方已经有明确死亡事实”的情况。
             * creative / spectator 调试状态虽然会让 Wathe 的 alivePlayers 看不到该玩家，
             * 但它没有 DeathApi confirmed death，不应该阻止第三阶段进入谢幕时刻。
             */
            return;
        }

        component.setPhaseFourTheme(theme);
        for (UUID uuid : List.of(component.first(), component.second())) {
            if (uuid == null) {
                continue;
            }
            component.setPhase(uuid, ShadowJesterPhase.CURTAIN_CALL);
            ServerPlayerEntity player = player(world, uuid);
            if (player != null) {
                player.getItemCooldownManager().remove(WatheItems.REVOLVER);
                giveIfMissing(player, WatheItems.REVOLVER);
                giveIfMissing(player, WatheItems.LOCKPICK);
                giveIfMissing(player, WatheItems.CROWBAR);
                recordStage(player, ShadowJesterConstants.PHASE_FOUR_TEXT_KEY, ShadowJesterConstants.PHASE_FOUR_DEFINITION_KEY);
            }
        }
    }

    private static void maybeEnterPhaseFour(ServerWorld world, ShadowJesterComponent component) {
        if (!component.hasPair() || component.getPhaseFourTheme() != ShadowJesterMusicTheme.NONE) {
            return;
        }
        UUID first = component.first();
        UUID second = component.second();
        if (first == null || second == null
                || !component.getPhase(first).atLeast(ShadowJesterPhase.VOW_BOUND)
                || !component.getPhase(second).atLeast(ShadowJesterPhase.VOW_BOUND)) {
            return;
        }
        if (component.areBothPairMembersConfirmedOrPendingDeath()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        List<ServerPlayerEntity> alive = world.getPlayers().stream()
                .filter(GameFunctions::isPlayerAliveAndSurvival)
                .filter(player -> !component.contains(player.getUuid()))
                .toList();
        boolean civiliansAlive = alive.stream().anyMatch(player -> {
            Role role = gameWorld.getRole(player);
            return role != null && (role.isInnocent() || role.getFaction() == dev.doctor4t.wathe.api.Faction.VIGILANTE);
        });
        boolean killersAlive = alive.stream().anyMatch(player -> {
            Role role = gameWorld.getRole(player);
            return role != null && role.canUseKiller();
        });
        if (!civiliansAlive) {
            enterPhaseFour(world, ShadowJesterMusicTheme.KING);
        } else if (!killersAlive) {
            enterPhaseFour(world, ShadowJesterMusicTheme.QUEEN);
        }
    }

    public static void recordStage(ServerPlayerEntity player, String phaseKey, String definitionKey) {
        NbtCompound extra = new NbtCompound();
        extra.putString("phase", phaseKey);
        extra.putString("definition", definitionKey);
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.SHADOW_JESTER_STAGE_EVENT, player, extra);
    }

    private static void sendWelcome(ServerPlayerEntity player, Role role) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getServerWorld());
        if (Harpymodloader.autogeneratedAnnouncements.containsKey(role)) {
            ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                    RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(Harpymodloader.autogeneratedAnnouncements.get(role)),
                    gameWorld.getAllKillerTeamPlayers().size(),
                    0
            ));
        } else if (role == WatheRoles.KILLER) {
            ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                    RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(RoleAnnouncementTexts.KILLER),
                    gameWorld.getAllKillerTeamPlayers().size(),
                    0
            ));
        }
    }

    private static void giveIfMissing(ServerPlayerEntity player, Item item) {
        if (!player.getInventory().contains(new ItemStack(item))) {
            player.getInventory().offerOrDrop(item.getDefaultStack());
        }
    }

    public static void removeOneItem(ServerPlayerEntity player, Item item) {
        player.getInventory().remove(stack -> stack.isOf(item), 1, player.getInventory());
    }

    private static void sendActionbar(ServerPlayerEntity player, String key, Object... args) {
        if (player != null) {
            player.sendMessage(Text.translatable(key, args).withColor(ShadowJesterConstants.ROLE_COLOR), true);
        }
    }

    private static ServerPlayerEntity player(ServerWorld world, UUID uuid) {
        return uuid == null ? null : world.getServer().getPlayerManager().getPlayer(uuid);
    }

    private static boolean isActiveAlive(ServerPlayerEntity player) {
        return player != null
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift();
    }

    private static void refreshConfirmedDeathIfDebugRevived(ShadowJesterComponent component, ServerPlayerEntity player) {
        if (player == null || !component.isConfirmedDead(player.getUuid())) {
            return;
        }
        if (isActiveAlive(player)) {
            /*
             * 管理员调试时可能把已经死亡的影子小丑重新切回 adventure/survival。
             * 这类“手动复活”没有 Wathe 回溯快照参与，所以在服务端 tick 里主动清掉死亡标记，
             * 让后续第四阶段、胜利阻拦和玩家本体变形重新按缔结誓言继续运转。
             */
            component.setConfirmedDead(player.getUuid(), false);
        }
    }
}
