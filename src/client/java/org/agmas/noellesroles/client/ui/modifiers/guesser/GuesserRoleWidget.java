package org.agmas.noellesroles.client.ui.modifiers.guesser;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import org.agmas.noellesroles.client.ui.common.LocalizedRoleInputSupport;
import org.agmas.noellesroles.packet.modifiers.GuessC2SPacket;

import java.util.List;
import java.util.UUID;

public class GuesserRoleWidget extends TextFieldWidget {
    public final LimitedInventoryScreen screen;
    public final ChatInputSuggestor suggestor;
    public static boolean stopClosing = false;

    // ✅ 核心修改：将「匹配列表+选中索引」整合为单个对象，统一管理联想补全状态
    private final SuggestionState suggestionState = new SuggestionState();

    public GuesserRoleWidget(LimitedInventoryScreen screen, TextRenderer textRenderer, int x, int y) {
        super(textRenderer, x, y, 200, 16, Text.literal(""));
        this.screen = screen;
        // 原修复：ChatInputSuggestor 构造参数错误 + 背景色改为完全透明
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
                0x00000000 // 完全透明背景，解决原半透明底色遮挡UI问题
        );
        refreshSuggestions();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean original = super.charTyped(chr, modifiers);
        suggestionState.selectedRole = null;
        refreshSuggestions(); // 抽离公共方法，统一刷新逻辑
        return original;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Tab键 循环补全提示文本 (键盘码258是Tab的标准键值)
        if (keyCode == 258 && !suggestionState.matchedRoles.isEmpty()) {
            LocalizedRoleInputSupport.Candidate candidate = suggestionState.matchedRoles.get(suggestionState.currentIndex);
            suggestionState.selectedRole = candidate.role();
            this.setText(candidate.completionText());
            suggestionState.currentIndex = (suggestionState.currentIndex + 1) % suggestionState.matchedRoles.size();
            this.setSuggestion("");
            return true; // 消费事件，防止穿透触发其他逻辑
        }
        if (keyCode == 265 && !suggestionState.matchedRoles.isEmpty()) {
            suggestionState.currentIndex--;
            if (suggestionState.currentIndex < 0) {
                suggestionState.currentIndex = suggestionState.matchedRoles.size() - 1;
            }
            this.setSuggestion(getSuggestionSuffix());
            return true;
        }
        if (keyCode == 264 && !suggestionState.matchedRoles.isEmpty()) {
            suggestionState.currentIndex++;
            if (suggestionState.currentIndex >= suggestionState.matchedRoles.size()) {
                suggestionState.currentIndex = 0;
            }
            this.setSuggestion(getSuggestionSuffix());
            return true;
        }
        // 回车/小键盘回车 提交猜职业请求 (257=主键盘回车，335=小键盘回车)
        if (keyCode == 257 || keyCode == 335) {
            UUID selectedPlayer = GuesserPlayerWidget.selectedPlayer;
            // 原修复：判空+非空校验，防止发送空请求/空玩家ID导致服务端报错
            if (selectedPlayer != null && !this.getText().isBlank()) {
                Role resolvedRole = resolveSubmittedRole();
                ClientPlayNetworking.send(new GuessC2SPacket(
                        selectedPlayer,
                        resolvedRole == null ? getText().trim() : resolvedRole.identifier().toString()
                ));
                screen.close();
                // 重置状态，防止残留导致下次打开异常
                this.setText("");
                suggestionState.reset();
            }
            return true; // 消费事件，防止执行父类逻辑
        }

        // 退格键/删除键 触发联想刷新
        boolean parentResult = super.keyPressed(keyCode, scanCode, modifiers);
        suggestionState.selectedRole = null;
        // 输入法提交、粘贴、退格和删除都可能通过父类修改文本，统一刷新而不是只监听两个键码。
        refreshSuggestions();
        return parentResult;
    }

    @Override
    public void eraseCharacters(int characterOffset) {
        super.eraseCharacters(characterOffset);
        refreshSuggestions(); // 抽离公共方法，统一逻辑
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // 原修复：选中状态赋值放到最前，防止关闭逻辑抢先执行
        stopClosing = isFocused();
        MinecraftClient client = MinecraftClient.getInstance();
        UUID selectedPlayer = GuesserPlayerWidget.selectedPlayer;

        // 双重判空：有选中玩家 + 客户端玩家非空，杜绝空指针
        if (selectedPlayer != null && client.player != null) {
            fillMatchedRoles(client); // 抽离职业匹配逻辑，解耦渲染方法

            // ✅ 优化索引越界兜底 - 更健壮的写法，全覆盖所有边界情况
            if (!suggestionState.matchedRoles.isEmpty()) {
                suggestionState.currentIndex = Math.max(0, Math.min(suggestionState.currentIndex, suggestionState.matchedRoles.size() - 1));
                String inputText = this.getText();
                String matchText = suggestionState.matchedRoles.get(suggestionState.currentIndex).completionText();
                // ✅ 关键修改：删除长度判断，输入为空/输入前缀 都实时显示完整后缀联想，无任何条件限制
                this.setSuggestion(matchText.substring(inputText.length()));
            } else {
                this.setSuggestion("");
            }

            // 执行父类渲染逻辑：绘制【半透明输入框】本体
            super.renderWidget(context, mouseX, mouseY, delta);
            // 渲染联想提示框
            suggestor.render(context, mouseX, mouseY);
        }
    }

    // ======== 核心优化：抽离【公共刷新方法】 ========
    private void refreshSuggestions() {
        suggestionState.currentIndex = 0;
        suggestionState.selectedRole = null;
        fillMatchedRoles(MinecraftClient.getInstance());
        this.suggestor.refresh();
    }

    // ======== 核心优化：抽离【职业匹配逻辑】 ========
    private void fillMatchedRoles(MinecraftClient client) {
        suggestionState.matchedRoles = LocalizedRoleInputSupport.findCandidates(this.getText(), this::isRoleAllowedForViewer);
    }

    // ✅ 新增：内部静态类 - 统一封装「匹配列表+选中索引」，状态整合管理
    private static class SuggestionState {
        // 存储匹配的职业名称列表
        public List<LocalizedRoleInputSupport.Candidate> matchedRoles = List.of();
        // 当前选中的补全索引
        public int currentIndex = 0;
        public Role selectedRole;

        // 提供统一的重置方法，一键清空状态，避免分散重置代码
        public void reset() {
            this.currentIndex = 0;
            this.matchedRoles = List.of();
            this.selectedRole = null;
        }
    }
    private String getSuggestionSuffix() {
        if (suggestionState.matchedRoles.isEmpty()) return "";
        String inputText = this.getText();
        String matchText = suggestionState.matchedRoles.get(suggestionState.currentIndex).completionText();
        return inputText.length() < matchText.length() ? matchText.substring(inputText.length()) : "";
    }

    public List<OrderedText> getSuggestionMessages() {
        return suggestionState.matchedRoles.stream().map(LocalizedRoleInputSupport::toSuggestionMessage).toList();
    }

    private boolean isRoleAllowedForViewer(Role role) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        return gameWorld.isInnocent(client.player) || !role.canUseKiller();
    }

    private Role resolveSubmittedRole() {
        if (suggestionState.selectedRole != null) {
            return suggestionState.selectedRole;
        }
        return LocalizedRoleInputSupport.resolveExact(getText(), this::isRoleAllowedForViewer);
    }

}
