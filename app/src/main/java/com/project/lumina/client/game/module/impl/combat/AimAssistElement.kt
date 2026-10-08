package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.R
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.Entity
import com.project.lumina.client.game.entity.LocalPlayer
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.atan2
import kotlin.math.sqrt

class AimAssistElement : Element(
    name = "AimAssist",
    category = CheatCategory.Combat,
    displayNameResId = R.string.module_killaura_display_name
) {
    private val range by floatValue("Range", 8f, 2f..16f)
    private val strength by floatValue("Strength", 0.35f, 0.05f..1f)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || interceptablePacket.packet !is PlayerAuthInputPacket || !isSessionCreated) return
        val packet = interceptablePacket.packet as PlayerAuthInputPacket
        val target = findTarget() ?: return
        val p = session.localPlayer.vec3Position
        val t = target.vec3Position
        val dx = t.x - p.x
        val dy = t.y - p.y
        val dz = t.z - p.z
        val horizontal = sqrt(dx * dx + dz * dz)
        val targetYaw = Math.toDegrees(atan2(-dx.toDouble(), dz.toDouble())).toFloat()
        val targetPitch = Math.toDegrees(-atan2(dy.toDouble(), horizontal.toDouble())).toFloat()
        val r = packet.rotation
        packet.rotation = Vector3f.from(
            r.x + (targetPitch - r.x) * strength,
            r.y + (targetYaw - r.y) * strength,
            r.z + (targetYaw - r.z) * strength
        )
    }

    private fun findTarget(): Entity? =
        session.level.entityMap.values
            .filter { it !is LocalPlayer && it.distance(session.localPlayer) <= range && it.isValid() }
            .minByOrNull { it.distance(session.localPlayer) }
}
