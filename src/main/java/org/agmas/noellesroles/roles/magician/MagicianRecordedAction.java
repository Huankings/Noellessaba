package org.agmas.noellesroles.roles.magician;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * 魔术师录制期捕获到的一条“语义动作”。
 *
 * <p>和逐 tick 轨迹帧不同，这里保存的是“这一刻到底做了什么交互”：
 * 1. 左键攻击；
 * 2. 右键使用主手/副手；
 * 3. 停止蓄力使用；
 * 4. 切换选槽；
 * 5. Wathe / NoellesRoles 自定义武器动作。</p>
 *
 * <p>方块右键会额外保存原始 {@link BlockHitResult}。
 * 播放时优先按录制当刻点中的方块、面和命中坐标执行，
 * 这样按钮、未上锁门、床这类交互不会因为播放体穿墙/门状态变化而被新的 raycast 错过。</p>
 */
public final class MagicianRecordedAction {

    public enum Type {
        ATTACK,
        USE_MAIN_HAND,
        USE_OFF_HAND,
        RELEASE_USE_ITEM,
        SWING_MAIN_HAND,
        SWING_OFF_HAND,
        SELECT_SLOT,
        GUN_SHOOT,
        KNIFE_STAB,
        BAYONET_STAB,
        BAYONET_KNOCKBACK,
        SNIPER_SHOOT
    }

    public final int tick;
    public final Type type;
    @Nullable public final Hand hand;
    public final int intValue;
    public final double x;
    public final double y;
    public final double z;
    @Nullable public final BlockPos blockPos;
    @Nullable public final Direction blockSide;
    public final double blockHitX;
    public final double blockHitY;
    public final double blockHitZ;
    public final boolean insideBlock;

    private MagicianRecordedAction(
            int tick,
            Type type,
            @Nullable Hand hand,
            int intValue,
            double x,
            double y,
            double z,
            @Nullable BlockPos blockPos,
            @Nullable Direction blockSide,
            double blockHitX,
            double blockHitY,
            double blockHitZ,
            boolean insideBlock
    ) {
        this.tick = tick;
        this.type = type;
        this.hand = hand;
        this.intValue = intValue;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockPos = blockPos;
        this.blockSide = blockSide;
        this.blockHitX = blockHitX;
        this.blockHitY = blockHitY;
        this.blockHitZ = blockHitZ;
        this.insideBlock = insideBlock;
    }

    public static MagicianRecordedAction attack(int tick) {
        return createSimple(tick, Type.ATTACK, null, 0);
    }

    public static MagicianRecordedAction useHand(int tick, Hand hand) {
        return createSimple(tick, hand == Hand.MAIN_HAND ? Type.USE_MAIN_HAND : Type.USE_OFF_HAND, hand, 0);
    }

    public static MagicianRecordedAction useBlock(int tick, Hand hand, BlockHitResult hitResult) {
        Vec3d hitPos = hitResult.getPos();
        return new MagicianRecordedAction(
                tick,
                hand == Hand.MAIN_HAND ? Type.USE_MAIN_HAND : Type.USE_OFF_HAND,
                hand,
                0,
                0.0D,
                0.0D,
                0.0D,
                hitResult.getBlockPos(),
                hitResult.getSide(),
                hitPos.x,
                hitPos.y,
                hitPos.z,
                hitResult.isInsideBlock()
        );
    }

    public static MagicianRecordedAction releaseUseItem(int tick) {
        return createSimple(tick, Type.RELEASE_USE_ITEM, null, 0);
    }

    public static MagicianRecordedAction swingHand(int tick, Hand hand) {
        return createSimple(
                tick,
                hand == Hand.MAIN_HAND ? Type.SWING_MAIN_HAND : Type.SWING_OFF_HAND,
                hand,
                0
        );
    }

    public static MagicianRecordedAction selectSlot(int tick, int slot) {
        return createSimple(tick, Type.SELECT_SLOT, null, slot);
    }

    public static MagicianRecordedAction gunShoot(int tick) {
        return createSimple(tick, Type.GUN_SHOOT, null, 0);
    }

    public static MagicianRecordedAction knifeStab(int tick) {
        return createSimple(tick, Type.KNIFE_STAB, null, 0);
    }

    public static MagicianRecordedAction bayonetStab(int tick) {
        return createSimple(tick, Type.BAYONET_STAB, null, 0);
    }

    public static MagicianRecordedAction bayonetKnockback(int tick) {
        return createSimple(tick, Type.BAYONET_KNOCKBACK, null, 0);
    }

    public static MagicianRecordedAction sniperShoot(int tick, double x, double y, double z) {
        return new MagicianRecordedAction(tick, Type.SNIPER_SHOOT, null, 0, x, y, z, null, null, 0.0D, 0.0D, 0.0D, false);
    }

    public @Nullable BlockHitResult createRecordedBlockHitResult() {
        if (this.blockPos == null || this.blockSide == null) {
            return null;
        }
        return new BlockHitResult(
                new Vec3d(this.blockHitX, this.blockHitY, this.blockHitZ),
                this.blockSide,
                this.blockPos,
                this.insideBlock
        );
    }

    private static MagicianRecordedAction createSimple(int tick, Type type, @Nullable Hand hand, int intValue) {
        return new MagicianRecordedAction(tick, type, hand, intValue, 0.0D, 0.0D, 0.0D, null, null, 0.0D, 0.0D, 0.0D, false);
    }
}
