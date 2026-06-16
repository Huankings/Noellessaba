package org.agmas.noellesroles.roles.magician;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔术师录制开始时保存的一份背包快照。
 *
 * <p>播放皮套时使用的是这份快照，而不是魔术师当前真实背包，原因是：
 * 1. 回放期间不应消耗魔术师手里的真实道具；
 * 2. 录制之后哪怕本体背包已经变化，皮套仍应复刻“录制那一刻拥有的物品状态”。</p>
 */
public final class MagicianInventorySnapshot {

    private final List<ItemStack> main = new ArrayList<>();
    private final List<ItemStack> armor = new ArrayList<>();
    private final List<ItemStack> offHand = new ArrayList<>();
    private final int selectedSlot;

    private MagicianInventorySnapshot(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.main) {
            this.main.add(stack.copy());
        }
        for (ItemStack stack : inventory.armor) {
            this.armor.add(stack.copy());
        }
        for (ItemStack stack : inventory.offHand) {
            this.offHand.add(stack.copy());
        }
        this.selectedSlot = inventory.selectedSlot;
    }

    public static MagicianInventorySnapshot capture(PlayerEntity player) {
        return new MagicianInventorySnapshot(player);
    }

    /**
     * 把录制快照完整复制到目标玩家实体上。
     *
     * <p>这里会先清空现有栏位，再按快照恢复，避免上一次播放残留内容污染下一次皮套。</p>
     */
    public void applyTo(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();

        inventory.main.clear();
        inventory.armor.clear();
        inventory.offHand.clear();

        /*
         * PlayerInventory 使用的是 DefaultedList。
         *
         * 这里的 clear() 只会把已有槽位重置成 EMPTY，不会把列表长度清成 0。
         * 如果再用 add() 追加，就会把 36 格主背包扩成 72 格，假玩家后续使用物品时
         * 很容易出现不可预期的异常。因此恢复快照时必须按槽位 set 回去。
         */
        for (int index = 0; index < inventory.main.size() && index < this.main.size(); index++) {
            inventory.main.set(index, this.main.get(index).copy());
        }
        for (int index = 0; index < inventory.armor.size() && index < this.armor.size(); index++) {
            inventory.armor.set(index, this.armor.get(index).copy());
        }
        for (int index = 0; index < inventory.offHand.size() && index < this.offHand.size(); index++) {
            inventory.offHand.set(index, this.offHand.get(index).copy());
        }
        inventory.selectedSlot = Math.max(0, Math.min(this.selectedSlot, 8));
    }

    /**
     * 把录制快照里的“可见装备状态”同步到皮套实体。
     *
     * <p>皮套不是原版玩家，没有整套 PlayerInventory，
     * 因此这里只复制渲染和动作复刻真正会用到的槽位：</p>
     * <p>1. 主手当前选槽；</p>
     * <p>2. 副手；</p>
     * <p>3. 四个护甲槽。</p>
     */
    public void applyTo(MagicianPlaybackEntity playbackEntity) {
        playbackEntity.clearEquipment();
        int safeSelectedSlot = Math.max(0, Math.min(this.selectedSlot, 8));
        if (safeSelectedSlot < this.main.size()) {
            playbackEntity.equipStack(EquipmentSlot.MAINHAND, this.main.get(safeSelectedSlot));
        }
        if (!this.offHand.isEmpty()) {
            playbackEntity.equipStack(EquipmentSlot.OFFHAND, this.offHand.getFirst());
        }
        if (this.armor.size() >= 4) {
            playbackEntity.equipStack(EquipmentSlot.FEET, this.armor.get(0));
            playbackEntity.equipStack(EquipmentSlot.LEGS, this.armor.get(1));
            playbackEntity.equipStack(EquipmentSlot.CHEST, this.armor.get(2));
            playbackEntity.equipStack(EquipmentSlot.HEAD, this.armor.get(3));
        }
    }

    /**
     * 按指定快捷栏槽位把“当前应该可见的装备”同步到皮套实体。
     *
     * <p>录制过程中玩家可能切换过快捷栏，
     * 所以播放时不能永远只显示录制开始那一刻的选中物品，
     * 而是要跟随每一帧记录到的 selectedSlot 实时变更主手。</p>
     */
    public void applyTo(MagicianPlaybackEntity playbackEntity, int selectedSlot) {
        playbackEntity.clearEquipment();
        int safeSelectedSlot = Math.max(0, Math.min(selectedSlot, 8));
        if (safeSelectedSlot < this.main.size()) {
            playbackEntity.equipStack(EquipmentSlot.MAINHAND, this.main.get(safeSelectedSlot));
        }
        if (!this.offHand.isEmpty()) {
            playbackEntity.equipStack(EquipmentSlot.OFFHAND, this.offHand.getFirst());
        }
        if (this.armor.size() >= 4) {
            playbackEntity.equipStack(EquipmentSlot.FEET, this.armor.get(0));
            playbackEntity.equipStack(EquipmentSlot.LEGS, this.armor.get(1));
            playbackEntity.equipStack(EquipmentSlot.CHEST, this.armor.get(2));
            playbackEntity.equipStack(EquipmentSlot.HEAD, this.armor.get(3));
        }
    }

    /**
     * 读取指定快捷栏槽位里的快照物品。
     *
     * <p>播放时需要把这一格同步给代理玩家与可见皮套，
     * 因此这里单独暴露一个按槽位读取的只读入口。</p>
     */
    public ItemStack getMainStackAtSlot(int slot) {
        int safeSlot = Math.max(0, Math.min(slot, 8));
        if (safeSlot >= this.main.size()) {
            return ItemStack.EMPTY;
        }
        return this.main.get(safeSlot).copy();
    }

    public ItemStack getOffHandStack() {
        return this.offHand.isEmpty() ? ItemStack.EMPTY : this.offHand.getFirst().copy();
    }

    public ItemStack getArmorStack(EquipmentSlot slot) {
        if (this.armor.size() < 4) {
            return ItemStack.EMPTY;
        }
        return switch (slot) {
            case FEET -> this.armor.get(0).copy();
            case LEGS -> this.armor.get(1).copy();
            case CHEST -> this.armor.get(2).copy();
            case HEAD -> this.armor.get(3).copy();
            default -> ItemStack.EMPTY;
        };
    }

    public int getSelectedSlot() {
        return Math.max(0, Math.min(this.selectedSlot, 8));
    }

    /**
     * 直接按“当前玩家身上的真实可见装备状态”同步到皮套实体。
     *
     * <p>魔术师播放期间，代理玩家会真实消耗临时快照里的手雷、飞斧等一次性物品。
     * 如果皮套仍然每 tick 都按照录制开始时的固定快照渲染，就会出现：
     * 已经扔掉的物品仍然卡在手上的错位表现。
     *
     * <p>因此播放阶段真正应该给客户端看的，是代理玩家此刻剩下的主手/副手/护甲状态。</p>
     */
    public static void syncVisibleEquipment(PlayerEntity source, MagicianPlaybackEntity playbackEntity) {
        playbackEntity.clearEquipment();
        PlayerInventory inventory = source.getInventory();
        int slot = Math.max(0, Math.min(inventory.selectedSlot, 8));
        if (slot < inventory.main.size()) {
            playbackEntity.equipStack(EquipmentSlot.MAINHAND, inventory.main.get(slot));
        }
        if (!inventory.offHand.isEmpty()) {
            playbackEntity.equipStack(EquipmentSlot.OFFHAND, inventory.offHand.getFirst());
        }
        if (inventory.armor.size() >= 4) {
            playbackEntity.equipStack(EquipmentSlot.FEET, inventory.armor.get(0));
            playbackEntity.equipStack(EquipmentSlot.LEGS, inventory.armor.get(1));
            playbackEntity.equipStack(EquipmentSlot.CHEST, inventory.armor.get(2));
            playbackEntity.equipStack(EquipmentSlot.HEAD, inventory.armor.get(3));
        }
    }
}
