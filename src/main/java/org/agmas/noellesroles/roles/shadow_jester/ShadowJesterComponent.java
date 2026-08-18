package org.agmas.noellesroles.roles.shadow_jester;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 影子小丑世界级运行态。
 *
 * <p>影子小丑是一对玩家共享的状态机：伴侣关系、双方阶段、各自任务进度、任务补发倒计时、
 * 缔结申请、谢幕音乐主题和离线待处理死亡都需要跨玩家共享，因此放在世界组件里。
 *
 * <p>该组件会进入时停者世界快照白名单。原因是这些字段全是局内时间线状态：
 * 回溯到 30 秒前时，阶段、任务数量、申请倒计时和第四阶段音乐都应该回到当时状态。</p>
 */
public class ShadowJesterComponent implements AutoSyncedComponent {
    public static final ComponentKey<ShadowJesterComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "shadow_jester"),
            ShadowJesterComponent.class
    );

    private static final String HAS_PAIR_KEY = "has_pair";
    private static final String FIRST_KEY = "first";
    private static final String SECOND_KEY = "second";
    private static final String FORCED_KEY = "forced";
    private static final String FIRST_PHASE_KEY = "first_phase";
    private static final String SECOND_PHASE_KEY = "second_phase";
    private static final String FIRST_TASKS_KEY = "first_tasks";
    private static final String SECOND_TASKS_KEY = "second_tasks";
    private static final String FIRST_REFILL_KEY = "first_refill";
    private static final String SECOND_REFILL_KEY = "second_refill";
    private static final String FIRST_MANAGED_TASKS_KEY = "first_managed_tasks";
    private static final String SECOND_MANAGED_TASKS_KEY = "second_managed_tasks";
    private static final String FIRST_CONFIRMED_DEAD_KEY = "first_confirmed_dead";
    private static final String SECOND_CONFIRMED_DEAD_KEY = "second_confirmed_dead";
    private static final String REQUEST_FROM_KEY = "request_from";
    private static final String REQUEST_TO_KEY = "request_to";
    private static final String REQUEST_TICKS_KEY = "request_ticks";
    private static final String PHASE_FOUR_THEME_KEY = "phase_four_theme";
    private static final String PENDING_OFFLINE_DEATHS_KEY = "pending_offline_deaths";
    private static final String PENDING_PLAYER_KEY = "player";
    private static final String PENDING_REASON_KEY = "reason";

    private final World world;
    private boolean hasPair;
    private UUID first;
    private UUID second;
    private boolean forced;
    private ShadowJesterPhase firstPhase = ShadowJesterPhase.TASKS;
    private ShadowJesterPhase secondPhase = ShadowJesterPhase.TASKS;
    private int firstCompletedTasks;
    private int secondCompletedTasks;
    private int firstRefillTicks = -1;
    private int secondRefillTicks = -1;
    private final List<Identifier> firstManagedTaskIds = new ArrayList<>();
    private final List<Identifier> secondManagedTaskIds = new ArrayList<>();
    private boolean firstConfirmedDead;
    private boolean secondConfirmedDead;
    private UUID requestFrom;
    private UUID requestTo;
    private int requestTicksLeft;
    private ShadowJesterMusicTheme phaseFourTheme = ShadowJesterMusicTheme.NONE;
    private final Map<UUID, Identifier> pendingOfflineDeaths = new HashMap<>();

    public ShadowJesterComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    public void clear() {
        this.hasPair = false;
        this.first = null;
        this.second = null;
        this.forced = false;
        this.firstPhase = ShadowJesterPhase.TASKS;
        this.secondPhase = ShadowJesterPhase.TASKS;
        this.firstCompletedTasks = 0;
        this.secondCompletedTasks = 0;
        this.firstRefillTicks = -1;
        this.secondRefillTicks = -1;
        this.firstManagedTaskIds.clear();
        this.secondManagedTaskIds.clear();
        this.firstConfirmedDead = false;
        this.secondConfirmedDead = false;
        clearVowRequest(false);
        this.phaseFourTheme = ShadowJesterMusicTheme.NONE;
        this.pendingOfflineDeaths.clear();
        sync();
    }

    public boolean hasPair() {
        return this.hasPair && this.first != null && this.second != null;
    }

    public void setPair(@NotNull UUID first, @NotNull UUID second, boolean forced) {
        if (first.equals(second)) {
            return;
        }
        this.hasPair = true;
        this.first = first;
        this.second = second;
        this.forced = forced;
        this.firstPhase = ShadowJesterPhase.TASKS;
        this.secondPhase = ShadowJesterPhase.TASKS;
        this.firstCompletedTasks = 0;
        this.secondCompletedTasks = 0;
        this.firstRefillTicks = -1;
        this.secondRefillTicks = -1;
        this.firstManagedTaskIds.clear();
        this.secondManagedTaskIds.clear();
        this.firstConfirmedDead = false;
        this.secondConfirmedDead = false;
        this.phaseFourTheme = ShadowJesterMusicTheme.NONE;
        clearVowRequest(false);
        sync();
    }

    public void removePairKeepPendingDeaths() {
        this.hasPair = false;
        this.first = null;
        this.second = null;
        this.forced = false;
        this.firstPhase = ShadowJesterPhase.TASKS;
        this.secondPhase = ShadowJesterPhase.TASKS;
        this.firstCompletedTasks = 0;
        this.secondCompletedTasks = 0;
        this.firstRefillTicks = -1;
        this.secondRefillTicks = -1;
        this.firstManagedTaskIds.clear();
        this.secondManagedTaskIds.clear();
        this.firstConfirmedDead = false;
        this.secondConfirmedDead = false;
        this.phaseFourTheme = ShadowJesterMusicTheme.NONE;
        clearVowRequest(false);
        sync();
    }

    public @Nullable UUID first() {
        return this.first;
    }

    public @Nullable UUID second() {
        return this.second;
    }

    public boolean isForced() {
        return hasPair() && this.forced;
    }

    public boolean contains(UUID player) {
        return player != null && hasPair() && (player.equals(this.first) || player.equals(this.second));
    }

    public boolean arePartners(UUID first, UUID second) {
        return first != null && second != null && second.equals(getPartner(first));
    }

    public @Nullable UUID getPartner(UUID player) {
        if (!contains(player)) {
            return null;
        }
        return player.equals(this.first) ? this.second : this.first;
    }

    public ShadowJesterPhase getPhase(UUID player) {
        if (!contains(player)) {
            return ShadowJesterPhase.TASKS;
        }
        return player.equals(this.first) ? this.firstPhase : this.secondPhase;
    }

    public void setPhase(UUID player, ShadowJesterPhase phase) {
        if (!contains(player)) {
            return;
        }
        if (player.equals(this.first)) {
            this.firstPhase = phase;
        } else {
            this.secondPhase = phase;
        }
        sync();
    }

    public int getCompletedTasks(UUID player) {
        if (!contains(player)) {
            return 0;
        }
        return player.equals(this.first) ? this.firstCompletedTasks : this.secondCompletedTasks;
    }

    public int incrementCompletedTasks(UUID player) {
        if (!contains(player)) {
            return 0;
        }
        if (player.equals(this.first)) {
            this.firstCompletedTasks++;
            sync();
            return this.firstCompletedTasks;
        }
        this.secondCompletedTasks++;
        sync();
        return this.secondCompletedTasks;
    }

    public int getRemainingTasks(UUID player) {
        return Math.max(0, ShadowJesterConstants.REQUIRED_COMPLETED_TASKS - getCompletedTasks(player));
    }

    public int getRefillTicks(UUID player) {
        if (!contains(player)) {
            return -1;
        }
        return player.equals(this.first) ? this.firstRefillTicks : this.secondRefillTicks;
    }

    public void setRefillTicks(UUID player, int ticks) {
        if (!contains(player)) {
            return;
        }
        if (player.equals(this.first)) {
            this.firstRefillTicks = ticks;
        } else {
            this.secondRefillTicks = ticks;
        }
        sync();
    }

    public void decrementRefillTicks(UUID player) {
        int ticks = getRefillTicks(player);
        if (ticks <= 0) {
            return;
        }
        setRefillTicks(player, ticks - 1);
    }

    public List<Identifier> getManagedTaskIds(UUID player) {
        if (!contains(player)) {
            return List.of();
        }
        return List.copyOf(player.equals(this.first) ? this.firstManagedTaskIds : this.secondManagedTaskIds);
    }

    public boolean hasManagedTaskIds(UUID player) {
        return !getManagedTaskIds(player).isEmpty();
    }

    public void setManagedTaskIds(UUID player, List<Identifier> taskIds) {
        if (!contains(player)) {
            return;
        }
        List<Identifier> target = player.equals(this.first) ? this.firstManagedTaskIds : this.secondManagedTaskIds;
        target.clear();
        for (Identifier taskId : taskIds) {
            if (taskId != null && !target.contains(taskId)) {
                target.add(taskId);
            }
        }
        sync();
    }

    public void clearManagedTaskIds(UUID player) {
        if (!contains(player)) {
            return;
        }
        List<Identifier> target = player.equals(this.first) ? this.firstManagedTaskIds : this.secondManagedTaskIds;
        if (!target.isEmpty()) {
            target.clear();
            sync();
        }
    }

    public boolean isConfirmedDead(UUID player) {
        if (!contains(player)) {
            return false;
        }
        return player.equals(this.first) ? this.firstConfirmedDead : this.secondConfirmedDead;
    }

    public void setConfirmedDead(UUID player, boolean confirmedDead) {
        if (!contains(player)) {
            return;
        }
        /*
         * 这里记录的是“经过 Wathe DeathApi 确认的死亡事实”，不要和
         * GameFunctions.isPlayerAliveAndSurvival(...) 混在一起。
         * 管理员为了调试把影子小丑切到 creative / spectator 时，Wathe 会把他视作非玩法存活，
         * 但那并不是一次有死因、有尸体、有死亡回放的死亡，第四阶段音乐和独立胜利不应因此失效。
         */
        if (player.equals(this.first)) {
            if (this.firstConfirmedDead == confirmedDead) {
                return;
            }
            this.firstConfirmedDead = confirmedDead;
        } else {
            if (this.secondConfirmedDead == confirmedDead) {
                return;
            }
            this.secondConfirmedDead = confirmedDead;
        }
        sync();
    }

    public boolean hasPendingOfflineDeath(UUID player) {
        return player != null && this.pendingOfflineDeaths.containsKey(player);
    }

    public boolean isConfirmedOrPendingDeath(UUID player) {
        return isConfirmedDead(player) || hasPendingOfflineDeath(player);
    }

    public boolean areBothPairMembersConfirmedOrPendingDeath() {
        return hasPair()
                && isConfirmedOrPendingDeath(this.first)
                && isConfirmedOrPendingDeath(this.second);
    }

    public void startVowRequest(@NotNull UUID from, @NotNull UUID to) {
        if (!arePartners(from, to)) {
            return;
        }
        this.requestFrom = from;
        this.requestTo = to;
        this.requestTicksLeft = ShadowJesterConstants.VOW_REQUEST_DURATION_TICKS;
        sync();
    }

    public boolean hasVowRequest() {
        return this.requestFrom != null && this.requestTo != null && this.requestTicksLeft > 0;
    }

    public boolean isRequestFrom(UUID player) {
        return player != null && player.equals(this.requestFrom) && hasVowRequest();
    }

    public boolean isRequestTo(UUID player) {
        return player != null && player.equals(this.requestTo) && hasVowRequest();
    }

    public @Nullable UUID getRequestFrom() {
        return hasVowRequest() ? this.requestFrom : null;
    }

    public @Nullable UUID getRequestTo() {
        return hasVowRequest() ? this.requestTo : null;
    }

    public int getRequestTicksLeft() {
        return hasVowRequest() ? this.requestTicksLeft : 0;
    }

    public boolean tickVowRequest() {
        if (!hasVowRequest()) {
            return false;
        }
        this.requestTicksLeft--;
        if (this.requestTicksLeft > 0) {
            sync();
            return false;
        }
        clearVowRequest(true);
        return true;
    }

    public void clearVowRequest(boolean sync) {
        this.requestFrom = null;
        this.requestTo = null;
        this.requestTicksLeft = 0;
        if (sync) {
            sync();
        }
    }

    public ShadowJesterMusicTheme getPhaseFourTheme() {
        return this.phaseFourTheme;
    }

    public void setPhaseFourTheme(ShadowJesterMusicTheme phaseFourTheme) {
        this.phaseFourTheme = phaseFourTheme == null ? ShadowJesterMusicTheme.NONE : phaseFourTheme;
        sync();
    }

    public void markPendingOfflineDeath(UUID player, Identifier reason) {
        if (player == null || reason == null) {
            return;
        }
        this.pendingOfflineDeaths.put(player, reason);
        sync();
    }

    public @Nullable Identifier consumePendingOfflineDeath(UUID player) {
        if (player == null) {
            return null;
        }
        Identifier reason = this.pendingOfflineDeaths.remove(player);
        if (reason != null) {
            sync();
        }
        return reason;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hasPair = tag.getBoolean(HAS_PAIR_KEY);
        this.first = tag.containsUuid(FIRST_KEY) ? tag.getUuid(FIRST_KEY) : null;
        this.second = tag.containsUuid(SECOND_KEY) ? tag.getUuid(SECOND_KEY) : null;
        this.forced = tag.getBoolean(FORCED_KEY);
        this.firstPhase = ShadowJesterPhase.fromId(tag.getInt(FIRST_PHASE_KEY));
        this.secondPhase = ShadowJesterPhase.fromId(tag.getInt(SECOND_PHASE_KEY));
        this.firstCompletedTasks = tag.getInt(FIRST_TASKS_KEY);
        this.secondCompletedTasks = tag.getInt(SECOND_TASKS_KEY);
        this.firstRefillTicks = tag.contains(FIRST_REFILL_KEY) ? tag.getInt(FIRST_REFILL_KEY) : -1;
        this.secondRefillTicks = tag.contains(SECOND_REFILL_KEY) ? tag.getInt(SECOND_REFILL_KEY) : -1;
        readTaskIdList(tag, FIRST_MANAGED_TASKS_KEY, this.firstManagedTaskIds);
        readTaskIdList(tag, SECOND_MANAGED_TASKS_KEY, this.secondManagedTaskIds);
        this.firstConfirmedDead = tag.getBoolean(FIRST_CONFIRMED_DEAD_KEY);
        this.secondConfirmedDead = tag.getBoolean(SECOND_CONFIRMED_DEAD_KEY);
        this.requestFrom = tag.containsUuid(REQUEST_FROM_KEY) ? tag.getUuid(REQUEST_FROM_KEY) : null;
        this.requestTo = tag.containsUuid(REQUEST_TO_KEY) ? tag.getUuid(REQUEST_TO_KEY) : null;
        this.requestTicksLeft = tag.getInt(REQUEST_TICKS_KEY);
        this.phaseFourTheme = ShadowJesterMusicTheme.fromSerialized(tag.getString(PHASE_FOUR_THEME_KEY));

        this.pendingOfflineDeaths.clear();
        for (NbtElement element : tag.getList(PENDING_OFFLINE_DEATHS_KEY, NbtElement.COMPOUND_TYPE)) {
            if (!(element instanceof NbtCompound deathTag)
                    || !deathTag.containsUuid(PENDING_PLAYER_KEY)
                    || !deathTag.contains(PENDING_REASON_KEY)) {
                continue;
            }
            Identifier reason = Identifier.tryParse(deathTag.getString(PENDING_REASON_KEY));
            if (reason != null) {
                this.pendingOfflineDeaths.put(deathTag.getUuid(PENDING_PLAYER_KEY), reason);
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean(HAS_PAIR_KEY, this.hasPair);
        if (this.first != null) {
            tag.putUuid(FIRST_KEY, this.first);
        }
        if (this.second != null) {
            tag.putUuid(SECOND_KEY, this.second);
        }
        tag.putBoolean(FORCED_KEY, this.forced);
        tag.putInt(FIRST_PHASE_KEY, this.firstPhase.id());
        tag.putInt(SECOND_PHASE_KEY, this.secondPhase.id());
        tag.putInt(FIRST_TASKS_KEY, this.firstCompletedTasks);
        tag.putInt(SECOND_TASKS_KEY, this.secondCompletedTasks);
        tag.putInt(FIRST_REFILL_KEY, this.firstRefillTicks);
        tag.putInt(SECOND_REFILL_KEY, this.secondRefillTicks);
        tag.put(FIRST_MANAGED_TASKS_KEY, writeTaskIdList(this.firstManagedTaskIds));
        tag.put(SECOND_MANAGED_TASKS_KEY, writeTaskIdList(this.secondManagedTaskIds));
        tag.putBoolean(FIRST_CONFIRMED_DEAD_KEY, this.firstConfirmedDead);
        tag.putBoolean(SECOND_CONFIRMED_DEAD_KEY, this.secondConfirmedDead);
        if (this.requestFrom != null) {
            tag.putUuid(REQUEST_FROM_KEY, this.requestFrom);
        }
        if (this.requestTo != null) {
            tag.putUuid(REQUEST_TO_KEY, this.requestTo);
        }
        tag.putInt(REQUEST_TICKS_KEY, this.requestTicksLeft);
        tag.putString(PHASE_FOUR_THEME_KEY, this.phaseFourTheme.serialized());

        NbtList pendingDeaths = new NbtList();
        for (Map.Entry<UUID, Identifier> entry : this.pendingOfflineDeaths.entrySet()) {
            NbtCompound deathTag = new NbtCompound();
            deathTag.putUuid(PENDING_PLAYER_KEY, entry.getKey());
            deathTag.putString(PENDING_REASON_KEY, entry.getValue().toString());
            pendingDeaths.add(deathTag);
        }
        tag.put(PENDING_OFFLINE_DEATHS_KEY, pendingDeaths);
    }

    private static void readTaskIdList(NbtCompound tag, String key, List<Identifier> target) {
        target.clear();
        for (NbtElement element : tag.getList(key, NbtElement.STRING_TYPE)) {
            Identifier taskId = Identifier.tryParse(element.asString());
            if (taskId != null && !target.contains(taskId)) {
                target.add(taskId);
            }
        }
    }

    private static NbtList writeTaskIdList(List<Identifier> taskIds) {
        NbtList list = new NbtList();
        for (Identifier taskId : taskIds) {
            list.add(NbtString.of(taskId.toString()));
        }
        return list;
    }
}
