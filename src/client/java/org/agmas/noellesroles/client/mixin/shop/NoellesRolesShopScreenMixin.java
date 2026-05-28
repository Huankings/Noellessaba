package org.agmas.noellesroles.client.mixin.shop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.shop.NoellesRolesShopRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 统一替换 NoellesRoles 角色商店在客户端的显示条目。
 *
 * <p>这样界面展示和服务端购买始终读取同一套注册中心数据，
 * 可以避免“界面看见的商品”和“实际买到的商品”不一致。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class NoellesRolesShopScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {

    @Shadow @Final public ClientPlayerEntity player;

    protected NoellesRolesShopScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @ModifyVariable(method = "init", at = @At(value = "STORE"), name = "entries")
    private List<ShopEntry> noellesroles$replaceRoleShopEntries(List<ShopEntry> originalEntries) {
        if (NoellesRolesShopRegistry.hasCustomShop(this.player)) {
            return NoellesRolesShopRegistry.getEntriesForPlayer(this.player);
        }
        return originalEntries;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void noellesroles$appendNonKillerRoleShop(CallbackInfo ci) {
        if (!NoellesRolesShopRegistry.hasCustomShop(this.player)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (gameWorld.canUseKillerFeatures(this.player)) {
            // 杀手类商店已经在 Wathe 原版 init 的 entries 层被替换，
            // 这里不再重复补绘一套按钮。
            return;
        }

        List<ShopEntry> entries = NoellesRolesShopRegistry.getEntriesForPlayer(this.player);
        int apart = 38;
        int x = this.width / 2 - entries.size() * apart / 2 + 9;
        int y = this.y - 46;

        for (int i = 0; i < entries.size(); i++) {
            this.addDrawableChild(new LimitedInventoryScreen.StoreItemWidget(
                    (LimitedInventoryScreen) (Object) this,
                    x + apart * i,
                    y,
                    entries.get(i),
                    i
            ));
        }
    }
}
