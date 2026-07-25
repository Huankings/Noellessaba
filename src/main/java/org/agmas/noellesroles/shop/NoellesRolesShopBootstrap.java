package org.agmas.noellesroles.shop;

import org.agmas.noellesroles.registry.NoellesFramingShopEntries;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerShopHandler;
import org.agmas.noellesroles.roles.assassin.AssassinShopHandler;
import org.agmas.noellesroles.roles.bartender.BartenderShopHandler;
import org.agmas.noellesroles.roles.bomber.BomberShopHandler;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterShopHandler;
import org.agmas.noellesroles.roles.controller.ControllerShopHandler;
import org.agmas.noellesroles.roles.cook.CookShopHandler;
import org.agmas.noellesroles.roles.coward.CowardShopHandler;
import org.agmas.noellesroles.roles.coroner.CoronerShopHandler;
import org.agmas.noellesroles.roles.dreamer.DreamerShopHandler;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerShopHandler;
import org.agmas.noellesroles.roles.engineer.EngineerShopHandler;
import org.agmas.noellesroles.roles.hacker.HackerShopHandler;
import org.agmas.noellesroles.roles.hunter.HunterShopHandler;
import org.agmas.noellesroles.roles.initiate.InitiateShopHandler;
import org.agmas.noellesroles.roles.kidnapper.KidnapperShopHandler;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainShopHandler;
import org.agmas.noellesroles.roles.muzzler.MuzzlerShopHandler;
import org.agmas.noellesroles.roles.necromancer.NecromancerShopHandler;
import org.agmas.noellesroles.roles.physician.PhysicianShopHandler;
import org.agmas.noellesroles.roles.prophet.ProphetShopHandler;
import org.agmas.noellesroles.roles.recaller.RecallerShopHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererShopHandler;
import org.agmas.noellesroles.roles.robber.RobberShopHandler;
import org.agmas.noellesroles.roles.stalker.StalkerShopHandler;
import org.agmas.noellesroles.roles.trapper.TrapperShopHandler;
import org.agmas.noellesroles.roles.waiter.WaiterShopHandler;
import org.agmas.noellesroles.roles.winder.WinderShopHandler;

import java.util.List;
import java.util.function.Supplier;

/**
 * 统一注册 NoellesRoles 角色商店。
 *
 * <p>后续如果新增一个有专属商店的职业，优先在这里注册：</p>
 * <p>1. 固定商店用 registerStatic</p>
 * <p>2. 动态商店用 register</p>
 */
public final class NoellesRolesShopBootstrap {

    private NoellesRolesShopBootstrap() {
    }

    public static void init() {
        // 好人阵营静态商店。
        registerStatic(NoellesRoleRegistry.BARTENDER, BartenderShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.ENGINEER, EngineerShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.RECALLER, RecallerShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.PROPHET, ProphetShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.REMEMBERER, RemembererShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.TRAPPER, TrapperShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.WINDER, WinderShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.NOISEMAKER, NoisemakerShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.CORONER, CoronerShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.COWARD, CowardShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.COOK, CookShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.PHYSICIAN, PhysicianShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.WAITER, WaiterShopHandler::getShopEntries);

        // 杀手阵营静态商店。
        registerStatic(NoellesRoleRegistry.BOMBER, BomberShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.ASSASSIN, AssassinShopHandler::getShopEntries);
        /*
         * 附体师只修改默认杀手商店的少数条目，因此也走 ShopModifier。
         * 这样它会保留 Wathe 默认左轮、刀、手雷等商品和后续新增机制。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "controller_shop"),
                ShopApi.DEFAULT_PRIORITY,
                ControllerShopHandler::modifyShop
        );
        /*
         * 强盗不再注册一整套覆盖商店，而是作为“默认杀手商店修改器”接入。
         * 这样它能保留 Wathe 默认杀手商店后续新增的机制，只对自己的禁售商品、专属插入项、
         * 手雷加价和 FIRECRACKER/BLACKOUT 顺序做局部调整。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "robber_shop"),
                ShopApi.DEFAULT_PRIORITY,
                RobberShopHandler::modifyShop
        );
        /*
         * 赏金猎人也是默认杀手商店的局部改写：
         * 禁售部分近战/毒物，左轮替换为赏金手枪，疯魔模式替换为赏金模式。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "bounty_hunter_shop"),
                ShopApi.DEFAULT_PRIORITY,
                BountyHunterShopHandler::modifyShop
        );
        /*
         * 静语者只替换默认杀手商店的左轮格子为胶带。
         * 这里也走 ShopModifier，保证其它默认杀手商品和 Wathe 的购买副作用全部保留。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "muzzler_shop"),
                ShopApi.DEFAULT_PRIORITY,
                MuzzlerShopHandler::modifyShop
        );
        /*
         * 追猎者和 kinssaba 一样只对默认杀手商店做局部改动：
         * 删除毒物、插入猎刀、提高普通匕首价格。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "hunter_shop"),
                ShopApi.DEFAULT_PRIORITY,
                HunterShopHandler::modifyShop
        );
        /*
         * 制毒师/绑匪来自 kinssaba，二者都是“默认杀手商店局部改写”：
         * 只插入/移除少数商品，不接管 Wathe 购买结算。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "drugmaker_shop"),
                ShopApi.DEFAULT_PRIORITY,
                DrugmakerShopHandler::modifyShop
        );
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "kidnapper_shop"),
                ShopApi.DEFAULT_PRIORITY,
                KidnapperShopHandler::modifyShop
        );
        /*
         * 死灵法师沿用 StupidExpress 默认配置：有杀手能力，但没有杀手商店。
         * 这里清空 ShopApi 已经解析出的默认杀手商品，避免只隐藏客户端界面却仍可服务端购买。
         */
        ShopApi.registerShopModifier(
                Identifier.of(NoellesRolesCore.MOD_ID, "necromancer_no_shop"),
                ShopApi.DEFAULT_PRIORITY,
                NecromancerShopHandler::modifyShop
        );

        // 共用一套伪装商店的职业。
        registerStatic(
                () -> NoellesFramingShopEntries.FRAMING_ROLES_SHOP,
                NoellesRoleRegistry.MIMIC,
                NoellesRoleRegistry.EXECUTIONER,
                NoellesRoleRegistry.JESTER,
                NoellesRoleRegistry.DREAMER
        );
        registerStatic(NoellesRoleRegistry.HACKER, HackerShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.INITIATE, InitiateShopHandler::getShopEntries);
        registerStatic(NoellesRoleRegistry.LICENSED_VILLAIN, LicensedVillainShopHandler::getShopEntries);

        // 杀手动态商店：按阶段状态实时变化。
        ShopApi.registerRoleShop(NoellesRoleRegistry.STALKER, provider(StalkerShopHandler::getShopEntries));
        // 好人动态商店：按阶段状态实时变化。
        
    }

    private static void registerStatic(Role role, Supplier<List<ShopEntry>> supplier) {
        ShopApi.registerRoleShop(role, staticProvider(supplier));
    }

    private static void registerStatic(Supplier<List<ShopEntry>> supplier, Role... roles) {
        for (Role role : roles) {
            registerStatic(role, supplier);
        }
    }

    private static dev.doctor4t.wathe.api.shop.RoleShopProvider staticProvider(Supplier<List<ShopEntry>> supplier) {
        return provider(player -> supplier.get());
    }

    private static dev.doctor4t.wathe.api.shop.RoleShopProvider provider(dev.doctor4t.wathe.api.shop.RoleShopProvider entriesProvider) {
        return new dev.doctor4t.wathe.api.shop.RoleShopProvider() {
            @Override
            public List<ShopEntry> getShopEntries(net.minecraft.entity.player.PlayerEntity player) {
                return entriesProvider.getShopEntries(player);
            }

            @Override
            public dev.doctor4t.wathe.api.shop.ShopPurchaseResult purchase(dev.doctor4t.wathe.api.shop.ShopPurchaseContext context) {
                /*
                 * NoellesRoles 仍然保留每职业 handler 负责“卖什么”，
                 * 但购买时统一回到 Wathe 的 ShopApi 流程；这里仅交付特殊商品或普通物品。
                 */
                return NoellesRolesShops.purchase(context);
            }
        };
    }
}
