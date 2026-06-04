package org.agmas.noellesroles.client;

import com.google.common.collect.Maps;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.GrenadeThrowModePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.renderer.CaptureDeviceEntityRenderer;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.client.renderer.RoleMineEntityRenderer;
import org.agmas.noellesroles.client.renderer.ThrowingAxeEntityRenderer;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.modifiers.guesser.GuesserPlayerWidget;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerState;
import org.agmas.noellesroles.client.ui.roles.operator.OperatorPlayerWidget;
import org.agmas.noellesroles.client.ui.roles.swapper.SwapperPlayerWidget;
import org.agmas.noellesroles.packet.host.AbilityC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerDashC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerGazeC2SPacket;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionViewS2CPacket;
import org.agmas.noellesroles.packet.role.vulture.VultureEatC2SPacket;
import org.agmas.noellesroles.roles.angel.AngelAbility;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistTargeting;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.lwjgl.glfw.GLFW;

import org.agmas.noellesroles.client.items.NoellesRolesItemToolTip;
import org.agmas.noellesroles.client.items.NoellesRolesItemExtraModel;

import java.util.*;
import dev.doctor4t.wathe.api.event.GameEvents;

public class NoellesrolesClient implements ClientModInitializer {


    public static int insanityTime = 0;
    public static KeyBinding abilityBind;
    public static PlayerEntity target;
    public static PlayerBodyEntity targetBody;
    // 在 NoellesrolesClient 类中添加变量
    private static boolean wasGazingPressed = false;
    private static boolean wasChargingPressed = false;
    private static boolean wasUsingKnife = false;
    private static boolean grenadeThrowModeToggleHeld = false;
    private static int lastThrowableGrenadeSelectedSlot = -1;

    public static Map<UUID, UUID> SHUFFLED_PLAYER_ENTRIES_CACHE = Maps.newHashMap();
    public static SkinTextures LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES = null;


    @Override
    public void onInitializeClient() {
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Noellesroles.MOD_ID + ".ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.wathe.keybinds"));
        registerItemTooltipsAndModels();

        // 分页缓存只在当前对局内生效。
        // 开局、停局、结算完成都清空一次，避免上一把的页码残留到下一把。
        GameEvents.ON_GAME_START.register(gameMode -> PagedPlayerScreenState.reset());
        GameEvents.ON_GAME_STOP.register(gameMode -> PagedPlayerScreenState.reset());
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> PagedPlayerScreenState.reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> noellesroles$resetClientCaches());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> noellesroles$resetClientCaches());
        SpiritualistClientController.init();
        ClientPlayNetworking.registerGlobalReceiver(SpiritualistPossessionViewS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SpiritualistClientController.handlePossessionViewPacket(payload)));

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> noellesroles$handleGrenadeThrowModeSwitch(player));



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RemembererClientEffects.tick(client);
            CowardClientEffects.tick(client);
            // 在 ClientTickEvents.END_CLIENT_TICK.register(client -> { ... } 中：
            if (abilityBind.isPressed()) {
                if (!wasGazingPressed) {
                    wasGazingPressed = true;
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(mc.player.getWorld());
                        if (gameWorld.isRole(mc.player, Noellesroles.STALKER)) {
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
                        if (gameWorld.isRole(mc.player, Noellesroles.STALKER)) {
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
                if (gameWorld.isRole(mc.player, Noellesroles.STALKER)) {
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
            }

            if (!client.options.attackKey.isPressed()) {
                grenadeThrowModeToggleHeld = false;
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
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.VULTURE)) {
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
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.ANGEL)) {
                        PlayerEntity angelTarget = AngelAbility.getGenericGuardTarget(MinecraftClient.getInstance().player);
                        if (angelTarget != null) {
                            targetId = angelTarget.getId();
                        }
                    } else if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.SPIRITUALIST)) {
                        PlayerEntity spiritualistTarget = SpiritualistTargeting.getPossessionTarget(MinecraftClient.getInstance().player);
                        if (spiritualistTarget != null) {
                            targetId = spiritualistTarget.getId();
                        }
                    }
                    ClientPlayNetworking.send(new AbilityC2SPacket(targetId));
                });
            }
        });
        EntityRendererRegistry.register(NoellesRolesEntities.ROLE_MINE_ENTITY_ENTITY_TYPE, RoleMineEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.CAPTURE_DEVICE_ENTITY_TYPE, CaptureDeviceEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.THROWING_AXE_ENTITY_TYPE, ThrowingAxeEntityRenderer::new);
    }

    private void registerItemTooltipsAndModels() {
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
            NoellesRolesItemToolTip.addItemtip(ModItems.CRYSTAL_BALL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.ROBBER_PISTOL, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BAYONET, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SILENCED_REVOLVER, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SILENT_GRENADE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SNIPER_RIFLE, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.SNIPER_RIFLE_BULLET, itemStack, list);
            NoellesRolesItemToolTip.addItemtip(ModItems.BAYONET_COLDOWN_REFRESH, itemStack, list);
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
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.CRYSTAL_BALL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.ROBBER_PISTOL);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.BAYONET);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SILENCED_REVOLVER);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SILENT_GRENADE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SNIPER_RIFLE);
        NoellesRolesItemExtraModel.registerExtraModel(ModItems.SNIPER_RIFLE_BULLET);
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
        if (isHoldingThrowableGrenade && lastThrowableGrenadeSelectedSlot != currentSlot) {
            WatheClient.showGrenadeThrowModeMessage(player, "tip.grenade.current_throw_mode");
            lastThrowableGrenadeSelectedSlot = currentSlot;
        } else if (!isHoldingThrowableGrenade) {
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

    private static void noellesroles$resetClientCaches() {
        // 二次进服崩溃的根源之一，就是上一局留下来的客户端临时缓存
        // 会在新连接建立早期参与渲染判断，而此时真实玩家列表/外观缓存还没同步完整。
        // 这里在 JOIN / DISCONNECT 两个时机都重置一次，让每次连接都从干净状态开始。
        PagedPlayerScreenState.reset();
        SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
        LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES = null;
        insanityTime = 0;
        target = null;
        targetBody = null;
        CowardClientEffects.reset();
        GuesserPlayerWidget.selectedPlayer = null;
        SwapperPlayerWidget.playerChoiceOne = null;
        OperatorPlayerWidget.firstChoice = null;
        CorpsemakerState.reset();
        SpiritualistClientController.reset();
        RemembererClientEffects.reset();
        wasGazingPressed = false;
        wasChargingPressed = false;
        wasUsingKnife = false;
        grenadeThrowModeToggleHeld = false;
        lastThrowableGrenadeSelectedSlot = -1;
    }


}
