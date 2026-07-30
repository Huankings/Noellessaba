package org.agmas.noellesroles.roles.morphling;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 变形怪试剂增强的服务端规则中心。
 *
 * <p>物品右键、遥控器触发、聊天/语音伪装、击杀奖励和死亡清理都走这里。
 * 这样“谁可以使用、目标怎么解析、什么时候消耗试剂、什么时候写回放”只有一份判定，
 * 后续调数值或改交互时不会出现物品逻辑和语音/聊天逻辑互相漂移。</p>
 */
public final class MorphlingReagentService {
    private static final String ROOT_KEY = "NoellesRolesMorphReagent";
    private static final String SAMPLE_UUID_KEY = "SampleUuid";
    private static final String SAMPLE_NAME_KEY = "SampleName";
    /*
     * 这里只记录“刚采样成功后，正在等待玩家松开右键”的输入防抖状态。
     * 它不是局内可被利用的玩法进度：松开右键、玩家重置和回合结束都会清掉；
     * 时停者回溯也不应该把一次鼠标按住动作回放出来，所以不加入 TimekeeperSnapshots。
     */
    private static final Set<UUID> WAITING_FOR_REAGENT_RELEASE = new HashSet<>();

    private MorphlingReagentService() {
    }

    public static void assignForRole(@NotNull ServerPlayerEntity player, @NotNull Role role) {
        if (role != NoellesRoleRegistry.MORPHLING || hasMorphDevice(player)) {
            return;
        }
        player.giveItemStack(ModItems.MORPH_DEVICE.getDefaultStack());
    }

    public static void reset(@NotNull ServerPlayerEntity player) {
        MorphMarkPlayerComponent.KEY.get(player).clear();
        clearReagentReleaseGate(player);
    }

    public static void clearAllReleaseGates() {
        WAITING_FOR_REAGENT_RELEASE.clear();
    }

    public static void clearReagentReleaseGate(@NotNull ServerPlayerEntity player) {
        WAITING_FOR_REAGENT_RELEASE.remove(player.getUuid());
    }

    public static @NotNull TypedActionResult<ItemStack> useReagent(
            @NotNull ServerPlayerEntity morphling,
            @NotNull ItemStack stack,
            @Nullable Entity explicitTarget
    ) {
        if (!canUseMorphlingItem(morphling)) {
            return TypedActionResult.fail(stack);
        }

        if (WAITING_FOR_REAGENT_RELEASE.contains(morphling.getUuid())) {
            /*
             * 采样成功后，同一次按住右键期间客户端和服务端可能继续收到 use/useOnEntity。
             * 如果不等玩家松开鼠标，就会从“刚采样”立刻落到“已有样本 -> 标记”的阶段，
             * 导致一瓶试剂被一次点击吃掉两段流程。
             */
            return TypedActionResult.success(stack);
        }

        Optional<SampleData> sample = getSample(stack);
        if (sample.isEmpty()) {
            SampleData sampled = sampleTarget(morphling, explicitTarget);
            setSample(stack, sampled);
            WAITING_FOR_REAGENT_RELEASE.add(morphling.getUuid());
            sendMorphlingMessage(morphling, "message.noellesroles.morphling.sampled", sampled.name());
            recordSample(morphling, sampled);
            return TypedActionResult.success(stack);
        }

        ServerPlayerEntity markTarget = findMarkTargetOrSelf(morphling, explicitTarget);
        if (sample.get().uuid().equals(markTarget.getUuid())) {
            sendMorphlingMessage(morphling, "message.noellesroles.morphling.same_target");
            return TypedActionResult.fail(stack);
        }

        MorphMarkPlayerComponent.KEY.get(markTarget).setPending(
                morphling,
                sample.get().uuid(),
                sample.get().name(),
                markTarget.getGameProfile().getName()
        );
        sendMorphlingMessage(morphling, "message.noellesroles.morphling.marked", markTarget.getGameProfile().getName());
        recordMark(morphling, markTarget, sample.get());

        stack.decrementUnlessCreative(1, morphling);
        return TypedActionResult.success(stack);
    }

    public static @NotNull TypedActionResult<ItemStack> useDevice(@NotNull ServerPlayerEntity morphling, @NotNull ItemStack stack) {
        if (!canUseMorphlingItem(morphling)) {
            return TypedActionResult.fail(stack);
        }

        int activated = 0;
        for (ServerPlayerEntity target : morphling.getServer().getPlayerManager().getPlayerList()) {
            MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(target);
            if (!component.isPending() || !component.isMarkedBy(morphling.getUuid())) {
                continue;
            }

            if (!GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.getUuid().equals(component.sampleUuid())) {
                /*
                 * 待触发标记是一次性陷阱；触发时如果目标已经死亡、离开可玩状态，
                 * 或者样本和目标因为后续状态变化变成同一个人，就直接清掉，避免遥控器以后反复扫到坏数据。
                 */
                component.clear();
                continue;
            }

            if (component.activate()) {
                activated++;
                recordTrigger(target, component);
            }
        }

        if (activated <= 0) {
            sendMorphlingMessage(morphling, "message.noellesroles.morphling.no_marks");
            return TypedActionResult.fail(stack);
        }

        sendMorphlingMessage(morphling, "message.noellesroles.morphling.triggered");
        return TypedActionResult.success(stack);
    }

    public static void afterKill(
            @NotNull ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            @NotNull Identifier deathReason
    ) {
        if (GameFunctions.isPlayerAliveAndSurvival(victim)) {
            return;
        }

        boolean victimHadActiveReagentMark = MorphMarkPlayerComponent.KEY.get(victim).isActive();
        if (!victimHadActiveReagentMark) {
            MorphBodyDisguiseWorldComponent.KEY.get(victim.getServerWorld()).clearBodyDisguise(victim.getUuid());
        }
        rewardSelfMorphKill(victim, killer);
        rewardMarkedPlayerEvent(victim, killer);
        MorphMarkPlayerComponent.KEY.get(victim).clear();
    }

    public static boolean shouldCancelRevolverPenalty(@NotNull PlayerEntity victim) {
        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(victim);
        if (!component.isActive()) {
            return false;
        }

        Role victimRole = GameWorldComponent.KEY.get(victim.getWorld()).getRole(victim);
        return victimRole != null && victimRole.isInnocent();
    }

    public static boolean isActivelyReagentDisguised(@NotNull ServerPlayerEntity player) {
        return MorphMarkPlayerComponent.KEY.get(player).isActive()
                && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    public static @NotNull List<ServerPlayerEntity> findActivePlayersDisguisedAs(@NotNull ServerPlayerEntity samplePlayer) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        if (!GameFunctions.isPlayerAliveAndSurvival(samplePlayer)) {
            /*
             * 语音和聊天伪装只允许“仍在局内存活的样本玩家”驱动。
             * 如果样本来自尸体，或者样本玩家之后死亡，死亡/旁观语音与聊天不能再借活着的伪装者传进局内，
             * 否则会绕过 Wathe 的死亡信息隔离。
             */
            return result;
        }

        MinecraftServer server = samplePlayer.getServer();
        for (ServerPlayerEntity possibleDisguised : server.getPlayerManager().getPlayerList()) {
            MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(possibleDisguised);
            if (component.isActive()
                    && samplePlayer.getUuid().equals(component.sampleUuid())
                    && GameFunctions.isPlayerAliveAndSurvival(possibleDisguised)) {
                result.add(possibleDisguised);
            }
        }
        return result;
    }

    public static boolean hasSample(@NotNull ItemStack stack) {
        return getSample(stack).isPresent();
    }

    public static @NotNull Optional<UUID> sampleUuid(@NotNull ItemStack stack) {
        return getSample(stack).map(SampleData::uuid);
    }

    public static @NotNull String sampleNameForTooltip(@NotNull ItemStack stack) {
        return getSample(stack).map(SampleData::name).orElse("");
    }

    private static boolean canUseMorphlingItem(@NotNull ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return game.isRole(player, NoellesRoleRegistry.MORPHLING)
                && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    private static boolean hasMorphDevice(@NotNull ServerPlayerEntity player) {
        return player.getInventory().contains(stack -> stack.isOf(ModItems.MORPH_DEVICE));
    }

    private static @NotNull SampleData sampleTarget(@NotNull ServerPlayerEntity morphling, @Nullable Entity explicitTarget) {
        Entity target = explicitTarget != null ? explicitTarget : findLookedAtSampleTarget(morphling);
        if (target instanceof ServerPlayerEntity playerTarget && isValidLivingTarget(morphling, playerTarget)) {
            return new SampleData(playerTarget.getUuid(), playerTarget.getGameProfile().getName());
        }
        if (target instanceof PlayerBodyEntity body) {
            /*
             * 按 SparkStrength66 规则，采样尸体时读取尸体真实 owner，
             * 不读取 appearanceUuid。这样被其它伪装改过外观的尸体不会把伪装目标继续套进试剂样本。
             */
            UUID bodyOwner = body.getPlayerUuid();
            return new SampleData(bodyOwner, resolvePlayerName(morphling.getServer(), bodyOwner));
        }
        return new SampleData(morphling.getUuid(), morphling.getGameProfile().getName());
    }

    private static @NotNull ServerPlayerEntity findMarkTargetOrSelf(@NotNull ServerPlayerEntity morphling, @Nullable Entity explicitTarget) {
        Entity target = explicitTarget != null ? explicitTarget : findLookedAtMarkTarget(morphling);
        if (target instanceof ServerPlayerEntity playerTarget && isValidLivingTarget(morphling, playerTarget)) {
            return playerTarget;
        }
        return morphling;
    }

    private static @Nullable Entity findLookedAtSampleTarget(@NotNull ServerPlayerEntity morphling) {
        HitResult hitResult = ProjectileUtil.getCollision(
                morphling,
                entity -> (entity instanceof ServerPlayerEntity target && isValidLivingTarget(morphling, target))
                        || entity instanceof PlayerBodyEntity,
                MorphlingConstants.REAGENT_TARGET_RANGE
        );
        return hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    private static @Nullable Entity findLookedAtMarkTarget(@NotNull ServerPlayerEntity morphling) {
        HitResult hitResult = ProjectileUtil.getCollision(
                morphling,
                entity -> entity instanceof ServerPlayerEntity target && isValidLivingTarget(morphling, target),
                MorphlingConstants.REAGENT_TARGET_RANGE
        );
        return hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    private static boolean isValidLivingTarget(@NotNull ServerPlayerEntity morphling, @NotNull ServerPlayerEntity target) {
        return target != morphling
                && GameFunctions.isPlayerAliveAndSurvival(target);
    }

    private static void rewardSelfMorphKill(@NotNull ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer == null || killer.getUuid().equals(victim.getUuid())) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(killer.getWorld());
        if (!game.isRole(killer, NoellesRoleRegistry.MORPHLING)) {
            return;
        }

        UUID currentDisguise = currentSelfDisguise(killer);
        if (currentDisguise == null) {
            return;
        }

        int reward = victim.getUuid().equals(currentDisguise)
                ? MorphlingConstants.SELF_MORPH_TARGET_KILL_REWARD
                : MorphlingConstants.SELF_MORPH_KILL_REWARD;
        PlayerShopComponent.KEY.get(killer).addToBalance(reward);
    }

    private static @Nullable UUID currentSelfDisguise(@NotNull ServerPlayerEntity morphling) {
        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(morphling);
        if (originalMorph.getMorphTicks() > 0 && originalMorph.disguise != null) {
            return originalMorph.disguise;
        }

        MorphMarkPlayerComponent reagentMorph = MorphMarkPlayerComponent.KEY.get(morphling);
        return reagentMorph.isActive() ? reagentMorph.sampleUuid() : null;
    }

    private static void rewardMarkedPlayerEvent(@NotNull ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer != null && !killer.getUuid().equals(victim.getUuid())) {
            rewardMarkerForActivePlayer(killer);
        }
        rewardMarkerForActivePlayer(victim);
    }

    private static void rewardMarkerForActivePlayer(@NotNull ServerPlayerEntity activePlayer) {
        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(activePlayer);
        UUID markerUuid = component.markerUuid();
        if (!component.isActive() || markerUuid == null || markerUuid.equals(activePlayer.getUuid())) {
            /*
             * 试剂作用在变形怪自己身上时，只走“自我伪装击杀奖励”。
             * 如果这里再给 +50，会让自用试剂比 SparkStrength66 规则多拿一份旁路收益。
             */
            return;
        }

        ServerPlayerEntity marker = activePlayer.getServer().getPlayerManager().getPlayer(markerUuid);
        if (marker != null
                && GameFunctions.isPlayerAliveAndSurvival(marker)) {
            PlayerShopComponent.KEY.get(marker).addToBalance(MorphlingConstants.OTHER_MARK_EVENT_REWARD);
        }
    }

    private static @NotNull Optional<SampleData> getSample(@NotNull ItemStack stack) {
        NbtCompound root = root(stack);
        if (!root.containsUuid(SAMPLE_UUID_KEY)) {
            return Optional.empty();
        }

        String name = root.getString(SAMPLE_NAME_KEY);
        if (name == null || name.isBlank()) {
            name = root.getUuid(SAMPLE_UUID_KEY).toString().substring(0, 8);
        }
        return Optional.of(new SampleData(root.getUuid(SAMPLE_UUID_KEY), name));
    }

    private static void setSample(@NotNull ItemStack stack, @NotNull SampleData sample) {
        NbtCompound customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        NbtCompound root = new NbtCompound();
        root.putUuid(SAMPLE_UUID_KEY, sample.uuid());
        root.putString(SAMPLE_NAME_KEY, sample.name());
        customData.put(ROOT_KEY, root);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }

    private static @NotNull NbtCompound root(@NotNull ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();
        return data.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE) ? data.getCompound(ROOT_KEY) : new NbtCompound();
    }

    private static void sendMorphlingMessage(@NotNull ServerPlayerEntity morphling, @NotNull String key, Object... args) {
        morphling.sendMessage(Text.translatable(key, args).withColor(NoellesRoleRegistry.MORPHLING.color()), true);
    }

    private static @NotNull String resolvePlayerName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getUserCache()
                .getByUuid(uuid)
                .map(GameProfile::getName)
                .orElse(uuid.toString().substring(0, 8));
    }

    private static void recordSample(@NotNull ServerPlayerEntity morphling, @NotNull SampleData sample) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("sample_player", sample.uuid());
        extra.putString("sample_name", sample.name());
        GameRecordManager.recordGlobalEvent(
                morphling.getServerWorld(),
                NoellesEventIds.MORPH_REAGENT_SAMPLED_EVENT,
                morphling,
                extra
        );
    }

    private static void recordMark(@NotNull ServerPlayerEntity morphling, @NotNull ServerPlayerEntity target, @NotNull SampleData sample) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("sample_player", sample.uuid());
        extra.putString("sample_name", sample.name());
        extra.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(
                morphling.getServerWorld(),
                NoellesEventIds.MORPH_REAGENT_MARKED_EVENT,
                morphling,
                extra
        );
    }

    private static void recordTrigger(@NotNull ServerPlayerEntity target, @NotNull MorphMarkPlayerComponent component) {
        UUID sampleUuid = component.sampleUuid();
        if (sampleUuid == null || !(target.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        NbtCompound extra = new NbtCompound();
        extra.putUuid("sample_player", sampleUuid);
        extra.putString("sample_name", component.sampleName());
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                NoellesEventIds.MORPH_MARK_TRIGGERED_EVENT,
                target,
                extra
        );
    }

    public record SampleData(@NotNull UUID uuid, @NotNull String name) {
    }
}
