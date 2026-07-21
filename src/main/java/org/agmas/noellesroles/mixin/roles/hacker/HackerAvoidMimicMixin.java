package org.agmas.noellesroles.mixin.roles.hacker;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 避免 Hacker 和 Mimic 同局随机生成。
 *
 * <p>这个 mixin 只观察 Harpy 本轮已经成功分配过的角色；
 * 一旦发现 Hacker/Mimic 其中一个已经落地，另一个在同轮随机分配时返回 0 人。</p>
 */
@Mixin(ModdedMurderGameMode.class)
public class HackerAvoidMimicMixin {
    @Unique
    private static final Set<Role> ASSIGNED_ROLES = new HashSet<>();
    @Unique
    private static boolean isAssigning = false;

    @Inject(method = "assignCivilianReplacingRoles", at = @At("HEAD"), remap = false)
    private void noellesroles$resetAssignedRoles(int desiredRoleCount, ServerWorld serverWorld, GameWorldComponent gameWorld, List<ServerPlayerEntity> players, CallbackInfo ci) {
        if (HackerConstants.GENERATE_WITH_MIMIC) {
            return;
        }
        isAssigning = true;
        ASSIGNED_ROLES.clear();
    }

    @Inject(method = "assignCivilianReplacingRoles", at = @At("RETURN"), remap = false)
    private void noellesroles$finishAssigning(int desiredRoleCount, ServerWorld serverWorld, GameWorldComponent gameWorld, List<ServerPlayerEntity> players, CallbackInfo ci) {
        if (HackerConstants.GENERATE_WITH_MIMIC) {
            return;
        }
        isAssigning = false;
    }

    @Inject(method = "findAndAssignPlayers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void noellesroles$preventConflictingRoles(int desiredRoleCount, @NotNull Role role, List<ServerPlayerEntity> players, GameWorldComponent gameWorld, World world, @NotNull CallbackInfoReturnable<Integer> cir) {
        if (HackerConstants.GENERATE_WITH_MIMIC || !isAssigning) {
            return;
        }
        for (Role assigned : ASSIGNED_ROLES) {
            if (conflicts(role, assigned)) {
                cir.setReturnValue(0);
                return;
            }
        }
    }

    @Inject(method = "findAndAssignPlayers", at = @At("RETURN"), remap = false)
    private static void noellesroles$recordAssignedRole(int desiredRoleCount, @NotNull Role role, List<ServerPlayerEntity> players, GameWorldComponent gameWorld, World world, @NotNull CallbackInfoReturnable<Integer> cir) {
        if (HackerConstants.GENERATE_WITH_MIMIC || !isAssigning) {
            return;
        }
        if (cir.getReturnValue() != null && cir.getReturnValue() > 0) {
            ASSIGNED_ROLES.add(role);
        }
    }

    @Unique
    private static boolean conflicts(Role role1, Role role2) {
        return (role1 == Noellesroles.HACKER && role2 == Noellesroles.MIMIC)
                || (role1 == Noellesroles.MIMIC && role2 == Noellesroles.HACKER);
    }
}
