package org.agmas.noellesroles.client.ui.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 选人分页界面的客户端状态缓存。
 *
 * 这份状态只在“当前这一局游戏”内保留：
 * - 玩家关闭再打开背包时，可以回到上一回浏览的页数。
 * - 新的一局开始、旧的一局结束时，要主动清空，避免上一局的页码污染下一局。
 *
 * 现在这里已经改成了通用的按 key 缓存页码：
 * - 每个职业/词条各自维护自己的当前页，不会互相串页。
 * - reset() 会在换局时统一清空整张表，保证缓存只在本局内生效。
 */
public final class PagedPlayerScreenState {
    public static final String SWAPPER_PAGE_KEY = "swapper";
    public static final String MORPHLING_PAGE_KEY = "morphling";
    public static final String VOODOO_PAGE_KEY = "voodoo";
    public static final String WINDER_PAGE_KEY = "winder";
    public static final String OPERATOR_PAGE_KEY = "operator";
    public static final String CORONER_PAGE_KEY = "coroner";
    public static final String BRAINWASHER_PAGE_KEY = "brainwasher";
    public static final String GODDESS_PAGE_KEY = "goddess";
    public static final String NOISEMAKER_PAGE_KEY = "noisemaker";
    public static final String GUESSER_PAGE_KEY = "guesser";
    public static final String CONTROLLER_PAGE_KEY = "controller";
    public static final String CORPSEMAKER_PAGE_KEY = "corpsemaker";

    /**
     * 按界面 key 保存对应页码。
     * 例如交换者翻到第 2 页，并不会影响变形怪、巫毒师等其它界面的当前页。
     */
    private static final Map<String, Integer> PAGE_CACHE = new HashMap<>();

    private PagedPlayerScreenState() {
    }

    public static int getPage(String key) {
        return PAGE_CACHE.getOrDefault(key, 0);
    }

    public static void setPage(String key, int page) {
        PAGE_CACHE.put(key, Math.max(0, page));
    }

    /**
     * 下面保留旧的专用方法，避免已经接好的交换者/变形怪代码必须同步大改。
     * 它们内部已经改成走同一张通用页码表。
     */
    public static int getSwapperPage() {
        return getPage(SWAPPER_PAGE_KEY);
    }

    public static void setSwapperPage(int page) {
        setPage(SWAPPER_PAGE_KEY, page);
    }

    public static int getMorphlingPage() {
        return getPage(MORPHLING_PAGE_KEY);
    }

    public static void setMorphlingPage(int page) {
        setPage(MORPHLING_PAGE_KEY, page);
    }

    /**
     * 开新局或结束当前对局时统一清空页码缓存。
     */
    public static void reset() {
        PAGE_CACHE.clear();
    }
}
