package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.psycho.PsychoModeState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 杀手皇后整局共享状态。
 *
 * <p>标记、招募和疯魔暂停都是跨玩家关系，不能只塞进某个目标自己的组件，否则多名调试
 * 杀手皇后会互相覆盖。这里始终用杀手皇后 UUID 作为关系所有者，并进入时停者世界快照。</p>
 */
public final class KillerQueenWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<KillerQueenWorldComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("killer_queen"), KillerQueenWorldComponent.class);

    private final World world;
    private int availableRecruitments;
    private final Map<UUID, Set<UUID>> markersByQueen = new HashMap<>();
    private final Map<UUID, Recruitment> recruitments = new HashMap<>();
    private final Set<UUID> psychoSessions = new HashSet<>();
    private final Map<UUID, Set<UUID>> psychoAddedMarkers = new HashMap<>();
    /** 每名杀手皇后本轮疯魔距离下一层护盾还未结算的有效首次标记次数。 */
    private final Map<UUID, Integer> psychoValidMarkProgress = new HashMap<>();
    private final Map<UUID, PsychoModeState> suspendedPsychoStates = new HashMap<>();
    private final Map<UUID, Identifier> suppressedPsychoProfiles = new HashMap<>();

    public KillerQueenWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    public int getAvailableRecruitments() {
        return Math.max(0, this.availableRecruitments);
    }

    public void addRecruitmentOpportunity() {
        this.availableRecruitments++;
        sync();
    }

    public boolean consumeRecruitmentOpportunity() {
        if (this.availableRecruitments <= 0) {
            return false;
        }
        this.availableRecruitments--;
        sync();
        return true;
    }

    public boolean isMarkedBy(UUID queen, UUID target) {
        return queen != null && target != null && this.markersByQueen.getOrDefault(queen, Set.of()).contains(target);
    }

    public boolean addMarker(UUID queen, UUID target, boolean addedDuringPsycho) {
        if (queen == null || target == null || queen.equals(target)) {
            return false;
        }
        boolean added = this.markersByQueen.computeIfAbsent(queen, ignored -> new HashSet<>()).add(target);
        if (added && addedDuringPsycho && this.psychoSessions.contains(queen)) {
            this.psychoAddedMarkers.computeIfAbsent(queen, ignored -> new HashSet<>()).add(target);
        }
        if (added) {
            sync();
        }
        return added;
    }

    /**
     * 标记法杖首次标记合法目标，并结算本轮疯魔的护盾进度。
     *
     * <p>标记、回退清单和计数只同步一次，避免同一次命中产生两份完整世界组件同步包。
     * 计数保存的是除以门槛后的余数，因此每达到一次门槛只奖励一层，后续命中从零继续累计。</p>
     */
    public PsychoMarkerResult addPsychoMarker(UUID queen, UUID target) {
        if (queen == null || target == null || queen.equals(target) || !this.psychoSessions.contains(queen)) {
            return PsychoMarkerResult.NOT_ADDED;
        }
        if (!this.markersByQueen.computeIfAbsent(queen, ignored -> new HashSet<>()).add(target)) {
            return PsychoMarkerResult.NOT_ADDED;
        }

        this.psychoAddedMarkers.computeIfAbsent(queen, ignored -> new HashSet<>()).add(target);
        int nextProgress = this.psychoValidMarkProgress.getOrDefault(queen, 0) + 1;
        boolean grantsShield = nextProgress >= KillerQueenConstants.MARKER_STAFF_VALID_MARKS_PER_SHIELD;
        this.psychoValidMarkProgress.put(queen, grantsShield
                ? nextProgress - KillerQueenConstants.MARKER_STAFF_VALID_MARKS_PER_SHIELD
                : nextProgress);
        sync();
        return new PsychoMarkerResult(true, grantsShield);
    }

    public boolean removeMarker(UUID queen, UUID target) {
        Set<UUID> targets = this.markersByQueen.get(queen);
        if (targets == null || !targets.remove(target)) {
            return false;
        }
        if (targets.isEmpty()) {
            this.markersByQueen.remove(queen);
        }
        Set<UUID> psychoTargets = this.psychoAddedMarkers.get(queen);
        if (psychoTargets != null) {
            psychoTargets.remove(target);
            if (psychoTargets.isEmpty()) {
                this.psychoAddedMarkers.remove(queen);
            }
        }
        sync();
        return true;
    }

    public Set<UUID> getMarkedTargets(UUID queen) {
        return Set.copyOf(this.markersByQueen.getOrDefault(queen, Set.of()));
    }

    public Set<UUID> getAllMarkedTargets() {
        Set<UUID> result = new HashSet<>();
        for (Set<UUID> targets : this.markersByQueen.values()) result.addAll(targets);
        return Set.copyOf(result);
    }

    public void removeAllMarkersForTarget(UUID target) {
        boolean changed = false;
        for (Set<UUID> targets : this.markersByQueen.values()) {
            changed |= targets.remove(target);
        }
        this.markersByQueen.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        for (Set<UUID> targets : this.psychoAddedMarkers.values()) {
            targets.remove(target);
        }
        this.psychoAddedMarkers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (changed) {
            sync();
        }
    }

    public void startPsychoSession(UUID queen) {
        this.psychoSessions.add(queen);
        this.psychoAddedMarkers.remove(queen);
        this.psychoValidMarkProgress.remove(queen);
        this.suspendedPsychoStates.remove(queen);
        sync();
    }

    public boolean hasPsychoSession(UUID queen) {
        return this.psychoSessions.contains(queen);
    }

    public Set<UUID> finishPsychoSession(UUID queen) {
        this.psychoSessions.remove(queen);
        this.suspendedPsychoStates.remove(queen);
        this.psychoValidMarkProgress.remove(queen);
        Set<UUID> added = this.psychoAddedMarkers.remove(queen);
        sync();
        return added == null ? Set.of() : Set.copyOf(added);
    }

    public void suspendPsycho(UUID queen, PsychoModeState state) {
        if (queen != null && state != null) {
            this.suspendedPsychoStates.put(queen, state);
            sync();
        }
    }

    public @Nullable PsychoModeState getSuspendedPsycho(UUID queen) {
        return this.suspendedPsychoStates.get(queen);
    }

    public void clearSuspendedPsycho(UUID queen) {
        if (this.suspendedPsychoStates.remove(queen) != null) {
            sync();
        }
    }

    public void setSuppressedPsychoProfile(UUID player, Identifier profileId) {
        this.suppressedPsychoProfiles.put(player, profileId);
        sync();
    }

    public Map<UUID, Identifier> getSuppressedPsychoProfiles() {
        return Map.copyOf(this.suppressedPsychoProfiles);
    }

    public void clearSuppressedPsychoProfiles() {
        if (!this.suppressedPsychoProfiles.isEmpty()) {
            this.suppressedPsychoProfiles.clear();
            sync();
        }
    }

    public void recruit(UUID recruit, UUID queen, UUID target) {
        this.recruitments.put(recruit, new Recruitment(queen, target));
        sync();
    }

    public @Nullable Recruitment getRecruitment(UUID recruit) {
        return this.recruitments.get(recruit);
    }

    public boolean isRecruited(UUID player) {
        return this.recruitments.containsKey(player);
    }

    public Set<UUID> getRecruitedPlayers() {
        return Set.copyOf(this.recruitments.keySet());
    }

    public void updateRecruitmentTarget(UUID recruit, UUID target) {
        Recruitment current = this.recruitments.get(recruit);
        if (current != null && target != null) {
            this.recruitments.put(recruit, new Recruitment(current.queen(), target));
            sync();
        }
    }

    public @Nullable Recruitment clearRecruitment(UUID recruit) {
        Recruitment removed = this.recruitments.remove(recruit);
        if (removed != null) {
            sync();
        }
        return removed;
    }

    /**
     * 被招募者在完成指定击杀前死亡时，清除招募关系并返还一次世界共享招募机会。
     *
     * <p>清理关系与增加计数必须放在同一个入口：死亡链、环境伤害和连锁死亡可能在相邻 tick
     * 再次尝试处理同一名死者。只有第一次成功移除招募关系时才增加计数，后续重复调用会返回
     * {@code null}，从而避免同一次死亡反复刷取招募机会。</p>
     *
     * <p>成功完成指定击杀后的转职仍使用 {@link #clearRecruitment(UUID)}，不会调用这里，
     * 因为成功招募已经正常消耗了这次机会，不应再返还。</p>
     */
    public @Nullable Recruitment clearRecruitmentAndRefundOpportunity(UUID recruit) {
        Recruitment removed = this.recruitments.remove(recruit);
        if (removed != null) {
            this.availableRecruitments++;
            sync();
        }
        return removed;
    }

    public void clear() {
        this.availableRecruitments = 0;
        this.markersByQueen.clear();
        this.recruitments.clear();
        this.psychoSessions.clear();
        this.psychoAddedMarkers.clear();
        this.psychoValidMarkProgress.clear();
        this.suspendedPsychoStates.clear();
        this.suppressedPsychoProfiles.clear();
        sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("available_recruitments", this.availableRecruitments);

        NbtList markerList = new NbtList();
        for (Map.Entry<UUID, Set<UUID>> entry : this.markersByQueen.entrySet()) {
            for (UUID target : entry.getValue()) {
                NbtCompound marker = new NbtCompound();
                marker.putUuid("queen", entry.getKey());
                marker.putUuid("target", target);
                markerList.add(marker);
            }
        }
        tag.put("markers", markerList);

        NbtList recruitmentList = new NbtList();
        for (Map.Entry<UUID, Recruitment> entry : this.recruitments.entrySet()) {
            NbtCompound recruitment = new NbtCompound();
            recruitment.putUuid("recruit", entry.getKey());
            recruitment.putUuid("queen", entry.getValue().queen());
            recruitment.putUuid("target", entry.getValue().target());
            recruitmentList.add(recruitment);
        }
        tag.put("recruitments", recruitmentList);

        NbtList sessions = new NbtList();
        for (UUID queen : this.psychoSessions) {
            NbtCompound session = new NbtCompound();
            session.putUuid("queen", queen);
            NbtList added = new NbtList();
            for (UUID target : this.psychoAddedMarkers.getOrDefault(queen, Set.of())) {
                NbtCompound targetTag = new NbtCompound();
                targetTag.putUuid("target", target);
                added.add(targetTag);
            }
            session.put("added_markers", added);
            session.putInt("valid_mark_progress", this.psychoValidMarkProgress.getOrDefault(queen, 0));
            PsychoModeState suspended = this.suspendedPsychoStates.get(queen);
            if (suspended != null) {
                session.putString("profile", suspended.profileId().toString());
                session.putInt("remaining", suspended.remainingTicks());
                session.putInt("max", suspended.maxTicks());
                session.putInt("armour", suspended.armour());
                session.putInt("initial_armour", suspended.initialArmour());
            }
            sessions.add(session);
        }
        tag.put("psycho_sessions", sessions);

        NbtList suppressed = new NbtList();
        for (Map.Entry<UUID, Identifier> entry : this.suppressedPsychoProfiles.entrySet()) {
            NbtCompound value = new NbtCompound();
            value.putUuid("player", entry.getKey());
            value.putString("profile", entry.getValue().toString());
            suppressed.add(value);
        }
        tag.put("suppressed_profiles", suppressed);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.availableRecruitments = Math.max(0, tag.getInt("available_recruitments"));
        this.markersByQueen.clear();
        this.recruitments.clear();
        this.psychoSessions.clear();
        this.psychoAddedMarkers.clear();
        this.psychoValidMarkProgress.clear();
        this.suspendedPsychoStates.clear();
        this.suppressedPsychoProfiles.clear();

        for (NbtElement element : tag.getList("markers", NbtElement.COMPOUND_TYPE)) {
            if (element instanceof NbtCompound marker && marker.containsUuid("queen") && marker.containsUuid("target")) {
                this.markersByQueen.computeIfAbsent(marker.getUuid("queen"), ignored -> new HashSet<>()).add(marker.getUuid("target"));
            }
        }
        for (NbtElement element : tag.getList("recruitments", NbtElement.COMPOUND_TYPE)) {
            if (element instanceof NbtCompound recruitment && recruitment.containsUuid("recruit")
                    && recruitment.containsUuid("queen") && recruitment.containsUuid("target")) {
                this.recruitments.put(recruitment.getUuid("recruit"),
                        new Recruitment(recruitment.getUuid("queen"), recruitment.getUuid("target")));
            }
        }
        for (NbtElement element : tag.getList("psycho_sessions", NbtElement.COMPOUND_TYPE)) {
            if (!(element instanceof NbtCompound session) || !session.containsUuid("queen")) {
                continue;
            }
            UUID queen = session.getUuid("queen");
            this.psychoSessions.add(queen);
            this.psychoValidMarkProgress.put(queen, Math.max(0,
                    session.getInt("valid_mark_progress") % KillerQueenConstants.MARKER_STAFF_VALID_MARKS_PER_SHIELD));
            for (NbtElement addedElement : session.getList("added_markers", NbtElement.COMPOUND_TYPE)) {
                if (addedElement instanceof NbtCompound added && added.containsUuid("target")) {
                    this.psychoAddedMarkers.computeIfAbsent(queen, ignored -> new HashSet<>()).add(added.getUuid("target"));
                }
            }
            Identifier profile = Identifier.tryParse(session.getString("profile"));
            if (profile != null && session.getInt("remaining") > 0) {
                this.suspendedPsychoStates.put(queen, new PsychoModeState(
                        profile,
                        session.getInt("remaining"),
                        session.getInt("max"),
                        session.getInt("armour"),
                        session.getInt("initial_armour")
                ));
            }
        }
        for (NbtElement element : tag.getList("suppressed_profiles", NbtElement.COMPOUND_TYPE)) {
            if (element instanceof NbtCompound value && value.containsUuid("player")) {
                Identifier profile = Identifier.tryParse(value.getString("profile"));
                if (profile != null) {
                    this.suppressedPsychoProfiles.put(value.getUuid("player"), profile);
                }
            }
        }
    }

    public record Recruitment(@NotNull UUID queen, @NotNull UUID target) {
    }

    /** 标记法杖首次标记的原子结算结果。 */
    public record PsychoMarkerResult(boolean added, boolean grantsShield) {
        private static final PsychoMarkerResult NOT_ADDED = new PsychoMarkerResult(false, false);
    }
}
