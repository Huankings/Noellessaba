package org.agmas.noellesroles.client.mood;

/**
 * NoellesRoles 接入 Wathe 低心情幻觉手持物 API 的预留注册入口。
 *
 * <p>当前没有职业或词条需要覆盖 Wathe 默认幻觉，因此这里暂时保持空实现。
 * 后续新增规则时，应在对应职业/词条的小类中调用
 * {@code PsychosisItemApi.registerProvider(...)}，再由本类统一聚合，
 * 不要重新编写 PlayerEntityRenderer 或 BipedEntityModel mixin。</p>
 */
public final class NoellesPsychosisHandlers {
    private NoellesPsychosisHandlers() {
    }

    public static void register() {
        // 预留：未来在这里调用各职业或词条自己的 Psychosis provider.init()/register()。
    }
}
