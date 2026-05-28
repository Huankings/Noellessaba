package org.agmas.noellesroles.client.ui.roles.corpsemaker;

import java.util.UUID;

public class CorpsemakerState {
    public static UUID selectedPlayerUuid = null;
    public static String selectedDeathReason = null;
    public static CorpsemakerPhase phase = CorpsemakerPhase.PLAYER_SELECT;

    public static void reset() {
        selectedPlayerUuid = null;
        selectedDeathReason = null;
        phase = CorpsemakerPhase.PLAYER_SELECT;
    }
}