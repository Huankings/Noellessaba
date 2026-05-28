package org.agmas.noellesroles.client.ui.roles.corpsemaker;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.noellesroles.packet.role.corpsemaker.CorpsemakerC2SPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CorpsemakerRoleInputWidget extends TextFieldWidget {
    private final LimitedInventoryScreen screen;
    private final ChatInputSuggestor suggestor;
    private final List<String> matchedRoles = new ArrayList<>();
    private int currentSuggestionIndex = 0;

    public CorpsemakerRoleInputWidget(LimitedInventoryScreen screen, TextRenderer textRenderer, int x, int y) {
        super(textRenderer, x, y, 200, 16, Text.literal(""));
        this.screen = screen;
        this.suggestor = new ChatInputSuggestor(
                MinecraftClient.getInstance(),
                screen,
                this,
                textRenderer,
                true,
                true,
                -1,
                10,
                false,
                0x00000000
        );
        setMaxLength(64);
        setEditableColor(0xFFFFFF);
        setUneditableColor(0x707070);
        setDrawsBackground(true);
        setFocused(true);
        refreshMatchedRoles();
        suggestor.refresh();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean result = super.charTyped(chr, modifiers);
        refreshMatchedRoles();
        suggestor.refresh();
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理 Tab 键
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            refreshMatchedRoles(); // 确保列表最新
            if (!matchedRoles.isEmpty()) {
                setText(matchedRoles.get(currentSuggestionIndex));
                setCursorToEnd(false);
                refreshMatchedRoles(); // 重新计算匹配列表
                normalizeSuggestionIndex(); // 确保索引有效
                updateSuggestionSuffix();
                suggestor.refresh();
            }
            return true;
        }
        // 处理上箭头
        if (keyCode == GLFW.GLFW_KEY_UP) {
            refreshMatchedRoles();
            if (!matchedRoles.isEmpty()) {
                currentSuggestionIndex--;
                normalizeSuggestionIndex();
                updateSuggestionSuffix();
            }
            return true;
        }
        // 处理下箭头
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            refreshMatchedRoles();
            if (!matchedRoles.isEmpty()) {
                currentSuggestionIndex++;
                normalizeSuggestionIndex();
                updateSuggestionSuffix();
            }
            return true;
        }
        // 处理回车
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!getText().isBlank() && CorpsemakerState.selectedPlayerUuid != null && CorpsemakerState.selectedDeathReason != null) {
                ClientPlayNetworking.send(new CorpsemakerC2SPacket(
                        CorpsemakerState.selectedPlayerUuid,
                        CorpsemakerState.selectedDeathReason,
                        getText().trim()
                ));
                screen.close();
            }
            return true;
        }
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        refreshMatchedRoles();
        suggestor.refresh();
        return result;
    }

    @Override
    public void eraseCharacters(int characterOffset) {
        super.eraseCharacters(characterOffset);
        refreshMatchedRoles();
        suggestor.refresh();
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshMatchedRoles();
        normalizeSuggestionIndex();
        updateSuggestionSuffix();
        super.renderWidget(context, mouseX, mouseY, delta);
        suggestor.render(context, mouseX, mouseY);
    }

    private void refreshMatchedRoles() {
        matchedRoles.clear();
        String input = getText().toLowerCase();
        for (var role : WatheRoles.ROLES) {
            if (HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString())) {
                continue;
            }
            String rolePath = role.identifier().getPath();
            if (input.isEmpty() || rolePath.startsWith(input)) {
                matchedRoles.add(rolePath);
            }
        }
        matchedRoles.sort(String::compareToIgnoreCase);
    }

    private void normalizeSuggestionIndex() {
        if (matchedRoles.isEmpty()) {
            currentSuggestionIndex = 0;
        } else {
            if (currentSuggestionIndex < 0) {
                currentSuggestionIndex = matchedRoles.size() - 1;
            } else if (currentSuggestionIndex >= matchedRoles.size()) {
                currentSuggestionIndex = 0;
            }
        }
    }

    private void updateSuggestionSuffix() {
        if (matchedRoles.isEmpty()) {
            setSuggestion("");
            return;
        }
        String input = getText();
        String match = matchedRoles.get(currentSuggestionIndex);
        if (input.length() < match.length()) {
            setSuggestion(match.substring(input.length()));
        } else {
            setSuggestion("");
        }
    }
}