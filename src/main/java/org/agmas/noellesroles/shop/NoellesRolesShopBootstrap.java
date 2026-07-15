package org.agmas.noellesroles.shop;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerShopHandler;
import org.agmas.noellesroles.roles.assassin.AssassinShopHandler;
import org.agmas.noellesroles.roles.bartender.BartenderShopHandler;
import org.agmas.noellesroles.roles.bomber.BomberShopHandler;
import org.agmas.noellesroles.roles.controller.ControllerShopHandler;
import org.agmas.noellesroles.roles.coward.CowardShopHandler;
import org.agmas.noellesroles.roles.coroner.CoronerShopHandler;
import org.agmas.noellesroles.roles.engineer.EngineerShopHandler;
import org.agmas.noellesroles.roles.prophet.ProphetShopHandler;
import org.agmas.noellesroles.roles.recaller.RecallerShopHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererShopHandler;
import org.agmas.noellesroles.roles.robber.RobberShopHandler;
import org.agmas.noellesroles.roles.stalker.StalkerShopHandler;
import org.agmas.noellesroles.roles.trapper.TrapperShopHandler;
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
        registerStatic(Noellesroles.BARTENDER, BartenderShopHandler::getShopEntries);
        registerStatic(Noellesroles.ENGINEER, EngineerShopHandler::getShopEntries);
        registerStatic(Noellesroles.RECALLER, RecallerShopHandler::getShopEntries);
        registerStatic(Noellesroles.PROPHET, ProphetShopHandler::getShopEntries);
        registerStatic(Noellesroles.REMEMBERER, RemembererShopHandler::getShopEntries);
        registerStatic(Noellesroles.TRAPPER, TrapperShopHandler::getShopEntries);
        registerStatic(Noellesroles.WINDER, WinderShopHandler::getShopEntries);
        registerStatic(Noellesroles.NOISEMAKER, NoisemakerShopHandler::getShopEntries);
        registerStatic(Noellesroles.CORONER, CoronerShopHandler::getShopEntries);
        registerStatic(Noellesroles.COWARD, CowardShopHandler::getShopEntries);

        // 杀手阵营静态商店。
        registerStatic(Noellesroles.BOMBER, BomberShopHandler::getShopEntries);
        registerStatic(Noellesroles.ASSASSIN, AssassinShopHandler::getShopEntries);
        /*
         * 附体师只修改默认杀手商店的少数条目，因此也走 ShopModifier。
         * 这样它会保留 Wathe 默认左轮、刀、手雷等商品和后续新增机制。
         */
        ShopApi.registerShopModifier(
                Identifier.of(Noellesroles.MOD_ID, "controller_shop"),
                ShopApi.DEFAULT_PRIORITY,
                ControllerShopHandler::modifyShop
        );
        /*
         * 强盗不再注册一整套覆盖商店，而是作为“默认杀手商店修改器”接入。
         * 这样它能保留 Wathe 默认杀手商店后续新增的机制，只对自己的禁售商品、专属插入项、
         * 手雷加价和 FIRECRACKER/BLACKOUT 顺序做局部调整。
         */
        ShopApi.registerShopModifier(
                Identifier.of(Noellesroles.MOD_ID, "robber_shop"),
                ShopApi.DEFAULT_PRIORITY,
                RobberShopHandler::modifyShop
        );

        // 共用一套伪装商店的职业。
        registerStatic(
                () -> Noellesroles.FRAMING_ROLES_SHOP,
                Noellesroles.MIMIC,
                Noellesroles.EXECUTIONER,
                Noellesroles.JESTER
        );

        // 杀手动态商店：按阶段状态实时变化。
        ShopApi.registerRoleShop(Noellesroles.STALKER, provider(StalkerShopHandler::getShopEntries));
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
