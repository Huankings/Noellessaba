package org.agmas.noellesroles.death;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * noellesroles 死亡判定里的公共“强制放行”规则。
 *
 * <p>这里之所以单独抽一个类，而不是继续塞回主模组类，
 * 是为了让死亡引导器只关心“顺序”和“谁先执行”，
 * 具体哪几种死因需要越过后续保护逻辑，则集中在这里维护。</p>
 *
 * <p>注意这里没有自己去注册 {@code AllowPlayerDeath} 事件。
 * 原因是旧代码里这条规则位于第一段监听器的中间：
 * 它只会跳过 Jester / Bartender / Prophet 这几个后续保护，
 * 但不会阻断后面第二段“反伤 / 自杀”监听器。
 * 如果把它做成一个独立监听器，就无法精确复刻这个“只跳过一部分逻辑”的行为。
 * 因此现在由死亡引导器在原位置主动调用它，才能做到零行为漂移。</p>
 */
public final class CommonForcedDeathHandler {

    private CommonForcedDeathHandler() {
    }

    /**
     * 判断这次死亡是否应该直接放行，不再继续执行第一阶段后半段的保命逻辑。
     *
     * <p>当前保持与旧实现完全一致：</p>
     * <p>1. 定时炸弹爆炸必须成立；</p>
     * <p>2. 落轨死亡必须成立；</p>
     * <p>3. 但这些死亡仍然会继续进入第二段监听器，以保留 Mimic / Executioner 的后续连锁逻辑。</p>
     */
    public static boolean shouldForceAllow(Identifier deathReason) {
        return Noellesroles.DEATH_REASON_BOMB.equals(deathReason)
                || Noellesroles.DEATH_REASON_SEDATIVE_OVERDOSE.equals(deathReason)
                || deathReason == GameConstants.DeathReasons.FELL_OUT_OF_TRAIN;
    }
}
