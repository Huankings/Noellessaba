package org.agmas.noellesroles.roles.rememberer;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 追忆者记忆状态和旧记忆书的玩家硬重置规则。 */
public final class RemembererResetHandler {
    private RemembererResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            RemembererPlayerComponent.KEY.get(player).reset();
            RemembererReplayBookBuilder.removeOldMemoryBooks(player);
        });
    }
}
