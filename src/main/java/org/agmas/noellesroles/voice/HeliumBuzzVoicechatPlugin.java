package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/** 变声发送端插件入口；服务端只加载空壳，客户端再反射注册编码前的麦克风 PCM 处理器。 */
public final class HeliumBuzzVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "noellesroles_helium_buzz";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
        try {
            Class<?> handler = Class.forName("org.agmas.noellesroles.client.roles.party_animal.PartyAnimalVoiceChatClientAudioHandler");
            handler.getMethod("register", EventRegistration.class).invoke(null, registration);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("无法注册派对狂客户端变声处理器", exception);
        }
    }
}
