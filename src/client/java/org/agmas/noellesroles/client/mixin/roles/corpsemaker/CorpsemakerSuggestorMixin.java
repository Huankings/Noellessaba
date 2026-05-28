package org.agmas.noellesroles.client.mixin.roles.corpsemaker;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerRoleInputWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatInputSuggestor.class)
public abstract class CorpsemakerSuggestorMixin {

    @Shadow @Final private TextFieldWidget textField;

    @Shadow public abstract void show(boolean narrateFirstSuggestion);

    @Shadow @Nullable private ChatInputSuggestor.SuggestionWindow window;

    @Shadow @Final private List<OrderedText> messages;

    @Shadow private int x;

    @Shadow private int width;

    @Shadow private boolean windowActive;

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    void onRefresh(@NotNull CallbackInfo ci) {
        if (textField instanceof CorpsemakerRoleInputWidget) {
            messages.clear();
            WatheRoles.ROLES.forEach((role) -> {
                if (HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString())) return;
                String rolePath = role.identifier().getPath();
                if (rolePath.startsWith(textField.getText()) || textField.getText().isEmpty()) {
                    MutableText s = Text.literal(rolePath);
                    // 如果不是英语，添加中文名
                    if (!MinecraftClient.getInstance().getLanguageManager().getLanguage().startsWith("en_")) {
                        s.append(Text.literal(" (").append(Harpymodloader.getRoleName(role)).append(")"));
                    }
                    messages.add(s.withColor(role.color()).asOrderedText());
                }
            });
            x = textField.getX();
            width = textField.getWidth();
            window = null;
            if (windowActive && client.options.getAutoSuggestions().getValue()) {
                show(false);
            }
            ci.cancel();
        }
    }

    @Inject(method = "show", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatInputSuggestor;sortSuggestions(Lcom/mojang/brigadier/suggestion/Suggestions;)Ljava/util/List;", shift = At.Shift.BEFORE))
    void onShow(boolean narrateFirstSuggestion, CallbackInfo ci, @Local(ordinal = 2) LocalIntRef intRef) {
        if (textField instanceof CorpsemakerRoleInputWidget) {
            intRef.set(textField.getY() + textField.getHeight());
        }
    }

    @Inject(method = "renderMessages", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", shift = At.Shift.BEFORE))
    void onRenderMessages(DrawContext context, CallbackInfo ci, @Local(ordinal = 1) LocalIntRef intRef, @Local(ordinal = 0) int i) {
        if (textField instanceof CorpsemakerRoleInputWidget) {
            intRef.set(textField.getY() + textField.getHeight() + 12 * i);
        }
    }
}