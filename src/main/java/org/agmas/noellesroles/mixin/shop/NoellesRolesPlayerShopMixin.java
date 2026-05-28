package org.agmas.noellesroles.mixin.shop;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.shop.NoellesRolesShopRegistry;
import org.agmas.noellesroles.shop.PlayerShopComponentAccessor;
import org.agmas.noellesroles.shop.RoleShopProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 统一接管 NoellesRoles 角色专属商店的服务端购买逻辑。
 *
 * <p>注册中心命中的角色都会走这里，后续不再需要为每个职业单独写 tryBuy mixin。</p>
 */
@Mixin(PlayerShopComponent.class)
public abstract class NoellesRolesPlayerShopMixin implements PlayerShopComponentAccessor {

    @Shadow public int balance;
    @Shadow @Final private PlayerEntity player;
    @Shadow public abstract void sync();

    @Inject(method = "tryBuy", at = @At("HEAD"), cancellable = true)
    private void noellesroles$tryRoleShopBuy(int index, CallbackInfo ci) {
        RoleShopProvider provider = NoellesRolesShopRegistry.getProviderForPlayer(this.player);
        if (provider == null) {
            return;
        }

        List<ShopEntry> entries = provider.getShopEntries(this.player);
        if (index < 0 || index >= entries.size()) {
            ci.cancel();
            return;
        }

        ShopEntry entry = entries.get(index);
        if (provider.handlePurchase(this.player, this.balance, entry)) {
            provider.completePurchase(this, entry.price());
        }

        ci.cancel();
    }

    @Override
    public int noellesroles$getBalance() {
        return this.balance;
    }

    @Override
    public void noellesroles$setBalance(int balance) {
        this.balance = balance;
    }

    @Override
    public void noellesroles$sync() {
        this.sync();
    }
}
