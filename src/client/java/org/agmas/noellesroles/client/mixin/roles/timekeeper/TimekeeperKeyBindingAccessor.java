package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface TimekeeperKeyBindingAccessor {
    @Accessor("timesPressed")
    void noellesroles$setTimesPressed(int timesPressed);

    @Accessor("boundKey")
    InputUtil.Key noellesroles$getBoundKey();
}
