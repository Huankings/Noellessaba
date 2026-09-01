package org.agmas.noellesroles;

import dev.doctor4t.ratatouille.util.registrar.SoundEventRegistrar;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * NoellesRoles 自己的声音注册表。
 *
 * <p>目前弹簧陷阱只新增一段疯魔环境音。单独建注册类，是为了后续新增职业声音时
 * 不需要把 SoundEvent 静态字段塞回 Fabric 主入口。</p>
 */
public interface NoellesRolesSounds {
    SoundEventRegistrar REGISTRAR = new SoundEventRegistrar(NoellesRolesCore.MOD_ID);

    SoundEvent AMBIENT_SPRING_TRAP = REGISTRAR.create("ambient.spring_trap");
    SoundEvent AMBIENT_JASON = REGISTRAR.create("ambient.jason");
    SoundEvent AMBIENT_JASON_ABILITY = REGISTRAR.create("ambient.jason_ability");
    SoundEvent AMBIENT_JASON_ABILITY_LAST = REGISTRAR.create("ambient.jason_ability_last");
    SoundEvent AMBIENT_JASON_ABILITY_END = REGISTRAR.create("ambient.jason_ability_end");
    SoundEvent AMBIENT_JASON_JUMP_SCARE = REGISTRAR.create("ambient.jason_jump_scare");
    SoundEvent AMBIENT_SHADOW_JESTER_KING = REGISTRAR.create("ambient.shadow_jester_king");
    SoundEvent AMBIENT_SHADOW_JESTER_QUEEN = REGISTRAR.create("ambient.shadow_jester_queen");
    SoundEvent AMBIENT_LICH = REGISTRAR.create("ambient.lich");
    SoundEvent AMBIENT_VECNA = REGISTRAR.create("ambient.vecna");
    SoundEvent ITEM_SYRINGE_STAB = REGISTRAR.create("item.syringe_stab");

    static void initialize() {
        REGISTRAR.registerEntries();
    }
}
