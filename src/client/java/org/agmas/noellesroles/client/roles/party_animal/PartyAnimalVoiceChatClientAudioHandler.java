package org.agmas.noellesroles.client.roles.party_animal;

import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.roles.party_animal.HeliumBuzzPlayerComponent;
import org.agmas.noellesroles.roles.party_animal.HeliumPitchShifter;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalConstants;

import java.util.UUID;

/**
 * 派对狂的麦克风源头变声，在 Simple Voice Chat 编码和发送 PCM 之前执行。
 *
 * <p>接线员/双重人格/电话使用静态语音，灵术师与变形试剂还会改写声源实体 UUID。
 * 如果等到接收端查实体再变声，既会遗漏静态/位置语音，也会在远距离无实体时失效，
 * 甚至把借来的声音错误地套用播放实体的等级。现在只处理原始说话人自己的麦克风，
 * 服务端各路转发会自然复用已经变声的 Opus 数据，每个副本只变声一次。</p>
 */
public final class PartyAnimalVoiceChatClientAudioHandler {
    /** 仅缓存本机麦克风的流式处理器，不能按远端实体共用音频管线。 */
    private static HeliumPitchShifter shifter;
    /** 当前管线所属的本地说话人，换账号或重新进服后必须重新建管线。 */
    private static UUID speakerUuid;
    /** 当前管线的等级；能力、调试指令或时停回溯改变等级后重新建管线。 */
    private static int pitchLevel;
    /** 上一次处理麦克风 PCM 的单调时钟时间，防止停麦后残留旧语音尾音。 */
    private static long lastFrameNanos;

    private PartyAnimalVoiceChatClientAudioHandler() {
    }

    public static void register(EventRegistration registration) {
        /*
         * ClientSoundEvent 是公开的编码前入口，不需要干涉服务端通信权限、包构造或 Opus 编解码。
         * 不再注册 ClientReceiveSoundEvent，避免收听各路转发时重复升/降调。
         */
        registration.registerEvent(ClientSoundEvent.class, PartyAnimalVoiceChatClientAudioHandler::handle);
    }

    /**
     * JOIN/DISCONNECT 清理入口。音频回调在麦克风线程运行，与主线程重置共用同一把锁，
     * 避免非线程安全的 WSOLA 管线处理到一半被替换。
     * 这些 PCM 缓冲属于客户端播放/采集缓存，不应加入时停快照；实际等级仍读取已同步的 CCA。
     */
    public static synchronized void reset() {
        shifter = null;
        speakerUuid = null;
        pitchLevel = 0;
        lastFrameNanos = 0L;
    }

    private static synchronized void handle(ClientSoundEvent event) {
        short[] pcm = event.getRawAudio();
        MinecraftClient client = MinecraftClient.getInstance();
        if (pcm == null || pcm.length == 0 || client.world == null || client.player == null) {
            reset();
            return;
        }

        /*
         * 必须使用原始麦克风说话人的等级，不读取听者、宿主或变形试剂的播放实体。
         * 本人 CCA 无需依赖远端实体追踪距离；普通等级、永久调试覆盖和回溯恢复共用同一读法。
         */
        int level = HeliumBuzzPlayerComponent.KEY.get(client.player).getEffectiveLevel();
        if (level == 0) {
            reset();
            return;
        }

        long now = System.nanoTime();
        UUID currentSpeaker = client.player.getUuid();
        if (shifter == null || !currentSpeaker.equals(speakerUuid) || pitchLevel != level
                || now - lastFrameNanos >= PartyAnimalConstants.VOICE_STREAM_IDLE_RESET_NANOS) {
            /*
             * Voice Chat 2.6.0 停止传输时不发送空的 ClientSoundEvent，故还要用空闲间隔兜底。
             * 换等级、回溯、停麦或换账号后，下一帧从干净管线开始，避免未来帧/旧语句串音。
             */
            shifter = new HeliumPitchShifter();
            speakerUuid = currentSpeaker;
            pitchLevel = level;
        }
        lastFrameNanos = now;
        event.setRawAudio(shifter.process(pcm, ratioFor(level)));
    }

    private static float ratioFor(int level) {
        float positive = switch (Math.abs(level)) {
            case 1 -> PartyAnimalConstants.PITCH_UP_LEVEL_1;
            case 2 -> PartyAnimalConstants.PITCH_UP_LEVEL_2;
            default -> PartyAnimalConstants.PITCH_UP_LEVEL_3;
        };
        return level > 0 ? positive : 1.0F / positive;
    }
}
