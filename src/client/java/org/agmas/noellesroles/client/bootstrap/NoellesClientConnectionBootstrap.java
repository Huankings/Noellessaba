package org.agmas.noellesroles.client.bootstrap;

/** 客户端连接生命周期注册入口。 */
public final class NoellesClientConnectionBootstrap {
    private NoellesClientConnectionBootstrap() {
    }

    public static void register() {
        NoellesClientResetHandler.register();
    }
}
