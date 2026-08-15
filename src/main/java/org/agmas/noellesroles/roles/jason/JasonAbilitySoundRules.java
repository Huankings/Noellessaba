package org.agmas.noellesroles.roles.jason;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * 杰森无恶不在期间的静音规则。
 *
 * <p>脚步静音已经复用了弹簧陷阱的移动静音规则；这里专门补原版水相关音效。
 * 原版入水 splash 会在 touchingWater 标记切换前播放，所以不能只靠“当前是否在水里”判断，
 * 必须从声音 id 本身识别 swim / splash / bubble / underwater 这类水声。</p>
 */
public final class JasonAbilitySoundRules {
    private static final Pattern WATER_SOUND_TOKEN = Pattern.compile(
            "(^|[^a-z0-9])(?:swim|splash|bubble|underwater|water)[0-9]*($|[^a-z0-9])"
    );
    private static final Pattern DAMAGE_SOUND_TOKEN = Pattern.compile(
            "(^|[^a-z0-9])(?:hurt|damage|burn|on_fire|in_fire|fire)[0-9]*($|[^a-z0-9])"
    );
    private static final Pattern LANDING_SOUND_TOKEN = Pattern.compile(
            "(^|[^a-z0-9])(?:fall|land|landing)[0-9]*($|[^a-z0-9])"
    );

    private JasonAbilitySoundRules() {
    }

    public static boolean shouldSuppressWaterSound(@Nullable PlayerEntity player, @Nullable SoundEvent sound) {
        return shouldSuppressByPattern(player, sound, WATER_SOUND_TOKEN);
    }

    public static boolean shouldSuppressAbilityEntitySound(@Nullable PlayerEntity player, @Nullable SoundEvent sound) {
        /*
         * Entity#playSound 是原版多数实体音效的服务端广播入口。
         * 无恶不在期间杰森已经不该产生水声；如果某条伤害路径绕过了 damage 拦截，
         * 这里再兜底吞掉扣血 / 灼烧相关声音，避免其它玩家靠声音定位幽魂杰森。
         */
        return shouldSuppressWaterSound(player, sound)
                || shouldSuppressDamageSound(player, sound)
                || shouldSuppressLandingSound(player, sound);
    }

    public static boolean shouldSuppressClientEntitySound(@Nullable Entity source, @Nullable SoundEvent sound) {
        /*
         * 客户端收到实体声音包时再做一层同样过滤。
         * 这不是主要防线，而是防止其它模组或客户端重放路径绕过服务端 Entity#playSound 取消。
         */
        return source instanceof PlayerEntity player && shouldSuppressAbilityEntitySound(player, sound);
    }

    private static boolean shouldSuppressDamageSound(@Nullable PlayerEntity player, @Nullable SoundEvent sound) {
        return shouldSuppressByPattern(player, sound, DAMAGE_SOUND_TOKEN);
    }

    private static boolean shouldSuppressLandingSound(@Nullable PlayerEntity player, @Nullable SoundEvent sound) {
        /*
         * 高处落地会播放 entity.player.big_fall / small_fall，部分方块还会播放 block.xxx.fall。
         * 这些声音不是脚步声，不能依赖 SpringTrap 的 step/walk/run 过滤，因此单独识别 fall/land。
         */
        return shouldSuppressByPattern(player, sound, LANDING_SOUND_TOKEN);
    }

    private static boolean shouldSuppressByPattern(
            @Nullable PlayerEntity player,
            @Nullable SoundEvent sound,
            @Nullable Pattern pattern
    ) {
        if (!JasonAbilityRules.isAbilityActiveLike(player) || sound == null || pattern == null) {
            return false;
        }

        Identifier soundId = sound.getId();
        return soundId != null && pattern.matcher(soundId.getPath()).find();
    }
}
