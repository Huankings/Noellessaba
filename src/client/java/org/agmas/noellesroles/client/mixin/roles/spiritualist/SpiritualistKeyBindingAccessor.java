package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取原版 KeyBinding 当前真正绑定的按键。
 *
 * <p>灵术师灵魂出窍时，需要绕过 wathe 对 jumpKey 的局内禁用，
 * 但仍然要尊重玩家自己的改键设置，所以这里直接取当前绑定键而不是写死空格。</p>
 */
@Mixin(KeyBinding.class)
public interface SpiritualistKeyBindingAccessor {

    @Accessor("boundKey")
    InputUtil.Key noellesroles$getBoundKey();
}
