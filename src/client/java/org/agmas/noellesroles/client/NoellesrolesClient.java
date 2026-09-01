package org.agmas.noellesroles.client;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import com.google.common.collect.Maps;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.GrenadeThrowModePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.WitherSkullEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.NoellesRolesParticles;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.client.particle.StarstruckSparkleParticle;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceHandlers;
import org.agmas.noellesroles.client.crosshair.NoellesCrosshairHandlers;
import org.agmas.noellesroles.client.hud.NoellesHudHandlers;
import org.agmas.noellesroles.client.movement.NoellesClientMovementBootstrap;
import org.agmas.noellesroles.client.renderer.CaptureDeviceEntityRenderer;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.client.renderer.JasonThrownWeaponEntityRenderer;
import org.agmas.noellesroles.client.renderer.MagicianPlaybackEntityRenderer;
import org.agmas.noellesroles.client.renderer.RoleMineEntityRenderer;
import org.agmas.noellesroles.client.renderer.ThrowingAxeEntityRenderer;
import org.agmas.noellesroles.client.renderer.ThrowingPanEntityRenderer;
import org.agmas.noellesroles.client.roles.executioner.ExecutionerMoodHud;
import org.agmas.noellesroles.client.roles.dreamer.DreamerMoodHud;
import org.agmas.noellesroles.client.roles.hacker.HackerMoodHud;
import org.agmas.noellesroles.client.roles.cook.CookMoodHud;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientEffects;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientSoundController;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityFogHandler;
import org.agmas.noellesroles.client.roles.jason.JasonMoodHud;
import org.agmas.noellesroles.client.roles.jester.JesterMoodHud;
import org.agmas.noellesroles.client.roles.lich.LichMoodHud;
import org.agmas.noellesroles.client.roles.vecna.VecnaMoodHud;
import org.agmas.noellesroles.client.roles.licensed_villain.LicensedVillainMoodHud;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.agmas.noellesroles.client.roles.rememberer.RemembererMoodHud;
import org.agmas.noellesroles.client.roles.robot.RobotMoodHud;
import org.agmas.noellesroles.client.roles.convener.ConvenerMoodHud;
import org.agmas.noellesroles.client.roles.shadow_jester.ShadowJesterMoodHud;
import org.agmas.noellesroles.client.roles.shadow_jester.ShadowJesterMusicController;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityClientState;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityKeybinds;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityTimeHud;
import org.agmas.noellesroles.client.roles.starstruck.StarstruckMoodHud;
import org.agmas.noellesroles.client.roles.spring_trap.SpringTrapMoodHud;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.agmas.noellesroles.client.ui.modifiers.guesser.GuesserPlayerWidget;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerState;
import org.agmas.noellesroles.client.ui.roles.operator.OperatorPlayerWidget;
import org.agmas.noellesroles.client.ui.roles.swapper.SwapperPlayerWidget;
import org.agmas.noellesroles.client.visibility.NoellesHeldItemVisibilityHandlers;
import org.agmas.noellesroles.packet.host.AbilityC2SPacket;
import org.agmas.noellesroles.packet.role.jason.JasonAbilitySoundS2CPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerDashC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerGazeC2SPacket;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionViewS2CPacket;
import org.agmas.noellesroles.packet.role.timekeeper.TimekeeperWatchModeC2SPacket;
import org.agmas.noellesroles.packet.role.vulture.VultureEatC2SPacket;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.roles.angel.AngelAbility;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistTargeting;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;
import org.agmas.noellesroles.roles.lich.LichConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;
import org.lwjgl.glfw.GLFW;

import org.agmas.noellesroles.client.items.NoellesRolesItemToolTip;
import org.agmas.noellesroles.client.items.NoellesRolesItemExtraModel;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtons;

import java.util.*;

public class NoellesrolesClient implements ClientModInitializer {


    public static int insanityTime = 0;
    public static KeyBinding abilityBind;
    public static PlayerBodyEntity targetBody;
    // 在 NoellesrolesClient 类中添加变量
    private static boolean wasGazingPressed = false;
    private static boolean wasChargingPressed = false;
    private static boolean wasUsingKnife = false;
    private static boolean grenadeThrowModeToggleHeld = false;
    private static boolean watchModeToggleHeld = false;
    private static int lastThrowableGrenadeSelectedSlot = -1;

    public static Map<UUID, UUID> SHUFFLED_PLAYER_ENTRIES_CACHE = Maps.newHashMap();
    public static SkinTextures LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES = null;


    @Override
    public void onInitializeClient() {
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + NoellesRolesCore.MOD_ID + ".ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.wathe.keybinds"));
        NoellesInstinctHandlers.register();
        NoellesAppearanceHandlers.register();
        NoellesHudHandlers.register();
        NoellesCrosshairHandlers.register();
        NoellesClientMovementBootstrap.init();
        DualPersonalityTimeHud.register();
        DualPersonalityKeybinds.init();
        NoellesHeldItemVisibilityHandlers.register();
        NoellesInventoryButtons.register();
        ExecutionerMoodHud.register();
        JesterMoodHud.register();
        RemembererMoodHud.register();
        DreamerMoodHud.register();
        HackerMoodHud.register();
        CookMoodHud.register();
        StarstruckMoodHud.register();
        RobotMoodHud.register();
        ConvenerMoodHud.register();
        LicensedVillainMoodHud.register();
        SpringTrapMoodHud.register();
        JasonMoodHud.register();
        LichMoodHud.register();
        VecnaMoodHud.register();
        JasonAbilityFogHandler.register();
        ShadowJesterMoodHud.register();
        /*
         * 弹簧陷阱疯魔的环境音是 profile 触发的背景音。
         * 客户端需要先把 SoundEvent 注册进 Wathe 的疯魔音乐表，否则服务端状态同步过来后只能拿到 id，
         * 本地没有对应播放器就不会真正循环播放 spring_trap.ogg。
         */
        dev.doctor4t.wathe.api.client.psycho.PsychoModeClientApi.registerBackgroundAmbience(NoellesRolesSounds.AMBIENT_SPRING_TRAP, 20);
        /*
         * 杰森模式同样使用 Wathe 疯魔背景音乐播放器；这里只注册本地 SoundEvent，
         * 真正何时播放/停止由服务端同步的 Jason profile 决定。
         */
        dev.doctor4t.wathe.api.client.psycho.PsychoModeClientApi.registerBackgroundAmbience(NoellesRolesSounds.AMBIENT_JASON, 20);
        /*
         * 巫妖疯魔音乐也走 Wathe 的统一疯魔背景音播放器。
         * 服务端 profile 只同步 SoundEvent id，客户端必须先注册可循环播放的本地播放器。
         */
        dev.doctor4t.wathe.api.client.psycho.PsychoModeClientApi.registerBackgroundAmbience(
                NoellesRolesSounds.AMBIENT_LICH,
                LichConstants.PSYCHO_LICH_BACKGROUND_FADE_TICKS
        );
        dev.doctor4t.wathe.api.client.psycho.PsychoModeClientApi.registerBackgroundAmbience(NoellesRolesSounds.AMBIENT_VECNA, 20);
        ParticleFactoryRegistry.getInstance().register(NoellesRolesParticles.STARSTRUCK_SPARKLE, StarstruckSparkleParticle.Provider::new);
        // 服务员商店图标和可服务物品的客户端外观/提示都在这里统一注册。
        registerItemColors();
        registerItemTooltipsAndModels();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> noellesroles$resetClientCaches());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> noellesroles$resetClientCaches());
        SpiritualistClientController.init();
        ClientPlayNetworking.registerGlobalReceiver(SpiritualistPossessionViewS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SpiritualistClientController.handlePossessionViewPacket(payload)));
        ClientPlayNetworking.registerGlobalReceiver(JasonAbilitySoundS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> JasonAbilityClientSoundController.handle(context.client(), payload.action())));

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> noellesroles$handlePreAttack(player));



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RemembererClientEffects.tick(client);
            CowardClientEffects.tick(client);
            JasonAbilityClientEffects.tick(client);
            ShadowJesterMusicController.tick(client);
            /*
             * 无恶不在持续音需要由所有客户端本地播放。
             * 服务端仍会发 START/STOP 包作为即时控制，但这里按同步组件做一层补偿，
             * 避免网络包顺序或初始 0 音量声音实例导致 jason_ability_last 没有真正进入播放队列。
             */
            JasonAbilityClientSoundController.tick(client);
            // 在 ClientTickEvents.END_CLIENT_TICK.register(client -> { ... } 中：
            if (abilityBind.isPressed()) {
                if (!wasGazingPressed) {
                    wasGazingPressed = true;
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(mc.player.getWorld());
                        if (gameWorld.isRole(mc.player, NoellesRoleRegistry.STALKER)) {
                            StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(mc.player);
                            if (comp.phase < 3) {
                                ClientPlayNetworking.send(new StalkerGazeC2SPacket(true));
                            } else {
                                ClientPlayNetworking.send(new StalkerDashC2SPacket(true));
                            }
                        }
                    }
                }
            } else {
                if (wasGazingPressed) {
                    wasGazingPressed = false;
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(mc.player.getWorld());
                        if (gameWorld.isRole(mc.player, NoellesRoleRegistry.STALKER)) {
                            StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(mc.player);
                            if (comp.phase < 3) {
                                ClientPlayNetworking.send(new StalkerGazeC2SPacket(false));
                            } else {
                                ClientPlayNetworking.send(new StalkerDashC2SPacket(false));
                            }
                        }
                    }
                }
            }


            // 潜行者三阶段：右键刀蓄力检测
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                GameWorldComponent gameWorld = GameWorldComponent.KEY.get(mc.player.getWorld());
                if (gameWorld.isRole(mc.player, NoellesRoleRegistry.STALKER)) {
                    StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(mc.player);
                    if (comp.phase == 3) {
                        boolean isUsingKnife = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(WatheItems.KNIFE);
                        if (isUsingKnife && !wasUsingKnife) {
                            // 开始蓄力
                            ClientPlayNetworking.send(new StalkerDashC2SPacket(true));
                            wasUsingKnife = true;
                        } else if (!isUsingKnife && wasUsingKnife) {
                            // 停止蓄力（释放）
                            ClientPlayNetworking.send(new StalkerDashC2SPacket(false));
                            wasUsingKnife = false;
                        }
                    } else {
                        // 确保重置状态，防止误判
                        wasUsingKnife = false;
                    }
                } else {
                    wasUsingKnife = false;
                }

                noellesroles$maybeShowGrenadeThrowModeHint(mc.player);
                noellesroles$refreshLookedAtBody(mc.player);
            }

            if (!client.options.attackKey.isPressed()) {
                grenadeThrowModeToggleHeld = false;
                watchModeToggleHeld = false;
            }


            insanityTime++;
            noellesroles$refreshLocalOriginalSkinCache(client);
            if (insanityTime >= 20*6) {
                insanityTime = 0;
                if (WatheClient.PLAYER_ENTRIES_CACHE == null || WatheClient.PLAYER_ENTRIES_CACHE.isEmpty()) {
                    SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
                    return;
                }
                List<UUID> keys = new ArrayList<UUID>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
                List<UUID> originalkeys = new ArrayList<UUID>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
                SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
                Collections.shuffle(keys);
                int i = 0;
                for (UUID o : originalkeys) {
                    SHUFFLED_PLAYER_ENTRIES_CACHE.put(o, keys.get(i));
                    i++;
                }
            }
            if (abilityBind.wasPressed()) {
                client.execute(() -> {
                    if (MinecraftClient.getInstance().player == null) return;
                    GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, NoellesRoleRegistry.VULTURE)) {
                        if (targetBody == null) return;
                        ClientPlayNetworking.send(new VultureEatC2SPacket(targetBody.getUuid()));
                        return;
                    }

                    /*
                     * 天使 / 灵术师这类“同一枚能力键会根据是否锁定到玩家而切模式”的职业，
                     * 这里把客户端这一帧真正命中的目标 id 一起发给服务端。
                     * 这样服务端只需要再做一次距离和合法性校验，
                     * 就不会因为目标横向移动而把原本应当触发的对人技能误判成另一种模式。
                     */
                    int targetId = -1;
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, NoellesRoleRegistry.ANGEL)) {
                        PlayerEntity angelTarget = AngelAbility.getGenericGuardTarget(MinecraftClient.getInstance().player);
                        if (angelTarget != null) {
                            targetId = angelTarget.getId();
                        }
                    } else if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, NoellesRoleRegistry.SPIRITUALIST)) {
                        PlayerEntity spiritualistTarget = SpiritualistTargeting.getPossessionTarget(MinecraftClient.getInstance().player);
                        if (spiritualistTarget != null) {
                            targetId = spiritualistTarget.getId();
                        }
                    } else if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, NoellesRoleRegistry.SHADOW_JESTER)) {
                        /*
                         * 影子小丑的缔结申请依赖准心目标。
                         * 客户端只把“这一帧看见的实体 id”发给服务端；服务端仍会重新校验职业、阶段、距离和伴侣关系。
                         */
                        EntityHitResult shadowJesterTarget = WeaponTargetingApi.getVisibleAlivePlayerTarget(
                                MinecraftClient.getInstance().player,
                                ShadowJesterConstants.VOW_TARGET_RANGE_BLOCKS
                        );
                        if (shadowJesterTarget != null && shadowJesterTarget.getEntity() instanceof PlayerEntity targetPlayer) {
                            targetId = targetPlayer.getId();
                        }
                    } else if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, NoellesRoleRegistry.VECNA)) {
                        EntityHitResult target = WeaponTargetingApi.getVisibleAlivePlayerTarget(MinecraftClient.getInstance().player, 2.0F);
                        if (target != null) targetId = target.getEntity().getId();
                    }
                    ClientPlayNetworking.send(new AbilityC2SPacket(targetId));
                });
            }
        });
        EntityRendererRegistry.register(NoellesRolesEntities.ROLE_MINE_ENTITY_ENTITY_TYPE, RoleMineEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.CAPTURE_DEVICE_ENTITY_TYPE, CaptureDeviceEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.THROWING_AXE_ENTITY_TYPE, ThrowingAxeEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.THROWING_PAN_ENTITY_TYPE, ThrowingPanEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.JASON_THROWN_WEAPON_ENTITY_TYPE, JasonThrownWeaponEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.MAGICIAN_PLAYBACK_ENTITY_TYPE, MagicianPlaybackEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.LICH_SKELETON_SKULL_ENTITY_TYPE, WitherSkullEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.LICH_MAGIC_BARRIER_ENTITY_TYPE, EmptyEntityRenderer::new);
    }

    private static void registerItemColors() {
        // random_potion 的 layer0 用药水颜色，layer1 仍然复用原版药水瓶模型。
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> tintIndex == 0 ? WaiterConstants.REGENERATION_POTION_COLOR : -1,
                ModItems.RANDOM_POTION
        );
    }

    private void registerItemTooltipsAndModels() {
        registerTimekeeperWatchCooldownTooltip();

        ItemTooltipCallback.EVENT.register(((itemStack, tooltipContext, tooltipType, list) -> {
            // 为 NoellesRoles 的所有物品添加提示（描述 + 冷却）
            NoellesRolesItemToolTip.addItemtip(ModItems.TOOLBOX, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.CAPTURE_DEVICE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.POWER_RESTORATION, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.FAKE_KNIFE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.FAKE_GRENADE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.FAKE_REVOLVER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.MASTER_KEY, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.DELUSION_VIAL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.WIND_MARK, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.DEFENSE_VIAL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SEDATIVE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ROLE_MINE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.TIMED_BOMB, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_PAN, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BLOOD_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.COLORFUL_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_SPEED_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_BOMB_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SPRING_TRAP, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SPRING_TRAP_ADDTIME, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_BLOOD_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_MACHETE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.TOMAHAWK, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_TOYS_AXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_PICKAXE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.THROWING_JERRY_CAN, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ONCE_LIGHTER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.RANDOM_THROWING_WEAPON, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PSYCHO_JASON, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PSYCHO_COOK, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.CRYSTAL_BALL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ROBBER_PISTOL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BOUNTY_PISTOL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BOUNTY_DERRINGER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BOUNTY_MODE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BAYONET, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SILENCED_REVOLVER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SILENT_GRENADE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SNIPER_RIFLE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SNIPER_RIFLE_BULLET, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BAYONET_COLDOWN_REFRESH, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.DREAM_IMPRINT, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.MEDICAL_KIT, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PAN, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PSYCHO_THROWING_PAN, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PILL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.TAPE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.HUNTING_KNIFE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SULFURIC_ACID_BARREL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BLOWGUN, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.POISON_INJECTOR, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.DELUSION_SYRINGE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.KNOCKOUT_DRUG, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.JERRY_CAN, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.LIGHTER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ONCE_STAFF, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PSYCHO_STAFF, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.MAGIC_BARRIER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PSYCHO_LICH, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PHONE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ICON_WEAPON_COOLDOWN_REFRESH, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ICON_ABILITY_COOLDOWN_REFRESH, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ICON_POTION_EFFECT_REFRESH, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.DYING_WATCH_PROTECT, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SLEEPING_BAG, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BOOK, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.RANDOM_FOOD, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.RANDOM_DRINK, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.RANDOM_POTION, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.PSYCHO_VECNA, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.VECNA_ADDTIME, itemStack, list);
        }));

        // 为需要额外模型的物品注册（目前所有物品都注册冷却模型，方便未来扩展）
        // 可以只注册有冷却的物品，但全部注册也无妨
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.TOOLBOX);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.CAPTURE_DEVICE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.POWER_RESTORATION);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.FAKE_KNIFE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.FAKE_GRENADE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.FAKE_REVOLVER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.MASTER_KEY);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.DELUSION_VIAL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.WIND_MARK);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.DEFENSE_VIAL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SEDATIVE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ROLE_MINE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.TIMED_BOMB);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_PAN);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BLOOD_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.COLORFUL_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_SPEED_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_BOMB_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SPRING_TRAP);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SPRING_TRAP_ADDTIME);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_BLOOD_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_MACHETE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.TOMAHAWK);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_TOYS_AXE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.THROWING_PICKAXE);
        NoellesRolesItemExtraModel.registerJasonJerryCanModel(ModItems.THROWING_JERRY_CAN);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ONCE_LIGHTER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.RANDOM_THROWING_WEAPON);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PSYCHO_JASON);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PSYCHO_COOK);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.CRYSTAL_BALL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ROBBER_PISTOL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BOUNTY_PISTOL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BOUNTY_DERRINGER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BOUNTY_MODE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BAYONET);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SILENCED_REVOLVER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SILENT_GRENADE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SNIPER_RIFLE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SNIPER_RIFLE_BULLET);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.DREAM_IMPRINT);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.MEDICAL_KIT);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PAN);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PSYCHO_THROWING_PAN);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PILL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.TAPE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.HUNTING_KNIFE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SULFURIC_ACID_BARREL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BLOWGUN);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.POISON_INJECTOR);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.DELUSION_SYRINGE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.KNOCKOUT_DRUG);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.JERRY_CAN);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.LIGHTER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ONCE_STAFF);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PSYCHO_STAFF);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.MAGIC_BARRIER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.PSYCHO_LICH);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ICON_WEAPON_COOLDOWN_REFRESH);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ICON_ABILITY_COOLDOWN_REFRESH);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ICON_POTION_EFFECT_REFRESH);
        NoellesRolesItemExtraModel.registerTimekeeperWatchModel(ModItems.DYING_WATCH);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.DYING_WATCH_PROTECT);
        NoellesRolesItemExtraModel.registerPhoneModel(ModItems.PHONE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SLEEPING_BAG);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BOOK);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.RANDOM_FOOD);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.RANDOM_DRINK);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.RANDOM_POTION);
    }

    private static void registerTimekeeperWatchCooldownTooltip() {
        TimekeeperWatchItem.setCooldownTooltipProvider((stack, mode) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return Text.translatable("item.noellesroles.dying_watch.tooltip.cooldown.ready");
            }

            /*
             * 怀表冷却不是原版 ItemCooldownManager，而是时停者自己的三模式冷却。
             * 服务端把 TimekeeperPlayerComponent 只同步给玩家本人，所以这里在客户端 tooltip 阶段
             * 直接读取本地玩家组件，就能精确显示当前选中模式的剩余秒数。
             */
            int cooldownTicks = TimekeeperPlayerComponent.KEY.get(client.player).getCooldownTicks(mode);
            if (cooldownTicks <= 0) {
                return Text.translatable("item.noellesroles.dying_watch.tooltip.cooldown.ready");
            }

            int cooldownSeconds = Math.max(1, (cooldownTicks + 19) / 20);
            return Text.translatable("item.noellesroles.dying_watch.tooltip.cooldown.seconds", cooldownSeconds);
        });
    }

    /**
     * noellesroles 需要优先接管的客户端左键逻辑。
     *
     * <p>Fabric 的 ClientPreAttackCallback 发生在客户端真正挥拳/攻击前。
     * 在这里返回 true 就等于“这次攻击已经被模组处理”，客户端不会继续把左键当成普通攻击。
     * 狙击枪开镜本身由每 tick 读取 attackKey 的按住状态控制，这里只负责吞掉攻击，
     * 避免玩家按住左键开镜时误伤附近玩家或破坏方块。</p>
     */
    private static boolean noellesroles$handlePreAttack(PlayerEntity player) {
        if (player.getMainHandStack().isOf(ModItems.SNIPER_RIFLE)) {
            return true;
        }
        if (noellesroles$handleTimekeeperWatchModeSwitch(player)) {
            return true;
        }
        return noellesroles$handleGrenadeThrowModeSwitch(player);
    }

    /**
     * 时停者怀表左键切换模式。
     *
     * <p>这里和手雷模式切换一样在客户端预攻击阶段吞掉左键，
     * 这样玩家不会因为想切怀表模式而误攻击玩家或破坏方块。
     * 本地先写一次数据组件只是为了 tooltip/actionbar 即刻更新；
     * 服务端收到包后会重新校验并写回权威状态。</p>
     */
    private static boolean noellesroles$handleTimekeeperWatchModeSwitch(PlayerEntity player) {
        if (!player.getMainHandStack().isOf(ModItems.DYING_WATCH)) {
            return false;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)) {
            return false;
        }

        if (watchModeToggleHeld) {
            return true;
        }
        watchModeToggleHeld = true;

        TimekeeperWatchMode nextMode = TimekeeperWatchItem.getMode(player.getMainHandStack()).next();
        TimekeeperWatchItem.setMode(player.getMainHandStack(), nextMode);
        player.sendMessage(
                Text.translatable("message.noellesroles.timekeeper.current_watch_mode", nextMode.text())
                        .withColor(TimekeeperConstants.ROLE_COLOR),
                true
        );
        ClientPlayNetworking.send(new TimekeeperWatchModeC2SPacket(nextMode.ordinal()));
        return true;
    }

    /**
     * 复刻 Wathe 原版手雷的左键切模式体验，并把 noellesroles 的扩展手雷一起接进来。
     *
     * <p>目前支持：
     * 1. 无声手雷；
     * 2. 假手雷。</p>
     */
    private static boolean noellesroles$handleGrenadeThrowModeSwitch(PlayerEntity player) {
        if (!noellesroles$isThrowableGrenade(player.getMainHandStack())) {
            return false;
        }

        /*
         * 扩展手雷也必须和 Wathe 原版手雷保持同一条规则：
         * 非存活旁观玩家左键点击其他玩家时，优先交还给原版 spectator 附身逻辑。
         *
         * 否则玩家死亡时如果手里残留无声手雷 / 假手雷，
         * 左键想附身观战会被这里吞掉并切换投掷模式，表现上就像“无法附身”。
         *
         * 这里使用 GameFunctions.isPlayerAliveAndSurvival，而不是直接判断 !isSpectator()：
         * Wathe 允许部分职业/调试逻辑把 spectator 或 creative 标记为“玩法仍存活”，
         * 这些特殊存活玩家仍然应该保留扩展手雷的模式切换能力。
         */
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }

        /*
         * 长按左键时 Fabric 会连续触发预攻击回调。
         * 这里沿用 Wathe 的“按住锁”做法，同一次按住只允许切换一次模式，
         * 并且整个按住期间都吞掉攻击动作，避免误打到别人。
         */
        if (grenadeThrowModeToggleHeld) {
            return true;
        }
        grenadeThrowModeToggleHeld = true;

        PlayerGrenadeComponent component = PlayerGrenadeComponent.KEY.get(player);
        component.toggleLocal();
        ClientPlayNetworking.send(new GrenadeThrowModePayload(component.isDirectThrowMode()));
        WatheClient.showGrenadeThrowModeSwitchMessage(player);
        return true;
    }

    /**
     * 当玩家刚切到可切模式的扩展手雷所在栏位时，提示当前投掷模式。
     *
     * <p>这样玩家不用试扔一次，就能立刻确认自己当前是直投还是蓄力。</p>
     */
    private static void noellesroles$maybeShowGrenadeThrowModeHint(PlayerEntity player) {
        int currentSlot = player.getInventory().selectedSlot;
        boolean isHoldingThrowableGrenade = noellesroles$isThrowableGrenade(player.getMainHandStack());
        boolean canUseGrenadeThrowMode = GameFunctions.isPlayerAliveAndSurvival(player);
        if (isHoldingThrowableGrenade && canUseGrenadeThrowMode && lastThrowableGrenadeSelectedSlot != currentSlot) {
            WatheClient.showGrenadeThrowModeMessage(player, "tip.grenade.current_throw_mode");
            lastThrowableGrenadeSelectedSlot = currentSlot;
        } else if (!isHoldingThrowableGrenade || !canUseGrenadeThrowMode) {
            lastThrowableGrenadeSelectedSlot = -1;
        }
    }

    /**
     * noellesroles 当前接入 Wathe 手雷双模式系统的扩展手雷名单。
     *
     * <p>统一收口到这里，后续再新增类似手雷时只改一处即可。</p>
     */
    private static boolean noellesroles$isThrowableGrenade(net.minecraft.item.ItemStack stack) {
        return stack.isOf(ModItems.SILENT_GRENADE) || stack.isOf(ModItems.FAKE_GRENADE);
    }

    private static void noellesroles$refreshLocalOriginalSkinCache(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        // 这里只在“本地玩家当前没有任何外观覆盖”时刷新原始皮肤缓存。
        // 原因是互相变形时，别人如果伪装成本地玩家自己，
        // 我们必须拿到“这个 UUID 原本的皮肤”，而不是“本地玩家当前已经显示成谁”。
        // 如果在自己正处于变形状态时继续覆盖这份缓存，就会把错误的伪装皮肤反写进去，
        // 从而出现你测试到的“双方互相看对方还是各自原样/错误样貌”的问题。
        if (DisguiseRenderHelper.resolveAppearanceOverrideSkinTextures(client.player) != null) {
            return;
        }

        if (client.player.networkHandler != null) {
            PlayerListEntry selfEntry = client.player.networkHandler.getPlayerListEntry(client.player.getUuid());
            if (selfEntry != null) {
                LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES = selfEntry.getSkinTextures();
                return;
            }
        }

        // 极端情况下玩家列表还没同步到自己条目时，
        // 当前玩家既然没有伪装覆盖，那直接读取自身皮肤也是原始皮肤，可作为最后兜底。
        LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES = client.player.getSkinTextures();
    }

    /**
     * 把当前准心下的尸体缓存到一个独立字段里，只给秃鹫的能力键使用。
     *
     * <p>验尸官 / 医师 / 死灵法师的 HUD 已经迁到 Wathe 的 {@link RoleNameHudApi}，
     * 不再依赖这份静态缓存；这里保留它，是为了让秃鹫按下能力键时还能拿到目标尸体 UUID，
     * 同时避免再让能力目标取决于某个 HUD mixin 是否先执行。</p>
     */
    private static void noellesroles$refreshLookedAtBody(ClientPlayerEntity player) {
        targetBody = RoleNameHudApi.findLookedAtBody(player, RoleNameHudApi.defaultLookRange(player));
    }

    private static void noellesroles$resetClientCaches() {
        // 二次进服崩溃的根源之一，就是上一局留下来的客户端临时缓存
        // 会在新连接建立早期参与渲染判断，而此时真实玩家列表/外观缓存还没同步完整。
        // 这里在 JOIN / DISCONNECT 两个时机都重置一次，让每次连接都从干净状态开始。
        SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
        LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES = null;
        insanityTime = 0;
        targetBody = null;
        CowardClientEffects.reset();
        GuesserPlayerWidget.selectedPlayer = null;
        SwapperPlayerWidget.playerChoiceOne = null;
        OperatorPlayerWidget.firstChoice = null;
        CorpsemakerState.reset();
        SpiritualistClientController.reset();
        RemembererClientEffects.reset();
        JasonAbilityClientEffects.reset();
        JasonAbilityClientSoundController.reset(MinecraftClient.getInstance());
        ShadowJesterMusicController.reset(MinecraftClient.getInstance());
        DualPersonalityClientState.resetTransientRenderState();
        DualPersonalityKeybinds.resetSyncedState();
        ExecutionerMoodHud.reset();
        wasGazingPressed = false;
        wasChargingPressed = false;
        wasUsingKnife = false;
        grenadeThrowModeToggleHeld = false;
        watchModeToggleHeld = false;
        lastThrowableGrenadeSelectedSlot = -1;
    }


}
