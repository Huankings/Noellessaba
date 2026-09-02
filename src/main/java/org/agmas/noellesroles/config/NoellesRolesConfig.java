package org.agmas.noellesroles.config;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.game.GameConstants;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.util.List;

public class NoellesRolesConfig {
    public static ConfigClassHandler<NoellesRolesConfig> HANDLER = ConfigClassHandler.createBuilder(NoellesRolesConfig.class)
            .id(Identifier.of(NoellesRolesCore.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve( NoellesRolesCore.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry(comment = "Whether insane players will randomly see people as morphed.")
    public boolean insanePlayersSeeMorphs = true;
    @SerialEntry(comment = "Allows the shitpost roles to retain their disable/enable state after a server restart")
    public boolean shitpostRoles = false;

    @SerialEntry(comment = "Starting cooldown (in ticks)")
    public int generalCooldownTicks = GameConstants.getInTicks(0,30);

    @SerialEntry(comment = "Allow Natural deaths to trigger voodoo (deaths without an assigned killer)")
    public boolean voodooNonKillerDeaths = false;

    @SerialEntry(comment = "Makes voodoos act like Evil players when shot by a revolver (no backfire, no gun lost)")
    public boolean voodooShotLikeEvil = true;

    @SerialEntry(comment = "Civillians can get the guesser modifier.")
    public boolean allowCivillianGuessers = false;

    @SerialEntry(comment = "How the guesser dies after an incorrect guess.\n\"none\" (default) - nothing happens, 2 minute cooldown applied\n\"death\" kills the player with a voodoo death message\n\"explode\" explodes the guesser, killing anyone nearby")
    public String guesserDiesAfterIncorrectGuess = "none";

    @SerialEntry(comment = "How many players must be online for the Master Key to look like a master key and not a lockpick. (0 = key always looks like a lockpick, 1-6 = key always looks normal)")
    public int playerCountToMakeConducterKeyVisible = 10;

    /** 乘务员掉落物本能提示，默认关闭以保持旧行为。 */
    @SerialEntry(comment = "Conductor: show dropped items through instinct highlighting (disabled by default).")
    public boolean conductorDroppedItemInstinct = false;

    /** 验尸官尸体本能提示，默认关闭以保持旧行为。 */
    @SerialEntry(comment = "Coroner: show bodies through instinct highlighting when mood is not low (disabled by default).")
    public boolean coronerBodyInstinct = false;

    /** 狂信者疯魔攻击杀手的拦截规则，默认关闭；BAT 死因始终保留例外。 */
    @SerialEntry(comment = "Jester: prevent attacking killer players in Psycho Mode (disabled by default; BAT remains an exception).")
    public boolean jesterPsychoCannotAttackKiller = false;

    @SerialEntry(comment = "Minimum participating player count required before the Dual Personality modifier can enter the random modifier pool. This is intentionally configurable and can be changed with /noellesroles constants minplayerspawn dual_personality.")
    public int dualPersonalityMinPlayerSpawn = DualPersonalityConstants.DEFAULT_MIN_RANDOM_PLAYER_COUNT;
}
