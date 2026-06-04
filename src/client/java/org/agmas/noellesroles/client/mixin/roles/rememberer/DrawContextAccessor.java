package org.agmas.noellesroles.client.mixin.roles.rememberer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.GuiAtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取出 DrawContext 里私有的 guiAtlasManager。
 *
 * <p>追忆者心情箭头想完全复刻 Wathe 原版的淡入淡出效果，
 * 就必须回到 drawSprite(..., sprite, alpha) 这条绘制路径。
 * 但 Sprite 本身是通过 DrawContext 持有的 guiAtlasManager 解析出来的，
 * 所以这里补一个极小的 accessor，专门给 RemembererMoodRenderer 复用。</p>
 */
@Mixin(DrawContext.class)
public interface DrawContextAccessor {

    @Accessor("guiAtlasManager")
    GuiAtlasManager noellesroles$getGuiAtlasManager();
}
