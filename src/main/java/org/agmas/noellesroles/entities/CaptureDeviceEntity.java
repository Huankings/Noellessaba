package org.agmas.noellesroles.entities;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.capture.StunnedPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaptureDeviceEntity extends Entity {
    private static final int MAX_LIFETIME = 120*20; // 120秒
    private static final double DETECTION_RADIUS = 5.0;
    public static final int STUN_DURATION_TICKS = 20 * 5;

    private UUID ownerUuid;
    private int lifeTime = 0;

    public CaptureDeviceEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setOwner(UUID owner) {
        this.ownerUuid = owner;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("owner")) {
            ownerUuid = nbt.getUuid("owner");
        }
        lifeTime = nbt.getInt("lifeTime");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (ownerUuid != null) {
            nbt.putUuid("owner", ownerUuid);
        }
        nbt.putInt("lifeTime", lifeTime);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        lifeTime++;
        if (lifeTime > MAX_LIFETIME) {
            if (ownerUuid != null && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                NbtCompound extra = new NbtCompound();
                extra.putUuid("owner", ownerUuid);
                GameRecordManager.recordGlobalEvent(serverWorld, Noellesroles.CAPTURE_DEVICE_EXPIRED_EVENT, null, extra);
            }
            this.discard();
            return;
        }

        Box box = this.getBoundingBox().expand(DETECTION_RADIUS);
        List<PlayerEntity> players = this.getWorld().getEntitiesByClass(PlayerEntity.class, box, player ->
                player.isAlive() && !player.isSpectator() && !player.getUuid().equals(ownerUuid)
        );

        if (!players.isEmpty()) {
            // 1. 定身 + 缓慢效果 + 提示 + 音效
            for (PlayerEntity player : players) {
                // 定身
                StunnedPlayerComponent.KEY.get(player).stun(STUN_DURATION_TICKS);
                if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    NbtCompound extra = new NbtCompound();
                    extra.putUuid("victim", player.getUuid());
                    if (ownerUuid != null) {
                        extra.putUuid("owner", ownerUuid);
                    }
                    GameRecordManager.recordGlobalEvent(serverWorld, Noellesroles.CAPTURE_DEVICE_TRIGGERED_EVENT, null, extra);
                }

                // 缓慢 II 效果（5秒）
                if (player instanceof ServerPlayerEntity sp) {
                    sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, STUN_DURATION_TICKS, 1, false, false, true));

                    // 屏幕下方提示
                    sp.sendMessage(Text.literal("你被陷阱捕捉到了！").formatted(Formatting.RED), true);

                    // 播放铁砧落地音效给被捕捉者（仅自己）
                    sp.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }

            // 2. 生成报告（直接放入工程师背包）
            if (ownerUuid != null) {
                PlayerEntity owner = getWorld().getPlayerByUuid(ownerUuid);
                if (owner != null) {
                    // 移除旧报告
                    removeOldReports(owner);

                    // 创建新报告
                    ItemStack reportStack = Items.PAPER.getDefaultStack();
                    reportStack.set(DataComponentTypes.CUSTOM_NAME,
                            Text.literal("捕捉检测报告").formatted(Formatting.RESET, Formatting.GOLD));

                    List<Text> loreLines = new ArrayList<>();
                    loreLines.add(Text.translatable("item.noellesroles.capture_report.tooltip", players.size())
                            .formatted(Formatting.GRAY));
                    for (PlayerEntity p : players) {
                        loreLines.add(Text.literal(" - " + p.getName().getString()).formatted(Formatting.WHITE));
                    }
                    reportStack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));

                    // 直接给予背包（如果背包满则掉落在脚下）
                    owner.getInventory().offerOrDrop(reportStack);

                    // 播放铁砧落地音效给工程师
                    if (owner instanceof ServerPlayerEntity sp) {
                        sp.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }
                    if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                        NbtCompound extra = new NbtCompound();
                        extra.putUuid("owner", owner.getUuid());
                        GameRecordManager.recordGlobalEvent(serverWorld, Noellesroles.CAPTURE_DEVICE_REPORT_EVENT, null, extra);
                    }
                }
            }

            // 3. 陷阱消失
            this.discard();
        }
    }

    private void removeOldReports(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.PAPER)) {
                Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
                if (name != null && name.getString().equals("捕捉检测报告")) {
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
