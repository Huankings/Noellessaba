package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;

/**
 * 巫妖疯魔法杖。
 *
 * <p>左键近战击杀不在物品类里实现，而是通过巫妖疯魔 profile 接入 Wathe
 * {@code PsychoModeApi} 的通用球棒击杀链。这样护盾、锁栏、回收、回放都沿用 Wathe 现有流程。
 * 本类只负责右键瞬发 8 个亡灵骷髅。</p>
 */
public class LichPsychoStaffItem extends Item {
    public LichPsychoStaffItem(Settings settings) {
        super(settings);
    }

    public static AttributeModifiersComponent createAttributeModifiers() {
        /*
         * Wathe 的疯魔近战命中使用 PlayerEntity#getAttackCooldownProgress 判断是否“满力”。
         * 如果疯魔法杖只是普通 Item，就会继承空手 4.0 攻击速度，看起来像拳击一样快。
         * 这里只给主手补攻击速度，不额外增加攻击伤害；真正击杀仍交给 Wathe 疯魔 API。
         */
        return AttributeModifiersComponent.builder()
                .add(
                        EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                                LichConstants.PSYCHO_STAFF_ATTACK_SPEED_MODIFIER,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.MAINHAND
                )
                .build();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!LichItemUseRules.canUseLichDebugAwareItem(user, this)) {
            return TypedActionResult.fail(stack);
        }

        user.swingHand(hand, true);
        LichItemUseRules.playUseSoundFromPlayer(
                world,
                user,
                SoundEvents.ENTITY_WITHER_SHOOT,
                LichConstants.SKELETON_SHOOT_SOUND_VOLUME,
                LichConstants.SKELETON_SHOOT_SOUND_PITCH
        );

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            LichSkeletonSkullEntity.spawnFan(
                    serverPlayer,
                    NoellesRolesEntities.LICH_SKELETON_SKULL_ENTITY_TYPE,
                    LichSkeletonKind.UNDEAD,
                    LichConstants.PSYCHO_STAFF_SKELETON_COUNT,
                    LichConstants.PSYCHO_STAFF_FAN_DEGREES,
                    LichConstants.PSYCHO_STAFF_RANGE_BLOCKS,
                    LichConstants.PSYCHO_STAFF_SKULL_SPEED_BLOCKS_PER_TICK
            );
            GameRecordManager.recordItemUse(
                    serverPlayer,
                    Registries.ITEM.getId(ModItems.PSYCHO_STAFF),
                    null,
                    LichSkeletonKind.UNDEAD.createReplayData(serverPlayer.getServerWorld(), stack)
            );
        }

        if (!GameFunctions.isPlayerSpectatingOrCreative(user)) {
            user.getItemCooldownManager().set(this, LichConstants.PSYCHO_STAFF_COOLDOWN_TICKS);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(stack, world.isClient);
    }
}
