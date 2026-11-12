package org.agmas.noellesroles.client.ui.common;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** 造尸怪与猜测者共用的本地化职业输入、筛选和补全工具。 */
public final class LocalizedRoleInputSupport {
    private LocalizedRoleInputSupport() {
    }

    public static @NotNull List<Candidate> findCandidates(@Nullable String rawInput,
                                                           @NotNull Predicate<Role> roleFilter) {
        String input = rawInput == null ? "" : rawInput.trim();
        String foldedInput = input.toLowerCase(Locale.ROOT);
        boolean preferLocalized = !MinecraftClient.getInstance().getLanguageManager().getLanguage().startsWith("en_");
        List<Candidate> result = new ArrayList<>();

        for (Role role : WatheRoles.ROLES) {
            if (!roleFilter.test(role)
                    || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString())) {
                continue;
            }

            String roleId = role.identifier().toString();
            String rolePath = role.identifier().getPath();
            String localizedName = normalizeLocalizedName(Harpymodloader.getRoleName(role).getString());
            boolean idMatch = roleId.toLowerCase(Locale.ROOT).startsWith(foldedInput);
            boolean pathMatch = rolePath.toLowerCase(Locale.ROOT).startsWith(foldedInput);
            boolean localizedMatch = localizedName.toLowerCase(Locale.ROOT).startsWith(foldedInput);
            if (!input.isEmpty() && !idMatch && !pathMatch && !localizedMatch) {
                continue;
            }

            String completionText;
            if (input.isEmpty()) {
                completionText = preferLocalized ? localizedName : rolePath;
            } else if (localizedMatch && !pathMatch && !idMatch) {
                completionText = localizedName;
            } else if (idMatch && !pathMatch) {
                completionText = roleId;
            } else {
                /* 输入的是英文 path 前缀时，即使客户端语言是中文，也必须继续补全英文 path。 */
                completionText = rolePath;
            }
            result.add(new Candidate(role, roleId, rolePath, localizedName, completionText));
        }

        result.sort(Comparator.comparing(Candidate::completionText, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Candidate::roleId));
        return result;
    }

    public static @Nullable Role resolveExact(@Nullable String rawInput, @NotNull Predicate<Role> roleFilter) {
        if (rawInput == null || rawInput.isBlank()) {
            return null;
        }
        String input = rawInput.trim();
        List<Candidate> localizedMatches = new ArrayList<>();
        for (Candidate candidate : findCandidates(input, roleFilter)) {
            if (candidate.roleId().equalsIgnoreCase(input) || candidate.rolePath().equalsIgnoreCase(input)) {
                return candidate.role();
            }
            if (candidate.localizedName().equalsIgnoreCase(input)) {
                localizedMatches.add(candidate);
            }
        }

        /*
         * 两个扩展若提供完全相同的本地化职业名，手工输入无法可靠判断用户选择了谁。
         * 此时不静默选择；用户可继续用 Tab 补全到带有稳定候选状态的具体职业。
         */
        return localizedMatches.size() == 1 ? localizedMatches.getFirst().role() : null;
    }

    public static @NotNull OrderedText toSuggestionMessage(@NotNull Candidate candidate) {
        return Text.literal(candidate.completionText())
                .append(Text.literal(" (" + candidate.roleId() + ")"))
                .withColor(candidate.role().color())
                .asOrderedText();
    }

    private static String normalizeLocalizedName(String name) {
        String normalized = name == null ? "" : name.trim();
        while (normalized.endsWith("!") || normalized.endsWith("！")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    public record Candidate(@NotNull Role role,
                            @NotNull String roleId,
                            @NotNull String rolePath,
                            @NotNull String localizedName,
                            @NotNull String completionText) {
    }
}
