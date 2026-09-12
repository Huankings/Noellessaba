package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.Faction;
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
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;

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
        /*
         * 回溯播放期间，ShadowJesterComponent 会被逐帧恢复。
         * 离线死亡补处理、早期阶段转狂信、缔结殉情和任务补发都属于正常时间线副作用，
         * 不能在历史帧上再次执行，否则会把刚恢复的 pair 又拆掉或重新清空进度。
         */
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return;
        }
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(world);
        tickPendingOfflineDeaths(world, component);
        if (!component.hasPair()) {
            return;
        }

        /*
         * 亡命时刻拥有最高优先级。影子小丑仍保留第四阶段本身，
         * 这里只暂停第四阶段新增的音乐、补给与左轮冷却，第三阶段誓言机制不受影响。
         */
        if (component.getPhaseFourTheme() != ShadowJesterMusicTheme.NONE) {
            if (OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
                pausePhaseFourForOutlaw(world);
            } else {
                resumePhaseFourAfterOutlaw(world);
            }
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

    /**
     * 在时停回溯最后一张历史帧应用后，修复影子小丑职业映射与世界级 pair 状态的短暂错位。
     *
     * <p>GameWorldComponent 不整体回滚，避免角色变更事件和其它全局配置产生副作用。
     * 这里严格只处理用户要求的情况：恢复后的 pair 成员当前职业恰好是 JESTER 时，
     * 才静默恢复为 SHADOW_JESTER。不会覆盖其它特殊转职，也不会触发职业分配事件。</p>
     */
    public static void reconcileAfterRewind(ServerWorld world) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(world);
        if (!component.hasPair()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        boolean changed = false;
        for (UUID uuid : List.of(component.first(), component.second())) {
            if (uuid == null || gameWorld.getRole(uuid) != NoellesRoleRegistry.JESTER) {
                continue;
            }

            /*
             * 直接改可变角色表而不调用 addRole：这是回溯状态收束，不是新的游戏内转职。
             * addRole 会记录角色变化 replay，且随后若触发 ModdedRoleAssigned 会重发/清理物品；
             * 最终历史帧已经包含正确的阶段、任务、背包和毒素状态，不能再次初始化。
             */
            gameWorld.getRoles().put(uuid, NoellesRoleRegistry.SHADOW_JESTER);
            /*
             * 运行时职业已经修复后，还要把这次“时间线分支”同步给 Wathe 回放缓存。
             * 该事件不会单独显示，但会阻止回放继续沿用倒流前的 JESTER 职业。
             */
            GameRecordManager.recordRoleCorrection(
                    world,
                    uuid,
                    NoellesRoleRegistry.JESTER,
                    NoellesRoleRegistry.SHADOW_JESTER
            );
            changed = true;
        }
        if (changed) {
            gameWorld.sync();
        }
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
        if (OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
            /* 条件已经满足但亡命时刻仍在持续时先不进入，最后一名亡命徒死亡后会重新计算。 */
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
                giveCurtainCallItemIfMissing(player, WatheItems.REVOLVER);
                giveCurtainCallItemIfMissing(player, WatheItems.LOCKPICK);
                giveCurtainCallItemIfMissing(player, WatheItems.CROWBAR);
                recordStage(player, ShadowJesterConstants.PHASE_FOUR_TEXT_KEY, ShadowJesterConstants.PHASE_FOUR_DEFINITION_KEY);
            }
        }
    }

    public static void pausePhaseFourForOutlaw(ServerWorld world) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(world);
        if (component.getPhaseFourTheme() == ShadowJesterMusicTheme.NONE) {
            return;
        }

        boolean newlySuspended = !component.isPhaseFourSuspended();
        if (newlySuspended) {
            component.setPhaseFourSuspended(true);
            removeCurtainCallGrantedItems(world);
            for (UUID uuid : List.of(component.first(), component.second())) {
                ServerPlayerEntity player = player(world, uuid);
                if (player != null && player.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER)) {
                    /* 当前剩余冷却无法通过公开 API 精确读取；暂停时改回该玩家的 Wathe 正常左轮冷却。 */
                    player.getItemCooldownManager().set(WatheItems.REVOLVER, dev.doctor4t.wathe.game.GameConstants.getRevolverCooldown(player));
                }
            }
        } else {
            /* 暂停期间重新上线的玩家，其持久化背包也必须继续接受临时物品回收。 */
            removeCurtainCallGrantedItemsFromInventories(world);
        }
    }

    public static void resumePhaseFourAfterOutlaw(ServerWorld world) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(world);
        if (!component.isPhaseFourSuspended()
                || component.getPhaseFourTheme() == ShadowJesterMusicTheme.NONE
                || component.areBothPairMembersConfirmedOrPendingDeath()) {
            return;
        }

        component.setPhaseFourSuspended(false);
        for (UUID uuid : List.of(component.first(), component.second())) {
            ServerPlayerEntity player = player(world, uuid);
            if (player == null) {
                continue;
            }
            player.getItemCooldownManager().remove(WatheItems.REVOLVER);
            giveCurtainCallItemIfMissing(player, WatheItems.REVOLVER);
            giveCurtainCallItemIfMissing(player, WatheItems.LOCKPICK);
            giveCurtainCallItemIfMissing(player, WatheItems.CROWBAR);
        }
        /* 恢复的是同一次谢幕时刻，按需求不重复记录第四阶段进入回放。 */
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
        if (OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        List<ServerPlayerEntity> alive = world.getPlayers().stream()
                /*
                 * 时间狭缝会把刚死亡的玩家临时标记为 Wathe 的“特殊存活旁观”，
                 * 单独使用 GameFunctions.isPlayerAliveAndSurvival 会让已经死亡的最后一名杀手
                 * 在 30 秒狭缝期间继续卡住第四阶段。谢幕时刻判断的是阵营是否已经死亡，
                 * 因此这里必须把处于时间狭缝的玩家排除。
                 */
                .filter(ShadowJesterManager::isActiveAlive)
                .filter(player -> !component.contains(player.getUuid()))
                .toList();
        boolean civiliansAlive = alive.stream().anyMatch(player -> {
            Role role = gameWorld.getRole(player);
            /* 好人阵营严格只包含 Wathe 显式注册的平民和义警，不再使用旧 isInnocent 标志推断。 */
            return role != null && (role.getFaction() == Faction.CIVILIAN || role.getFaction() == Faction.VIGILANTE);
        });
        boolean killersAlive = alive.stream().anyMatch(player -> {
            Role role = gameWorld.getRole(player);
            /*
             * canUseKiller 只代表能否使用杀手功能，并不等价于阵营。
             * 扩展职业即使拥有杀手能力，只要显式 Faction 不是 KILLER，就不能阻挡杀手阵营全灭判定。
             */
            return role != null && role.getFaction() == Faction.KILLER;
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

    private static void giveCurtainCallItemIfMissing(ServerPlayerEntity player, Item item) {
        if (!player.getInventory().contains(stack -> stack.isOf(item))) {
            ItemStack stack = item.getDefaultStack();
            stack.set(ModItems.SHADOW_JESTER_CURTAIN_CALL_GRANTED, true);
            player.getInventory().offerOrDrop(stack);
        }
    }

    private static void removeCurtainCallGrantedItems(ServerWorld world) {
        /*
         * 来源标记随 ItemStack 一起移动，因此这里同时清理所有玩家背包和地面掉落实体。
         * 第三阶段原本给予的普通左轮/开锁器没有标记，会被完整保留。
         */
        removeCurtainCallGrantedItemsFromInventories(world);
        for (ItemEntity itemEntity : world.getEntitiesByType(TypeFilter.equals(ItemEntity.class), ignored -> true)) {
            if (itemEntity.getStack().getOrDefault(ModItems.SHADOW_JESTER_CURTAIN_CALL_GRANTED, false)) {
                itemEntity.discard();
            }
        }
    }

    private static void removeCurtainCallGrantedItemsFromInventories(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.getInventory().remove(
                    stack -> stack.getOrDefault(ModItems.SHADOW_JESTER_CURTAIN_CALL_GRANTED, false),
                    Integer.MAX_VALUE,
                    player.getInventory()
            );
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
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
