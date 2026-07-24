package org.agmas.noellesroles.roles.hacker;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerPlayerComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * 写在所有玩家身上的黑客破解进度组件。
 *
 * <p>进度存在目标身上，而不是黑客身上，是为了让多个黑客/观察 HUD 都能读取同一份目标状态。
 * 只有服务端确认黑客正看着这个目标时才会推进计时。</p>
 */
public class HackerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<HackerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "hacker"),
            HackerComponent.class
    );

    private final PlayerEntity player;
    public int hackingTime = 0;

    public HackerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        checkIfTargetedByHacker();
        if (this.hackingTime > 0 && GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            reset();
        }
    }

    private void checkIfTargetedByHacker() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRunning()
                || HackerSafeTimeComponent.KEY.get(this.player.getWorld()).isSafe()
                || gameWorld.getRole(this.player) == null
                || HackerTargeting.countsAsFilteredKillerCohort(gameWorld, this.player)
                || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            return;
        }

        for (ServerPlayerEntity hacker : this.player.getServer().getPlayerManager().getPlayerList()) {
            if (!gameWorld.isRole(hacker, NoellesRoleRegistry.HACKER) || !GameFunctions.isPlayerAliveAndSurvival(hacker)) {
                continue;
            }
            if (isHackerLookingAtTarget(hacker)) {
                if (this.hackingTime <= HackerConstants.HACKING_TIME_TICKS) {
                    this.hackingTime++;
                    addRoleOnPhone(hacker);
                    sync();
                }
                return;
            }
        }
    }

    private boolean isHackerLookingAtTarget(@NotNull PlayerEntity hacker) {
        HitResult hitResult = ProjectileUtil.getCollision(
                hacker,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                2.0F
        );
        return hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == this.player;
    }

    private void addRoleOnPhone(@NotNull PlayerEntity hacker) {
        if (this.hackingTime != HackerConstants.HACKING_TIME_TICKS) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        Role targetRole = gameWorld.getRole(this.player);
        if (targetRole == null) {
            return;
        }

        /*
         * 按用户确认，破解奖励固定为 100 金币。
         * 不再保留 kinssaba 对 Taskmaster 的 150 金币分支，避免从 Noelles 反向读取外部词条。
         */
        PlayerShopComponent.KEY.get(hacker).addToBalance(HackerConstants.HACK_REWARD_COINS);

        for (ServerPlayerEntity recipient : this.player.getServer().getPlayerManager().getPlayerList()) {
            if (gameWorld.getRole(recipient) == null || !GameFunctions.isPlayerAliveAndSurvival(recipient)) {
                continue;
            }
            if (!gameWorld.canUseKillerFeatures(recipient) && !gameWorld.isRole(recipient, NoellesRoleRegistry.HACKER)) {
                continue;
            }

            recipient.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
            appendRevealedRoleToPhones(recipient, targetRole);
        }

        if (hacker instanceof ServerPlayerEntity hackerPlayer && this.player instanceof ServerPlayerEntity targetPlayer) {
            GameRecordManager.recordSkillUse(hackerPlayer, NoellesEventIds.HACKER_REVEAL_EVENT, targetPlayer, null);
        }
    }

    private void appendRevealedRoleToPhones(@NotNull ServerPlayerEntity recipient, @NotNull Role targetRole) {
        for (int i = 0; i < recipient.getInventory().size(); i++) {
            ItemStack stack = recipient.getInventory().getStack(i);
            if (!stack.isOf(ModItems.PHONE)) {
                continue;
            }

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            List<Text> loreLines = lore != null ? new ArrayList<>(lore.lines()) : new ArrayList<>();
            Text playerInfo = Text.literal(this.player.getName().getString() + " ")
                    .styled(style -> style.withItalic(false).withColor(0xFFFFFF))
                    .append(Harpymodloader.getRoleName(targetRole).copy().styled(style -> style.withItalic(false).withColor(targetRole.color())));
            if (!loreLines.contains(playerInfo)) {
                loreLines.add(playerInfo);
                stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
            }
        }
    }

    public static boolean refreshWeaponCooldown(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(ModItems.ICON_WEAPON_COOLDOWN_REFRESH, HackerConstants.REFRESH_WEAPON_COOLDOWN_TICKS);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        for (ServerPlayerEntity serverPlayer : player.getServer().getPlayerManager().getPlayerList()) {
            if (!gameWorld.canUseKillerFeatures(serverPlayer)) {
                continue;
            }

            serverPlayer.sendMessage(Text.translatable("tip.noellesroles.hacker.weapon_cooldown_refresh").withColor(Color.RED.getRGB()), true);
            serverPlayer.playSoundToPlayer(SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            clearCooldowns(serverPlayer,
                    WatheItems.KNIFE,
                    WatheItems.REVOLVER,
                    WatheItems.GRENADE,
                    ModItems.THROWING_AXE,
                    ModItems.ROBBER_PISTOL,
                    ModItems.BAYONET,
                    ModItems.SILENCED_REVOLVER,
                    ModItems.SILENT_GRENADE,
                    ModItems.TIMED_BOMB,
                    ModItems.LIGHTER,
                    ModItems.BLOWGUN,
                    ModItems.POISON_INJECTOR,
                    ModItems.KNOCKOUT_DRUG,
                    ModItems.TAPE
            );
            /*
             * 这三件迁移物品存在“开局 30 秒 / 普通 45 秒”两种冷却来源。
             * 黑客提前刷新武器冷却后，如果不把来源标记一起清掉，
             * 玩家立刻使用道具写入的 45 秒普通冷却会继续被 tooltip 误按 30 秒显示。
             */
            DrugmakerPlayerComponent.KEY.get(serverPlayer).clearStartCooldowns();
            KidnapperComponent.KEY.get(serverPlayer).clearKnockoutDrugStartCooldown();

            if (FabricLoader.getInstance().isModLoaded("harpysimpleroles")) {
                clearCooldowns(serverPlayer,
                        Registries.ITEM.get(Identifier.of("harpysimpleroles", "toxin")),
                        Registries.ITEM.get(Identifier.of("harpysimpleroles", "bandit_revolver"))
                );
            }
            if (FabricLoader.getInstance().isModLoaded("starexpress")) {
                clearCooldowns(serverPlayer, Registries.ITEM.get(Identifier.of("starexpress", "tape")));
            }
        }
        return true;
    }

    public static boolean refreshAbilityCooldown(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(ModItems.ICON_ABILITY_COOLDOWN_REFRESH, HackerConstants.REFRESH_ABILITY_COOLDOWN_TICKS);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        for (ServerPlayerEntity serverPlayer : player.getServer().getPlayerManager().getPlayerList()) {
            if (!gameWorld.canUseKillerFeatures(serverPlayer)) {
                continue;
            }

            serverPlayer.sendMessage(Text.translatable("tip.noellesroles.hacker.ability_cooldown_refresh").withColor(Color.GREEN.getRGB()), true);
            serverPlayer.playSoundToPlayer(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            serverPlayer.getItemCooldownManager().remove(WatheItems.PSYCHO_MODE);
            AbilityPlayerComponent.KEY.get(serverPlayer).setCooldown(0);
        }
        return true;
    }

    public static boolean refreshPotionEffect(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(ModItems.ICON_POTION_EFFECT_REFRESH, HackerConstants.REFRESH_POTION_EFFECT_TICKS);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        for (ServerPlayerEntity serverPlayer : player.getServer().getPlayerManager().getPlayerList()) {
            if (!gameWorld.canUseKillerFeatures(serverPlayer)) {
                continue;
            }

            serverPlayer.sendMessage(Text.translatable("tip.noellesroles.hacker.potion_effect_refresh").withColor(Color.YELLOW.getRGB()), true);
            serverPlayer.playSoundToPlayer(SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 1.0F, 1.0F);
            for (StatusEffectInstance effect : new ArrayList<>(serverPlayer.getStatusEffects())) {
                if (!StatusEffects.INVISIBILITY.equals(effect.getEffectType())) {
                    serverPlayer.removeStatusEffect(effect.getEffectType());
                }
            }
            DelusionPlayerComponent.KEY.get(serverPlayer).reset();
            SedativePlayerComponent.KEY.get(serverPlayer).reset();
        }
        return true;
    }

    private static void clearCooldowns(@NotNull PlayerEntity player, Item... items) {
        for (Item item : items) {
            player.getItemCooldownManager().remove(item);
        }
    }

    public void reset() {
        this.hackingTime = 0;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("hackingTime", this.hackingTime);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hackingTime = tag.contains("hackingTime") ? tag.getInt("hackingTime") : 0;
    }
}
