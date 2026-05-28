package org.agmas.noellesroles.client.ui.common;

/**
 * 玩家头像分页布局工具。
 *
 * 这里把“每页最多显示多少人”“头像之间的固定间距”“翻页按钮放在哪里”
 * 这些和具体职业无关的规则统一收口到一个地方，后续其它职业要接分页时
 * 只要复用这里的常量和坐标计算即可，避免每个职业都各写一套而出现偏差。
 */
public final class PlayerPageLayout {
    /**
     * 每一页最多渲染的玩家头像数量。
     * 目前按需求固定为 10，后续如果要调整页容量，只改这里即可。
     */
    public static final int PLAYERS_PER_PAGE = 10;

    /**
     * 头像按钮之间沿用原本界面的间距，保证视觉风格和旧版一致。
     */
    public static final int SLOT_APART = 36;

    /**
     * 原本这些玩家头像按钮的计算里自带一个 +9 的视觉修正，
     * 这里继续保留，避免改分页后整体位置出现半格偏移。
     */
    public static final int SLOT_X_OFFSET = 9;

    private PlayerPageLayout() {
    }

    /**
     * 玩家头像这一整排的 Y 坐标。
     * 继续沿用原本职业界面下方头像条的纵向位置。
     */
    public static int getPlayerRowY(int screenHeight) {
        return (screenHeight - 32) / 2 + 80;
    }

    /**
     * 按“当前页真实显示人数”来重新居中头像。
     *
     * 这样最后一页就算不足 10 人，也不会左对齐挤在一边，
     * 而是依旧和原版一样从中间展开。
     */
    public static int getCenteredPlayerStartX(int screenWidth, int visiblePlayerCount) {
        return screenWidth / 2 - visiblePlayerCount * SLOT_APART / 2 + SLOT_X_OFFSET;
    }

    /**
     * 计算“固定 10 个头像位”这整块区域的左侧起点。
     *
     * 翻页按钮不参与 10 个头像位的数量计算，
     * 但它们要稳定地挂在这 10 个头像位的左右两侧，
     * 因此按钮坐标要基于固定页宽来算，而不是基于当前页实际人数来算。
     */
    public static int getFixedAreaStartX(int screenWidth) {
        return screenWidth / 2 - PLAYERS_PER_PAGE * SLOT_APART / 2 + SLOT_X_OFFSET;
    }

    /**
     * 左侧“上一页”按钮的位置。
     */
    public static int getPreviousButtonX(int screenWidth) {
        return getFixedAreaStartX(screenWidth) - SLOT_APART;
    }

    /**
     * 右侧“下一页”按钮的位置。
     */
    public static int getNextButtonX(int screenWidth) {
        return getFixedAreaStartX(screenWidth) + PLAYERS_PER_PAGE * SLOT_APART;
    }

    /**
     * 计算“上一页按钮 + 当前页头像 + 下一页按钮”这一整条的起始 X。
     *
     * 和上一版不同，这次翻页按钮也一起参与居中排布。
     * 这样最后一页人数不足时，按钮不会还死死钉在最边上，
     * 而是会随着这一页实际显示出来的内容一起整体居中。
     */
    public static int getCenteredGroupStartX(int screenWidth, int visiblePlayerCount, boolean showPrevious, boolean showNext) {
        int buttonCount = (showPrevious ? 1 : 0) + (showNext ? 1 : 0);
        int totalSlots = visiblePlayerCount + buttonCount;
        return screenWidth / 2 - totalSlots * SLOT_APART / 2 + SLOT_X_OFFSET;
    }

    /**
     * 根据总玩家数计算总页数。
     *
     * 这里至少返回 1，避免极端情况下出现 0 页导致翻页逻辑除法/边界判断出错。
     */
    public static int getTotalPageCount(int totalPlayers) {
        return Math.max(1, (totalPlayers + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
    }
}
