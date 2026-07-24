package org.agmas.noellesroles.modifiers.magnate;

public final class MagnateConstants {
    public static final int COLOR = 0xFFFF00;

    /**
     * 富豪需要把本次被动收入翻倍；这里保留为“额外补几份基础收入”，
     * 后续如果要从双倍改成三倍，只需要调整这个常量。
     */
    public static final int EXTRA_BASE_INCOME_COPIES = 1;

    private MagnateConstants() {
    }
}
