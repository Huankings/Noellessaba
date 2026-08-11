package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.regex.Pattern;

/**
 * 弹簧陷阱状态的静音规则。
 *
 * <p>这里集中判断“哪些声音应该被吞掉”，让服务端脚步 mixin、客户端兜底 mixin、门交互 mixin 共用同一套规则。
 * 这样后续如果要允许/禁止某类声音，只需要改这个类，不必在多个 mixin 里分别改判断。</p>
 */
public final class SpringTrapSoundRules {
    private static final Pattern MOVEMENT_SOUND_TOKEN = Pattern.compile(
            "(^|[^a-z0-9])(?:step|walk|run|wander|sprint|footsteps?)[0-9]*($|[^a-z0-9])"
    );

    private SpringTrapSoundRules() {
    }

    public static boolean shouldSuppressSounds(PlayerEntity player) {
        return player != null && SpringTrapPsychoHandler.isSpringTrapActive(player);
    }

    public static boolean shouldSuppressStepSounds(Entity entity) {
        return entity instanceof PlayerEntity player && shouldSuppressSounds(player);
    }

    public static boolean shouldSuppressMovementSound(PlayerEntity player, SoundEvent sound) {
        if (!shouldSuppressSounds(player) || sound == null) {
            return false;
        }
        Identifier soundId = sound.getId();
        return soundId != null && MOVEMENT_SOUND_TOKEN.matcher(soundId.getPath()).find();
    }

    public static Entity.MoveEffect suppressMovementSounds(Entity.MoveEffect original, boolean suppress) {
        if (!suppress) {
            return original;
        }
        /*
         * Entity.MoveEffect 同时控制“移动声音”和“移动 game event”。
         * 弹簧陷阱只需要静音，不应该让压力板/监听事件等移动事件一起消失，
         * 所以 ALL 降级为 EVENTS，SOUNDS 则完全清空。
         */
        if (original == Entity.MoveEffect.ALL) {
            return Entity.MoveEffect.EVENTS;
        }
        if (original == Entity.MoveEffect.SOUNDS) {
            return Entity.MoveEffect.NONE;
        }
        return original;
    }

    public static boolean shouldSuppressClientEntityMovementSound(Entity source, SoundEvent sound, SoundCategory category) {
        if (!(source instanceof PlayerEntity player) || sound == null) {
            return false;
        }
        Identifier soundId = sound.getId();
        return shouldSuppressSounds(player)
                && soundId != null
                && MOVEMENT_SOUND_TOKEN.matcher(soundId.getPath()).find();
    }

    public static boolean shouldSuppressDoorSound(SoundEvent sound) {
        /*
         * 只吞 Wathe 门自己的普通开门/钥匙/锁住提示音。
         * 彩虹斧撬门用 ITEM_CROWBAR_PRY，彩虹斧杀人用 ITEM_BAT_HIT，都不在这里列入，
         * 因此能保留用户指定的“只有撬门和彩虹斧杀人有声音”。
         */
        return sound == WatheSounds.BLOCK_DOOR_TOGGLE
                || sound == WatheSounds.ITEM_KEY_DOOR
                || sound == WatheSounds.ITEM_LOCKPICK_DOOR
                || sound == WatheSounds.BLOCK_DOOR_LOCKED;
    }
}
