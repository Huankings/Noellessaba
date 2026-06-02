package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.agmas.noellesroles.roles.operator.OperatorCommunicationManager;
import org.agmas.noellesroles.roles.operator.OperatorPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistCommunicationManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class NoellesrolesVoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return Noellesroles.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoicechatPlugin.super.initialize(api);
    }

    public void paranoidEvent(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity spectator = resolveServerPlayer(event.getSenderConnection());
        if (spectator == null) {
            return;
        }

        // 新增：被控制者不能说话
        ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(spectator);
        if (controlledComp.isControlled) {
            event.cancel();
            return;
        }

        /*
         * 灵术师附身时，需要把自己的语音“改从宿主身上发出”。
         *
         * 这里直接取消原始麦克风包，再按宿主 UUID 手动广播新的实体语音包，
         * 这样：
         * 1. 语音显示出来的说话者会是宿主本人；
         * 2. 声音位置也会跟着宿主走；
         * 3. 同时还能套用灵术师自己的可听见规则过滤接收者。
         */
        SpiritualistPlayerComponent spiritualistComponent = SpiritualistPlayerComponent.KEY.get(spectator);
        ServerPlayerEntity possessionTarget = SpiritualistManager.getCurrentPossessionTarget(spectator);
        if (spiritualistComponent.isPossessing()
                && possessionTarget != null
                && GameFunctions.isPlayerAliveAndSurvival(possessionTarget)) {
            event.cancel();
            relayPossessionVoice(api, event, spectator, possessionTarget);
            return;
        }

        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(spectator.getWorld());
        if (spectator.interactionManager.getGameMode().equals(GameMode.SPECTATOR)) {
            spectator.getWorld().getPlayers().forEach((p) -> {
                if (gameWorldComponent.isRole(p, Noellesroles.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES) && GameFunctions.isPlayerAliveAndSurvival(p)) {
                    if (spectator.distanceTo(p) <= api.getVoiceChatDistance()) {
                        VoicechatConnection con = api.getConnectionOf(p.getUuid());
                        api.sendLocationalSoundPacketTo(con, event.getPacket().locationalSoundPacketBuilder()
                                        .position(api.createPosition(p.getX(), p.getY(), p.getZ()))
                                        .distance((float)api.getVoiceChatDistance())
                                        .build());
                    }
                }
            });
        }

        // 接线员的语音效果不替换原始 proximity voice，
        // 而是在默认语音之外额外补发“超距可听”的副本。
        relayOperatorVoice(api, event, spectator);
    }

    private void handleSoundPacket(SoundPacketEvent<?> event) {
        ServerPlayerEntity sender = resolveServerPlayer(event.getSenderConnection());
        ServerPlayerEntity receiver = resolveServerPlayer(event.getReceiverConnection());
        if (sender == null || receiver == null) {
            return;
        }

        if (shouldBlockVoiceBetween(sender, receiver)) {
            event.cancel();
        }
    }

    private void relayPossessionVoice(
            VoicechatServerApi api,
            MicrophonePacketEvent event,
            ServerPlayerEntity spiritualist,
            ServerPlayerEntity host
    ) {
        /*
         * 这里不能再用 event.getPacket().entitySoundPacketBuilder().entityUuid(hostUuid) 了。
         *
         * 我实际反编译了当前 simple-voice-chat 2.6.0 的实现，
         * 发现它的 EntitySoundPacket builder 在 build() 时完全忽略了 entityUuid 字段，
         * 最终仍然把“原麦克风发送者 UUID”写进了 PlayerSoundPacket。
         * 这就是为什么你测试时，声音仍然从灵术师本体位置发出。
         *
         * 但这些内部实现类当前不在项目编译类路径里，
         * 所以这里继续走 API builder，只是在 build 前用反射把真正参与构包的底层字段修正成宿主。
         */
        EntitySoundPacket.Builder<?> redirectedBuilder = event.getPacket()
                .entitySoundPacketBuilder()
                .entityUuid(host.getUuid())
                .whispering(event.getPacket().isWhispering())
                .distance((float) api.getVoiceChatDistance());
        retargetEntitySoundBuilder(
                redirectedBuilder,
                getPossessionVoiceChannelId(spiritualist, host),
                host.getUuid()
        );
        EntitySoundPacket redirectedPacket = redirectedBuilder.build();

        for (ServerPlayerEntity recipient : spiritualist.getServer().getPlayerManager().getPlayerList()) {
            /*
             * 这份“从宿主身上发出的重定向语音”不应该再回送给灵术师自己。
             *
             * 否则灵术师会像开着本地监听一样，持续听到自己刚说出去的那份实体语音。
             */
            if (recipient.getUuid().equals(spiritualist.getUuid())) {
                continue;
            }

            if (!canHearPossessingSpiritualist(recipient)) {
                continue;
            }

            VoicechatConnection connection = api.getConnectionOf(recipient.getUuid());
            if (connection == null) {
                continue;
            }

            api.sendEntitySoundPacketTo(connection, redirectedPacket);
        }
    }

    /**
     * 为“灵术师通过宿主发声”生成一条稳定但不和普通玩家麦克风冲突的语音通道 ID。
     */
    private UUID getPossessionVoiceChannelId(ServerPlayerEntity spiritualist, ServerPlayerEntity host) {
        return UUID.nameUUIDFromBytes(
                (Noellesroles.MOD_ID + ":spiritualist_voice:" + spiritualist.getUuid() + ":" + host.getUuid())
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 反射修正 voicechat builder 内部真正被 build() 使用的 sender/channelId 字段。
     *
     * <p>当前 2.6.0 版里，公开的 entityUuid(...) 最终不会落进真正发送出去的 PlayerSoundPacket，
     * 所以这里只能在运行时补一刀，确保声源实体真的切到宿主。</p>
     */
    private void retargetEntitySoundBuilder(
            EntitySoundPacket.Builder<?> builder,
            UUID channelId,
            UUID senderUuid
    ) {
        try {
            setFieldRecursively(builder, "sender", senderUuid);
            setFieldRecursively(builder, "channelId", channelId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to retarget spiritualist possession voice packet", exception);
        }
    }

    private void setFieldRecursively(Object instance, String fieldName, Object value) throws ReflectiveOperationException {
        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private boolean shouldBlockVoiceBetween(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        if (SpiritualistPlayerComponent.KEY.get(receiver).isProjecting()) {
            return true;
        }

        SpiritualistPlayerComponent senderSpiritualist = SpiritualistPlayerComponent.KEY.get(sender);
        if (senderSpiritualist.isProjecting()) {
            return !SpiritualistCommunicationManager.isOutOfGameAudience(receiver);
        }

        if (senderSpiritualist.isPossessing()) {
            // 灵术师附身中的语音已在麦克风事件里被手动重发，这里把原始那份全部拦掉。
            return true;
        }

        SpiritualistHostComponent senderHost = SpiritualistHostComponent.KEY.get(sender);
        if (senderHost.possessed && senderHost.spiritualistController != null) {
            return !SpiritualistCommunicationManager.isOutOfGameAudience(receiver)
                    && !receiver.getUuid().equals(senderHost.spiritualistController);
        }

        return false;
    }

    private boolean canHearPossessingSpiritualist(ServerPlayerEntity recipient) {
        return !SpiritualistPlayerComponent.KEY.get(recipient).isProjecting();
    }

    private void relayOperatorVoice(
            VoicechatServerApi api,
            MicrophonePacketEvent event,
            ServerPlayerEntity sender
    ) {
        if (!OperatorCommunicationManager.isLiveConnectionEndpoint(sender)) {
            return;
        }

        /*
         * 接线员的效果要求是“无论多远，都像直接在耳边听到一样”。
         *
         * EntitySoundPacket / LocationalSoundPacket 都仍然带有空间位置，
         * 客户端会继续按声源距离做衰减甚至直接静音。
         * 这正是之前“明明转发了包，但远距离还是听不到”的根因。
         *
         * 这里改用 StaticSoundPacket：
         * 1. 它是 non-directional，不绑定世界位置；
         * 2. 接收者会像群聊语音那样直接听到；
         * 3. 正好符合你要求的“相当于直接在耳边播放”。
         */
        StaticSoundPacket redirectedPacket = event.getPacket().staticSoundPacketBuilder().build();

        relayOperatorConnectionVoice(api, sender, redirectedPacket);
        relayOperatorBroadcastVoice(api, sender, redirectedPacket);
    }

    private void relayOperatorConnectionVoice(
            VoicechatServerApi api,
            ServerPlayerEntity sender,
            StaticSoundPacket redirectedPacket
    ) {
        for (ServerPlayerEntity possibleOperator : sender.getServer().getPlayerManager().getPlayerList()) {
            OperatorPlayerComponent component = OperatorPlayerComponent.KEY.get(possibleOperator);
            OperatorCommunicationManager.UUIDPair pair = OperatorCommunicationManager.getConnectionPair(component);
            if (pair == null) {
                continue;
            }

            UUID recipientUuid = null;
            if (sender.getUuid().equals(pair.first())) {
                recipientUuid = pair.second();
            } else if (sender.getUuid().equals(pair.second())) {
                recipientUuid = pair.first();
            }

            if (recipientUuid == null) {
                continue;
            }

            ServerPlayerEntity recipient = sender.getServer().getPlayerManager().getPlayer(recipientUuid);
            if (!OperatorCommunicationManager.isLiveConnectionEndpoint(recipient)) {
                continue;
            }

            VoicechatConnection connection = api.getConnectionOf(recipientUuid);
            if (connection != null) {
                api.sendStaticSoundPacketTo(connection, redirectedPacket);
            }
        }
    }

    private void relayOperatorBroadcastVoice(
            VoicechatServerApi api,
            ServerPlayerEntity sender,
            StaticSoundPacket redirectedPacket
    ) {
        for (ServerPlayerEntity possibleOperator : sender.getServer().getPlayerManager().getPlayerList()) {
            OperatorPlayerComponent component = OperatorPlayerComponent.KEY.get(possibleOperator);
            if (!component.hasActiveBroadcast() || component.getBroadcastTarget() == null) {
                continue;
            }
            if (!sender.getUuid().equals(component.getBroadcastTarget())) {
                continue;
            }

            for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
                if (recipient.getUuid().equals(sender.getUuid())) {
                    continue;
                }
                if (!OperatorCommunicationManager.isLiveConnectionEndpoint(recipient)) {
                    continue;
                }
                VoicechatConnection connection = api.getConnectionOf(recipient.getUuid());
                if (connection != null) {
                    api.sendStaticSoundPacketTo(connection, redirectedPacket);
                }
            }
        }
    }

    private ServerPlayerEntity resolveServerPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null || connection.getPlayer().getPlayer() == null) {
            return null;
        }

        Object rawPlayer = connection.getPlayer().getPlayer();
        return rawPlayer instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::paranoidEvent);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::handleSoundPacket);
        registration.registerEvent(EntitySoundPacketEvent.class, this::handleSoundPacket);
        registration.registerEvent(StaticSoundPacketEvent.class, this::handleSoundPacket);
        VoicechatPlugin.super.registerEvents(registration);
    }
}
