package org.agmas.noellesroles.roles.executioner;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ExecutionerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<ExecutionerPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(NoellesRolesCore.MOD_ID, "executioner"), ExecutionerPlayerComponent.class);
    private final PlayerEntity player;
    public UUID target;
    public boolean won = false;


    public void reset() {
        this.target = player.getUuid();
        this.sync();
    }

    public ExecutionerPlayerComponent(PlayerEntity player) {
        this.player = player;
        target = player.getUuid();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void clientTick() {
    }

    public void serverTick() {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorldComponent.isRole(player, NoellesRoleRegistry.EXECUTIONER)) return;
        UUID previousTarget = this.target;
        PlayerEntity player1 = player.getWorld().getPlayerByUuid(target);
        UUID dualPersonalityPartner = DualPersonalityComponent.KEY.get(player.getWorld()).getPartner(player.getUuid());
        if (!isValidExecutionTarget(gameWorldComponent, player1)
                || java.util.Objects.equals(this.target, dualPersonalityPartner)
                || (GameFunctions.isPlayerEliminated(player1)) && !won) {
            List<UUID> validTargets = new ArrayList<>();
            WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(player.getWorld());
            LoversPairComponent loversPairComponent = LoversPairComponent.KEY.get(player.getWorld());
            List<UUID> lovers = modifierComponent.getAllWithModifier(NoellesModifierRegistry.LOVERS);
            gameWorldComponent.getRoles().forEach((uuid2,role1)->{
                if (uuid2 == null) return;
                PlayerEntity player2 = player.getWorld().getPlayerByUuid(uuid2);
                /*
                 * 恋人迁移到 Noelles 后，处刑人不能把自己的恋人抽成目标。
                 * 否则“恋人共生”和“处刑人希望目标死亡”会在同一名玩家身上互相冲突；
                 * 多对恋人存在时必须通过 LoversPairComponent 精确判断自己的伴侣，不能只看是否同为 LOVERS。
                 */
                if (loversPairComponent.arePartnersOrFallback(player.getUuid(), uuid2, lovers)) {
                    return;
                }
                /*
                 * 双重人格的另一人格与自己轮流操控同一具身体，本质上不是“外部仇杀对象”。
                 * 如果允许抽中 partner，仇杀客会被卡在无法通过目标死亡转杀手的状态。
                 */
                if (uuid2.equals(dualPersonalityPartner)) {
                    return;
                }
                if (isValidExecutionTarget(gameWorldComponent, player2, role1)) {
                    validTargets.add(uuid2);
                }
            });
            Collections.shuffle(validTargets);
            if (!validTargets.isEmpty()) {
                target = validTargets.getFirst();
            } else {
                target = player.getUuid();
            }
        }
        if (!java.util.Objects.equals(previousTarget, this.target) && player instanceof ServerPlayerEntity serverPlayer) {
            boolean previousWasRealTarget = previousTarget != null && !previousTarget.equals(player.getUuid());
            if (!previousWasRealTarget) {
                var lockedTarget = player.getServer().getPlayerManager().getPlayer(this.target);
                GameRecordManager.event(dev.doctor4t.wathe.record.GameRecordTypes.GLOBAL_EVENT)
                        .world(serverPlayer.getServerWorld())
                        .actor(serverPlayer)
                        .target(lockedTarget)
                        .put("event", NoellesEventIds.EXECUTIONER_TARGET_LOCKED_EVENT.toString())
                        .putUuid("locked_target", this.target)
                        .record();
            } else {
                GameRecordManager.event(dev.doctor4t.wathe.record.GameRecordTypes.GLOBAL_EVENT)
                        .world(serverPlayer.getServerWorld())
                        .actor(serverPlayer)
                        .put("event", NoellesEventIds.EXECUTIONER_TARGET_CHANGED_EVENT.toString())
                        .putUuid("old_target", previousTarget)
                        .putUuid("new_target", this.target)
                        .record();
            }
        }
        sync();
    }


    public void setTarget(UUID target) {
        this.target = target;
        this.sync();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putUuid("target", this.target);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.target = tag.contains("target") ? tag.getUuid("target") : player.getUuid();
    }

    private static boolean isValidExecutionTarget(GameWorldComponent gameWorldComponent, PlayerEntity target) {
        if (target == null) {
            return false;
        }
        return isValidExecutionTarget(gameWorldComponent, target, gameWorldComponent.getRole(target));
    }

    private static boolean isValidExecutionTarget(GameWorldComponent gameWorldComponent, PlayerEntity target, Role role) {
        if (target == null || role == null || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }
        /*
         * 仇杀客的目标不再只看 Role#isInnocent：
         * 1. 平民阵营和义警阵营仍然是原本意义上的“好人目标”；
         * 2. 独立中立拥有自己的独胜窗口，也需要在好人阵营全部死亡后继续成为可仇杀目标；
         * 3. 普通中立只做分组导出，不进入仇杀目标池。
         */
        if (role.getFaction() == Faction.CIVILIAN || role.getFaction() == Faction.VIGILANTE) {
            return !role.equals(NoellesRoleRegistry.MIMIC);
        }
        return NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(role);
    }
}
