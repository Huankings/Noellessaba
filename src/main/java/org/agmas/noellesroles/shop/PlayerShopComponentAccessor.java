package org.agmas.noellesroles.shop;

/**
 * 给通用商店结算逻辑使用的最小适配接口。
 *
 * <p>这样可以把“购买成功后扣钱 + 同步”的动作从具体 mixin 中抽出来，
 * 后续其他动态商店职业也能直接复用。</p>
 */
public interface PlayerShopComponentAccessor {

    int noellesroles$getBalance();

    void noellesroles$setBalance(int balance);

    void noellesroles$sync();
}
