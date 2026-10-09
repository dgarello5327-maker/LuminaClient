package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.R
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.Entity
import com.project.lumina.client.game.entity.LocalPlayer
import com.project.lumina.client.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.atan2
import kotlin.math.sqrt

class AutoAimElement : Element(
    name = "AutoAim",
    category = CheatCategory.Combat,
    displayNameResId = R.string.module_killaura_display_name
) {
    private val range by floatValue("Range", 8f, 2f..16f)
    private val playersOnly by boolValue("Players Only", false)

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
        val yaw = Math.toDegrees(atan2(-dx.toDouble(), dz.toDouble())).toFloat()
        val pitch = Math.toDegrees(-atan2(dy.toDouble(), horizontal.toDouble())).toFloat()
        packet.rotation = Vector3f.from(pitch, yaw, yaw)
    }

    private fun findTarget(): Entity? =
        session.level.entityMap.values
            .filter { it !is LocalPlayer && it.distance(session.localPlayer) <= range }
            .filter { !playersOnly || it is Player }
            .minByOrNull { it.distance(session.localPlayer) }
}
