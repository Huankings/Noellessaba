package org.agmas.noellesroles.packet.role.noisemaker;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerConstants;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerGlowTargetComponent;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerPlayerComponent;
import dev.doctor4t.wathe.record.GameRecordManager;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public record NoisemakerGlowC2SPacket(UUID target) implements CustomPayload {
    public static final Identifier NOISEMAKER_GLOW_ID = Identifier.of(Noellesroles.MOD_ID, "noisemaker_glow");
    public static final CustomPayload.Id<NoisemakerGlowC2SPacket> ID = new CustomPayload.Id<>(NOISEMAKER_GLOW_ID);
    public static final PacketCodec<RegistryByteBuf, NoisemakerGlowC2SPacket> CODEC;

    public NoisemakerGlowC2SPacket(UUID target) {
        this.target = target;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.target);
    }

    public static NoisemakerGlowC2SPacket read(PacketByteBuf buf) {
        return new NoisemakerGlowC2SPacket(buf.readUuid());
    }

    public UUID target() {
        return this.target;
    }

    // 处理函数
    public static void handle(NoisemakerGlowC2SPacket packet, ServerPlayNetworkHandler handler) {
        ServerPlayerEntity player = handler.getPlayer();
        MinecraftServer server = player.getServer();

        if (server != null) {
            server.execute(() -> {
                // 检查玩家是否是大嗓门
                NoisemakerPlayerComponent comp = NoisemakerPlayerComponent.KEY.get(player);

                if (comp.tryUseAbility(packet.target())) {
                    // 查找目标玩家
                    ServerPlayerEntity target = server.getPlayerManager().getPlayer(packet.target());

                    if (target != null) {
                        // 检查游戏模式：只有生存和冒险模式可以发光
                        if (target.interactionManager.getGameMode().isSurvivalLike()) {
                            // 应用发光效果，并把“结束回放”的追踪状态也一起挂到目标身上。
                            target.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.GLOWING,  // 发光效果
                                    NoisemakerConstants.GLOW_DURATION_TICKS,
                                    0,                      // 等级0
                                    false,                  // 环境效果
                                    true,                   // 显示粒子
                                    true                    // 显示图标
                            ));

                            NoisemakerGlowTargetComponent.KEY.get(target)
                                    .start(player.getUuid(), NoisemakerConstants.GLOW_DURATION_TICKS);

                            NbtCompound extra = new NbtCompound();
                            extra.putUuid("target_player", target.getUuid());
                            GameRecordManager.recordGlobalEvent(
                                    player.getServerWorld(),
                                    Noellesroles.NOISEMAKER_GLOW_STARTED_EVENT,
                                    player,
                                    extra
                            );
                        }
                        // 如果目标在创造或旁观模式，什么也不做，但冷却仍然生效
                    }
                }
            });
        }
    }

    static {
        CODEC = PacketCodec.of(NoisemakerGlowC2SPacket::write, NoisemakerGlowC2SPacket::read);
    }
}
