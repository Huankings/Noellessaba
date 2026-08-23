package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.PlayerLifeStateApi;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.cca.PlayerInstinctComponent;
import dev.doctor4t.wathe.cca.PlayerLifeStateComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerNoteComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.mixin.roles.convener.ItemCooldownEntryAccessor;
import org.agmas.noellesroles.mixin.roles.convener.ItemCooldownManagerAccessor;
import org.agmas.noellesroles.modifiers.allergic.AllergicPlayerComponent;
import org.agmas.noellesroles.modifiers.chameleon.ChameleonPlayerComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.roles.noisemaker.NoisemakerGlowTargetComponent;
import org.agmas.noellesroles.roles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.agmas.noellesroles.roles.avaricious.AvariciousPayoutComponent;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerMomentumComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;
import org.agmas.noellesroles.roles.cook.CookPlayerComponent;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerPlayerComponent;
import org.agmas.noellesroles.roles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.hacker.HackerPhoneComponent;
import org.agmas.noellesroles.roles.hacker.HackerSafeTimeComponent;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonFireWorldComponent;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.magician.MagicianPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphMarkPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphBodyDisguiseWorldComponent;
import org.agmas.noellesroles.roles.muzzler.SilencePlayerComponent;
import org.agmas.noellesroles.roles.necromancer.NecromancerWorldComponent;
import org.agmas.noellesroles.roles.operator.OperatorPlayerComponent;
import org.agmas.noellesroles.roles.phantom.PhantomPlayerComponent;
import org.agmas.noellesroles.roles.physician.PhysicianPlayerComponent;
import org.agmas.noellesroles.roles.prophet.ProphetPlayerComponent;
import org.agmas.noellesroles.roles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.agmas.noellesroles.roles.robot.RobotPlayerComponent;
import org.agmas.noellesroles.roles.robber.RobberPlayerComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.agmas.noellesroles.roles.starstruck.StarstruckPlayerComponent;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapAuraWorldComponent;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapPlayerComponent;
import org.agmas.noellesroles.roles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.roles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.roles.waiter.WaiterPlayerComponent;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 时停者快照工具。
 *
 * <p>这里保存的是“可控运行态”，不是整张地图。
 * 每张快照包含：玩家基础状态、背包、物品冷却、Wathe/Noelles 常用组件、尸体、掉落物、门和火。
 * 这能覆盖当前需求里最容易影响局势的状态，同时避免每 4 tick 克隆全地图方块带来的性能风险。</p>
 */
public final class TimekeeperSnapshots {
    private static final List<ComponentEntry> PLAYER_COMPONENTS = List.of(
            component("wathe:mood", PlayerMoodComponent.KEY),
            /*
             * 厨师疯魔会临时提高体力上限并每 tick 回满体力。
             * 体力本身是局内运行态，回溯到疯魔前/中/后都应该恢复当时的 stamina 和 maxStaminaBonus。
             */
            component("wathe:stamina", PlayerStaminaComponent.KEY),
            component("wathe:shop", PlayerShopComponent.KEY),
            component("wathe:poison", PlayerPoisonComponent.KEY),
            component("wathe:psycho", PlayerPsychoComponent.KEY),
            component("wathe:life_state", PlayerLifeStateComponent.KEY),
            component("wathe:grenade_throw_mode", PlayerGrenadeComponent.KEY),
            component("wathe:instinct", PlayerInstinctComponent.KEY),
            component("wathe:note", PlayerNoteComponent.KEY),
            component("noellesroles:ability", AbilityPlayerComponent.KEY),
            component("noellesroles:death_process", DeathProcessComponent.KEY),
            component("noellesroles:delusion", DelusionPlayerComponent.KEY),
            component("noellesroles:doused", DousedPlayerComponent.KEY),
            component("noellesroles:bartender", BartenderPlayerComponent.KEY),
            component("noellesroles:morphling", MorphlingPlayerComponent.KEY),
            component("noellesroles:morph_mark_player", MorphMarkPlayerComponent.KEY),
            component("noellesroles:voodoo", VoodooPlayerComponent.KEY),
            component("noellesroles:convener", ConvenerPlayerComponent.KEY),
            component("noellesroles:convener_disguise", ConvenerDisguiseComponent.KEY),
            component("noellesroles:convener_momentum", ConvenerMomentumComponent.KEY),
            component("noellesroles:phantom", PhantomPlayerComponent.KEY),
            component("noellesroles:executioner", ExecutionerPlayerComponent.KEY),
            component("noellesroles:recaller", RecallerPlayerComponent.KEY),
            component("noellesroles:prophet", ProphetPlayerComponent.KEY),
            component("noellesroles:vulture", VulturePlayerComponent.KEY),
            component("noellesroles:chameleon", ChameleonPlayerComponent.KEY),
            component("noellesroles:noisemaker", NoisemakerPlayerComponent.KEY),
            component("noellesroles:noisemaker_glow_target", NoisemakerGlowTargetComponent.KEY),
            component("noellesroles:coroner", CoronerPlayerComponent.KEY),
            component("noellesroles:controller", ControllerPlayerComponent.KEY),
            component("noellesroles:controlled", ControlledPlayerComponent.KEY),
            component("noellesroles:stunned", StunnedPlayerComponent.KEY),
            component("noellesroles:stalker", StalkerPlayerComponent.KEY),
            component("noellesroles:bomber", BomberPlayerComponent.KEY),
            component("noellesroles:bounty_hunter", BountyHunterPlayerComponent.KEY),
            component("noellesroles:robber", RobberPlayerComponent.KEY),
            component("noellesroles:engineer", EngineerPlayerComponent.KEY),
            component("noellesroles:assassin", AssassinPlayerComponent.KEY),
            component("noellesroles:winder", WinderPlayerComponent.KEY),
            component("noellesroles:wind_mark", WindMarkPlayerComponent.KEY),
            component("noellesroles:operator", OperatorPlayerComponent.KEY),
            component("noellesroles:angel", AngelPlayerComponent.KEY),
            component("noellesroles:coward", CowardPlayerComponent.KEY),
            component("noellesroles:sedative", SedativePlayerComponent.KEY),
            component("noellesroles:rememberer", RemembererPlayerComponent.KEY),
            component("noellesroles:dreamer", DreamerComponent.KEY),
            component("noellesroles:dreamer_killer", DreamerKillerComponent.KEY),
            component("noellesroles:hacker", HackerComponent.KEY),
            component("noellesroles:hacker_phone", HackerPhoneComponent.KEY),
            component("noellesroles:waiter", WaiterPlayerComponent.KEY),
            component("noellesroles:cook", CookPlayerComponent.KEY),
            component("noellesroles:physician", PhysicianPlayerComponent.KEY),
            component("noellesroles:spiritualist", SpiritualistPlayerComponent.KEY),
            component("noellesroles:spiritualist_host", SpiritualistHostComponent.KEY),
            component("noellesroles:starstruck", StarstruckPlayerComponent.KEY),
            component("noellesroles:silence", SilencePlayerComponent.KEY),
            component("noellesroles:hunter", HunterPlayerComponent.KEY),
            component("noellesroles:drugmaker", DrugmakerPlayerComponent.KEY),
            component("noellesroles:kidnapper", KidnapperComponent.KEY),
            component("noellesroles:robot", RobotPlayerComponent.KEY),
            component("noellesroles:allergic", AllergicPlayerComponent.KEY),
            component("noellesroles:magician", MagicianPlayerComponent.KEY),
            /*
             * 亡语杀手尸体伪装是玩家局内运行态：
             * 30 秒前如果还没躺尸，回溯后就应该站起来；30 秒前如果已经躺尸，回溯后也应恢复该伪装。
             */
            component("noellesroles:insane_damned_paranoid_killer", InsaneDamnedKillerPlayerComponent.KEY),
            /*
             * 血斧开局冷却来源会影响客户端 tooltip 对剩余秒数的换算。
             * 物品冷却本身已经被时停者快照保存，这里同步保存来源标记，避免回溯后显示 30/45 秒总长错位。
             */
            component("noellesroles:spring_trap", SpringTrapPlayerComponent.KEY),
            /*
             * 杰森的倒地次数、失血/救治进度、汽油标记都属于局内运行态。
             * 回溯到命中前时必须清掉倒地/汽油；回溯到倒地中时也必须恢复暂停倒计时的状态。
             */
            component("noellesroles:jason_wounded", JasonWoundedPlayerComponent.KEY),
            /*
             * 无恶不在的冷却、进入/退出阶段、持续 tick 和惊吓状态都会改变局内信息与追逐节奏。
             * 时停者回溯到技能发动前时应清掉幽魂和惊吓；回溯到技能持续中时也应恢复当时的阶段进度。
             */
            component("noellesroles:jason_ability", JasonAbilityPlayerComponent.KEY),
            /*
             * 时停者自己的怀表冷却、光阴被动收入计时和时间狭缝剩余时间也属于“运行态”。
             * 因此它们要随快照倒回目标时间点；只有发动本次回溯产生的扣光阴、写冷却、
             * 普通怀表破碎等后置代价，会由 TimekeeperWorldComponent 的 actorPostUseState
             * 在每帧恢复后重新压回，避免时停者通过自己的回溯把发动成本抹掉。
             */
            component("noellesroles:timekeeper", TimekeeperPlayerComponent.KEY)
    );

    private static final List<ComponentEntry> WORLD_COMPONENTS = List.of(
            component("wathe:time", GameTimeComponent.KEY),
            component("wathe:train", TrainWorldComponent.KEY),
            component("wathe:blackout", WorldBlackoutComponent.KEY),
            component("wathe:roundend", GameRoundEndComponent.KEY),
            component("noellesroles:hidden_bodies", HiddenBodiesWorldComponent.KEY),
            component("noellesroles:hacker_safe_time", HackerSafeTimeComponent.KEY),
            component("noellesroles:avaricious_payout", AvariciousPayoutComponent.KEY),
            component("noellesroles:necromancer", NecromancerWorldComponent.KEY),
            component("noellesroles:lovers_pairs", LoversPairComponent.KEY),
            component("noellesroles:dual_personality", DualPersonalityComponent.KEY),
            /*
             * 试剂尸体来源记录要和尸体实体一起回溯。
             * 否则尸体本身虽然能由 TimekeeperSnapshots 恢复，客户端却不知道哪具尸体应该在本能视角显原貌。
             */
            component("noellesroles:morph_body_disguise_world", MorphBodyDisguiseWorldComponent.KEY),
            /*
             * 增速飞斧光环是会持续展开、发粒子并刷药水的局内运行态。
             * 把它纳入世界组件快照后，时停者回溯到 30 秒前时，光环位置、年龄和剩余持续时间都会随时间线倒回。
             */
            component("noellesroles:spring_trap_auras", SpringTrapAuraWorldComponent.KEY)
            ,
            /*
             * 投掷油桶落点、自动点火倒计时和火焰区域的年龄/位置都由该世界组件保存。
             * 纳入快照后能保证回溯不会留下未来时间线的火焰或汽油标记来源。
             */
            component("noellesroles:jason_fire", JasonFireWorldComponent.KEY),
            /*
             * 影子小丑的配对、双方阶段、任务计数、缔结申请和谢幕音乐主题都属于局内时间线。
             * 如果不纳入快照，时停者回溯后可能出现“玩家回到第三阶段前，组件仍停在第四阶段”的错位。
             */
            component("noellesroles:shadow_jester", ShadowJesterComponent.KEY)
    );

    private TimekeeperSnapshots() {
    }

    public static @NotNull GlobalSnapshot capture(@NotNull ServerWorld world) {
        return new GlobalSnapshot(world);
    }

    private static @NotNull ComponentEntry component(@NotNull String id, @NotNull ComponentKey<? extends Component> key) {
        return new ComponentEntry(id, key);
    }

    public static final class GlobalSnapshot {
        private final long worldTime;
        private final Map<String, NbtCompound> worldComponents = new LinkedHashMap<>();
        private final Map<UUID, PlayerSnapshot> players = new HashMap<>();
        private final Map<UUID, NbtCompound> bodyEntityData = new HashMap<>();
        private final Map<UUID, NbtCompound> itemEntityData = new HashMap<>();
        private final TimekeeperWorldStateSnapshot worldStateSnapshot;

        private GlobalSnapshot(@NotNull ServerWorld world) {
            RegistryWrapper.WrapperLookup registryLookup = world.getRegistryManager();
            this.worldTime = world.getTime();

            for (ComponentEntry entry : WORLD_COMPONENTS) {
                this.worldComponents.put(entry.id(), captureComponent(entry.key(), world, registryLookup));
            }
            this.worldStateSnapshot = TimekeeperWorldStateSnapshot.capture(world);

            for (ServerPlayerEntity player : world.getPlayers()) {
                this.players.put(player.getUuid(), new PlayerSnapshot(player));
            }

            for (PlayerBodyEntity body : world.getEntitiesByType(WatheEntities.PLAYER_BODY, body -> !body.isRemoved())) {
                NbtCompound bodyNbt = new NbtCompound();
                if (body.saveNbt(bodyNbt)) {
                    this.bodyEntityData.put(body.getUuid(), bodyNbt);
                }
            }

            for (ItemEntity item : world.getEntitiesByType(EntityType.ITEM, item -> !item.isRemoved())) {
                NbtCompound itemNbt = new NbtCompound();
                if (item.saveNbt(itemNbt)) {
                    this.itemEntityData.put(item.getUuid(), itemNbt);
                }
            }
        }

        private GlobalSnapshot(@NotNull GlobalSnapshot other) {
            this.worldTime = other.worldTime;
            this.worldStateSnapshot = new TimekeeperWorldStateSnapshot(other.worldStateSnapshot);
            copyNbtMap(other.worldComponents, this.worldComponents);
            copyPlayerMap(other.players, this.players);
            copyNbtMap(other.bodyEntityData, this.bodyEntityData);
            copyNbtMap(other.itemEntityData, this.itemEntityData);
        }

        public @NotNull GlobalSnapshot copy() {
            return new GlobalSnapshot(this);
        }

        public boolean hasPlayableAliveSnapshot(@NotNull UUID playerUuid) {
            PlayerSnapshot playerSnapshot = this.players.get(playerUuid);
            return playerSnapshot != null && playerSnapshot.isPlayableAlive();
        }

        public void apply(@NotNull ServerWorld world, @NotNull Set<UUID> protectedPlayers) {
            RegistryWrapper.WrapperLookup registryLookup = world.getRegistryManager();

            /*
             * 世界组件先恢复，玩家再恢复。
             * 这样玩家的任务、金币、语音和物品状态回写时，读到的是目标时间点的局内时间与世界运行态。
             * 不恢复 GameWorldComponent 本身，是为了避免 readFromNbt 里的角色变更回放副作用。
             */
            for (ComponentEntry entry : WORLD_COMPONENTS) {
                NbtCompound data = this.worldComponents.get(entry.id());
                if (data != null) {
                    restoreComponent(entry.key(), world, data, registryLookup);
                }
            }

            this.worldStateSnapshot.restore(world);
            restoreBodyEntities(world);
            restoreItemEntities(world);

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (protectedPlayers.contains(player.getUuid())) {
                    continue;
                }

                PlayerSnapshot playerSnapshot = this.players.get(player.getUuid());
                if (playerSnapshot == null) {
                    continue;
                }
                playerSnapshot.apply(player);
            }
        }

        private void restoreBodyEntities(@NotNull ServerWorld world) {
            Set<UUID> restored = new HashSet<>();

            for (PlayerBodyEntity body : world.getEntitiesByType(WatheEntities.PLAYER_BODY, body -> true)) {
                NbtCompound targetNbt = this.bodyEntityData.get(body.getUuid());
                if (targetNbt == null) {
                    body.discard();
                    continue;
                }
                body.readNbt(targetNbt.copy());
                restored.add(body.getUuid());
            }

            for (Map.Entry<UUID, NbtCompound> entry : this.bodyEntityData.entrySet()) {
                if (restored.contains(entry.getKey())) {
                    continue;
                }
                PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(world);
                if (body == null) {
                    continue;
                }
                body.readNbt(entry.getValue().copy());
                if (!body.isRemoved()) {
                    world.spawnEntity(body);
                }
            }
        }

        private void restoreItemEntities(@NotNull ServerWorld world) {
            Set<UUID> restored = new HashSet<>();

            for (ItemEntity item : world.getEntitiesByType(EntityType.ITEM, item -> true)) {
                NbtCompound targetNbt = this.itemEntityData.get(item.getUuid());
                if (targetNbt == null) {
                    item.discard();
                    continue;
                }
                item.readNbt(targetNbt.copy());
                restored.add(item.getUuid());
            }

            for (Map.Entry<UUID, NbtCompound> entry : this.itemEntityData.entrySet()) {
                if (restored.contains(entry.getKey())) {
                    continue;
                }
                ItemEntity item = new ItemEntity(world, 0.0D, 0.0D, 0.0D, ItemStack.EMPTY);
                item.readNbt(entry.getValue().copy());
                if (!item.isRemoved()) {
                    world.spawnEntity(item);
                }
            }
        }
    }

    public static final class PlayerSnapshot {
        private static final String ALIVE_IN_NON_SURVIVAL_MODE_KEY = "AliveInNonSurvivalMode";

        private final UUID uuid;
        private final boolean aliveAndSurvival;
        private final GameMode gameMode;
        private final boolean aliveOverride;
        private final boolean invisible;
        private final boolean noGravity;
        private final boolean noClip;
        private final Vec3d position;
        private final Vec3d velocity;
        private final float yaw;
        private final float pitch;
        private final float health;
        private final float absorption;
        private final int fireTicks;
        private final int frozenTicks;
        private final int air;
        private final int selectedSlot;
        private final NbtList inventory;
        private final ItemStack cursorStack;
        private final List<StatusEffectInstance> statusEffects;
        private final Map<Item, Integer> itemCooldownTicks;
        private final Map<String, NbtCompound> components = new LinkedHashMap<>();

        private PlayerSnapshot(@NotNull ServerPlayerEntity player) {
            RegistryWrapper.WrapperLookup registryLookup = player.getRegistryManager();
            this.uuid = player.getUuid();
            this.aliveAndSurvival = GameFunctions.isPlayerAliveAndSurvival(player);
            this.gameMode = player.interactionManager.getGameMode();

            NbtCompound lifeStateData = captureComponent(PlayerLifeStateComponent.KEY, player, registryLookup);
            this.aliveOverride = lifeStateData.getBoolean(ALIVE_IN_NON_SURVIVAL_MODE_KEY);

            this.invisible = player.isInvisible();
            this.noGravity = player.hasNoGravity();
            this.noClip = player.noClip;
            this.position = player.getPos();
            this.velocity = player.getVelocity();
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.health = player.getHealth();
            this.absorption = player.getAbsorptionAmount();
            this.fireTicks = player.getFireTicks();
            this.frozenTicks = player.getFrozenTicks();
            this.air = player.getAir();
            this.selectedSlot = player.getInventory().selectedSlot;
            this.inventory = player.getInventory().writeNbt(new NbtList());
            this.cursorStack = player.currentScreenHandler.getCursorStack().copy();
            this.statusEffects = copyStatusEffects(player.getStatusEffects());
            this.itemCooldownTicks = captureItemCooldownTicks(player);

            for (ComponentEntry entry : PLAYER_COMPONENTS) {
                this.components.put(entry.id(), captureComponent(entry.key(), player, registryLookup));
            }
        }

        private PlayerSnapshot(@NotNull PlayerSnapshot other) {
            this.uuid = other.uuid;
            this.aliveAndSurvival = other.aliveAndSurvival;
            this.gameMode = other.gameMode;
            this.aliveOverride = other.aliveOverride;
            this.invisible = other.invisible;
            this.noGravity = other.noGravity;
            this.noClip = other.noClip;
            this.position = other.position;
            this.velocity = other.velocity;
            this.yaw = other.yaw;
            this.pitch = other.pitch;
            this.health = other.health;
            this.absorption = other.absorption;
            this.fireTicks = other.fireTicks;
            this.frozenTicks = other.frozenTicks;
            this.air = other.air;
            this.selectedSlot = other.selectedSlot;
            this.inventory = other.inventory.copy();
            this.cursorStack = other.cursorStack.copy();
            this.statusEffects = copyStatusEffects(other.statusEffects);
            this.itemCooldownTicks = new HashMap<>(other.itemCooldownTicks);
            copyNbtMap(other.components, this.components);
        }

        private boolean isPlayableAlive() {
            /*
             * “真实可复活快照”必须同时满足两件事：
             * 1. Wathe 在该快照时间点认为玩家仍属于存活玩家；
             * 2. 玩家当时不在 spectator / creative 这类非正常游玩模式。
             *
             * 第二点很重要。时间狭缝会用 PlayerLifeStateApi 把 spectator 临时标成“玩法存活”，
             * 这样死者能在 30 秒内被回溯救回；但狭缝本身不是复活点。
             * 如果只看 aliveAndSurvival，就会把“特殊存活旁观”误判成真实活人，
             * 导致已经死透的玩家在第二次回溯里过早参与倒放，甚至回到另一个狭缝状态。
             */
            return this.aliveAndSurvival && !PlayerLifeStateApi.isNonSurvivalMode(this.gameMode);
        }

        private void apply(@NotNull ServerPlayerEntity player) {
            RegistryWrapper.WrapperLookup registryLookup = player.getRegistryManager();

            /*
             * 先恢复玩法生命授权，再切模式。
             * 如果快照本身就是“特殊存活旁观”，必须走 PlayerLifeStateApi，
             * 否则 Wathe 的 ServerPlayerEntity.changeGameMode mixin 会把 alive override 清掉。
             */
            if (PlayerLifeStateApi.isNonSurvivalMode(this.gameMode) && this.aliveOverride) {
                PlayerLifeStateApi.changeGameModeAsGameplayAlive(player, this.gameMode);
            } else {
                PlayerLifeStateApi.clearAliveOverride(player);
                if (player.interactionManager.getGameMode() != this.gameMode) {
                    player.changeGameMode(this.gameMode);
                }
            }

            player.teleport(player.getServerWorld(), this.position.x, this.position.y, this.position.z, this.yaw, this.pitch);
            player.setVelocity(this.velocity);
            player.velocityModified = true;
            /*
             * 恢复玩家实体底层 flag。
             *
             * 灵术师附身会直接使用 setInvisible/noGravity/noClip 把本体变成隐藏空气壳；
             * 这些标记不属于状态效果，也不会随组件 NBT 自动回滚。若不在快照里恢复，
             * 回溯到附身前的时间点时，组件可能已经是 NORMAL，本体却仍保留未来时间线的隐身标记。
             */
            player.setInvisible(this.invisible);
            player.setNoGravity(this.noGravity);
            player.noClip = this.noClip;
            player.setHealth(Math.max(1.0F, this.health));
            player.setAbsorptionAmount(this.absorption);
            player.setFireTicks(this.fireTicks);
            player.setFrozenTicks(this.frozenTicks);
            player.setAir(this.air);
            restoreStatusEffects(player);

            player.getInventory().clear();
            player.getInventory().readNbt(this.inventory.copy());
            player.getInventory().selectedSlot = Math.max(0, Math.min(this.selectedSlot, 8));
            player.currentScreenHandler.setCursorStack(this.cursorStack.copy());
            player.getInventory().markDirty();
            player.currentScreenHandler.sendContentUpdates();

            restoreItemCooldownTicks(player, this.itemCooldownTicks);

            boolean wasInTimeRiftBeforeRestore = TimekeeperPlayerComponent.KEY.get(player).isInTimeRift();

            for (ComponentEntry entry : PLAYER_COMPONENTS) {
                NbtCompound data = this.components.get(entry.id());
                if (data != null) {
                    restoreComponent(entry.key(), player, data, registryLookup);
                }
            }

            /*
             * 组件和实体 flag 都恢复后，再让灵术师本体状态做一次收口。
             * 这一步只处理灵术师自己的“空气壳运行期缓存”，避免回溯绕过 finishPossession 后，
             * invisible/noClip 或缓存标记滞留在错误时间点。
             */
            SpiritualistPlayerComponent.KEY.get(player).reconcileDetachedBodyShellAfterSnapshotRestore();

            /*
             * 回溯到玩家仍活着的快照时，时间狭缝中的死者应被拉回正常游戏。
             * 这里用恢复前的狭缝标记做判断，因为 TimekeeperPlayerComponent 已经随目标快照恢复：
             * 若目标点仍是活人，旧组件 NBT 通常会把 inTimeRift 还原为 false，
             * 但我们仍需要走一次专门出口来补语音频道和脱离狭缝提示。
             * 注意 Wathe 的 isPlayerAliveAndSurvival 也会把“特殊存活旁观”视作活人；
             * 时间狭缝本身正是这种状态，所以真正复活必须额外要求目标快照不是旁观/创造模式。
             */
            TimekeeperPlayerComponent timekeeperState = TimekeeperPlayerComponent.KEY.get(player);
            boolean targetWasPlayableAlive = isPlayableAlive();
            if (targetWasPlayableAlive && wasInTimeRiftBeforeRestore) {
                timekeeperState.finishTimeRiftAsRewoundAlive();
            }

            if (GameFunctions.isPlayerAliveAndSurvival(player)) {
                TrainVoicePlugin.resetPlayer(this.uuid);
            } else if (!timekeeperState.isInTimeRift()) {
                TrainVoicePlugin.addPlayer(this.uuid);
            } else {
                TrainVoicePlugin.resetPlayer(this.uuid);
            }
        }

        private void restoreStatusEffects(@NotNull ServerPlayerEntity player) {
            player.clearStatusEffects();
            for (StatusEffectInstance effect : this.statusEffects) {
                player.addStatusEffect(new StatusEffectInstance(effect));
            }
        }
    }

    private static @NotNull List<StatusEffectInstance> copyStatusEffects(@NotNull Collection<StatusEffectInstance> source) {
        List<StatusEffectInstance> result = new ArrayList<>();
        for (StatusEffectInstance effect : source) {
            result.add(new StatusEffectInstance(effect));
        }
        return result;
    }

    private static @NotNull Map<Item, Integer> captureItemCooldownTicks(@NotNull ServerPlayerEntity player) {
        Map<Item, Integer> cooldownTicks = new HashMap<>();
        ItemCooldownManager cooldownManager = player.getItemCooldownManager();
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) (Object) cooldownManager;
        int currentTick = accessor.noellesroles$getTick();
        for (Map.Entry<Item, Object> entry : accessor.noellesroles$getEntries().entrySet()) {
            int remainingTicks = ((ItemCooldownEntryAccessor) entry.getValue()).noellesroles$getEndTick() - currentTick;
            if (remainingTicks > 0) {
                cooldownTicks.put(entry.getKey(), remainingTicks);
            }
        }
        return cooldownTicks;
    }

    private static void restoreItemCooldownTicks(@NotNull ServerPlayerEntity player, @NotNull Map<Item, Integer> itemCooldownTicks) {
        ItemCooldownManager cooldownManager = player.getItemCooldownManager();
        List<Item> currentItems = new ArrayList<>(((ItemCooldownManagerAccessor) (Object) cooldownManager).noellesroles$getEntries().keySet());
        for (Item item : currentItems) {
            cooldownManager.remove(item);
        }
        for (Map.Entry<Item, Integer> entry : itemCooldownTicks.entrySet()) {
            if (entry.getValue() > 0) {
                cooldownManager.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private static @NotNull NbtCompound captureComponent(
            @NotNull ComponentKey<? extends Component> key,
            @NotNull Object provider,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        NbtCompound tag = new NbtCompound();
        key.get(provider).writeToNbt(tag, registryLookup);
        return tag;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restoreComponent(
            @NotNull ComponentKey<? extends Component> key,
            @NotNull Object provider,
            @NotNull NbtCompound tag,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        Component component = key.get(provider);
        component.readFromNbt(tag.copy(), registryLookup);
        ((ComponentKey) key).sync(provider);
    }

    private static <T> void copyNbtMap(@NotNull Map<T, NbtCompound> source, @NotNull Map<T, NbtCompound> target) {
        for (Map.Entry<T, NbtCompound> entry : source.entrySet()) {
            target.put(entry.getKey(), entry.getValue().copy());
        }
    }

    private static void copyPlayerMap(@NotNull Map<UUID, PlayerSnapshot> source, @NotNull Map<UUID, PlayerSnapshot> target) {
        for (Map.Entry<UUID, PlayerSnapshot> entry : source.entrySet()) {
            target.put(entry.getKey(), new PlayerSnapshot(entry.getValue()));
        }
    }

    private record ComponentEntry(@NotNull String id, @NotNull ComponentKey<? extends Component> key) {
    }
}
