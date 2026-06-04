package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.agmas.noellesroles.roles.rememberer.RemembererSniperManager;
import org.jetbrains.annotations.NotNull;

/**
 * 狙击枪开火数据包。
 *
 * <p>只上传“开火当帧锁定的方向向量”，
 * 这样哪怕开枪后玩家立刻转头，服务端后续 20 tick 的弹道也会沿着原方向继续前进。</p>
 */
public record SniperRifleShootC2SPacket(double directionX, double directionY, double directionZ) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "sniper_rifle_shoot");
    public static final Id<SniperRifleShootC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, SniperRifleShootC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public SniperRifleShootC2SPacket decode(RegistryByteBuf buf) {
            return new SniperRifleShootC2SPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(RegistryByteBuf buf, SniperRifleShootC2SPacket value) {
            buf.writeDouble(value.directionX());
            buf.writeDouble(value.directionY());
            buf.writeDouble(value.directionZ());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void handle(SniperRifleShootC2SPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                return;
            }

            /*
             * 真正校验能否开火前，再用服务端“当前已经确认过的选槽状态”兜底同步一次部署冷却。
             *
             * 这样即使玩家把“切到狙击枪”和“立刻右键开火”压在同一小段时间里，
             * 只要服务端此时已经知道他当前手上的确是狙击枪，就会先补上部署冷却再判断开火。
             * 如果服务端此时还没收到切槽包，那么当前主手也还不是狙击枪，下面同样会直接拦住。
             */
            RemembererPlayerComponent remembererComponent = RemembererPlayerComponent.KEY.get(player);
            remembererComponent.syncSniperSelectionStateNow();

            ItemStack rifleStack = player.getMainHandStack();
            if (!rifleStack.isOf(ModItems.SNIPER_RIFLE)) {
                return;
            }
            if (player.getItemCooldownManager().isCoolingDown(ModItems.SNIPER_RIFLE)) {
                return;
            }

            int currentAmmo = rifleStack.getOrDefault(ModItems.SNIPER_AMMO, 0);
            if (currentAmmo <= 0) {
                return;
            }

            Vec3d direction = new Vec3d(payload.directionX(), payload.directionY(), payload.directionZ());
            if (!Double.isFinite(direction.x) || !Double.isFinite(direction.y) || !Double.isFinite(direction.z)) {
                return;
            }
            if (direction.lengthSquared() < 1.0E-6D) {
                return;
            }

            if (!player.isCreative()) {
                rifleStack.set(ModItems.SNIPER_AMMO, currentAmmo - 1);
            }
            player.playerScreenHandler.sendContentUpdates();

            // 开火本身也是一次“主动行为”，需要写进回放链路，方便追忆书回看时补上未命中的射击记录。
            GameRecordManager.recordItemUse(player, Registries.ITEM.getId(rifleStack.getItem()), null, null);

            remembererComponent.startSniperShotCooldown();
            player.getItemCooldownManager().set(ModItems.SNIPER_RIFLE, RemembererConstants.SNIPER_SHOT_COOLDOWN_TICKS);

            player.getWorld().playSound(
                    null,
                    player.getX(),
                    player.getEyeY(),
                    player.getZ(),
                    WatheSounds.ITEM_REVOLVER_SHOOT,
                    SoundCategory.PLAYERS,
                    5.0F,
                    1.0F + player.getRandom().nextFloat() * 0.1F - 0.05F
            );

            for (ServerPlayerEntity tracking : PlayerLookup.tracking(player)) {
                ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(player.getUuidAsString()));
            }
            ServerPlayNetworking.send(player, new ShootMuzzleS2CPayload(player.getUuidAsString()));

            RemembererSniperManager.fireShot(player, direction, rifleStack.copy());
        });
    }

    /**
     * 提供给客户端开火时直接构造“锁定方向”的便捷入口。
     */
    public static @NotNull SniperRifleShootC2SPacket fromLook(@NotNull Vec3d lookDirection) {
        Vec3d normalized = lookDirection.normalize();
        return new SniperRifleShootC2SPacket(normalized.x, normalized.y, normalized.z);
    }
}
