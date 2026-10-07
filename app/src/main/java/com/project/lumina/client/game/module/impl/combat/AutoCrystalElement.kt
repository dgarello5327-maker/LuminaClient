package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.EntityUnknown
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class AutoCrystalElement(
    iconResId: Int = AssetManager.getAsset("ic_sword_cross_black_24dp")
) : Element(
    name = "AutoCrystal",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_killaura_display_name")
) {
    private val range by floatValue("Range", 6.0f, 2f..12f)
    private val delay by intValue("Delay", 5, 1..40)
    private val packets by intValue("Packets", 1, 1..5)

    private var lastAttack = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || interceptablePacket.packet !is PlayerAuthInputPacket) return

        val now = System.currentTimeMillis()
        if (now - lastAttack < delay * 20L) return

        val crystals = session.level.entityMap.values
            .filterIsInstance<EntityUnknown>()
            .filter { entity ->
                val id = entity.identifier.lowercase()
                id.contains("ender_crystal") ||
                    id.contains("end_crystal") ||
                    id.contains("endcrystal")
            }
            .filter { it.distance(session.localPlayer) <= range }
            .sortedBy { it.distance(session.localPlayer) }

        val target = crystals.firstOrNull() ?: return

        repeat(packets) {
            session.localPlayer.attack(target)
        }
        lastAttack = now
    }
}
