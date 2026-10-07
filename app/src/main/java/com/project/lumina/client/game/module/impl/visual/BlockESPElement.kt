package com.project.lumina.client.game.module.impl.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.project.lumina.client.R
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.world.chunk.Chunk
import com.project.lumina.client.render.ESPRenderOverlayView
import com.project.lumina.client.game.module.api.setting.stringValue
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.cos
import kotlin.math.sin

open class BlockESPElement(
    private val defaultMode: String = "Ores",
    moduleName: String = "blockesp"
) : Element(
    name = moduleName,
    category = CheatCategory.Visual,
    displayNameResId = R.string.esp_module_name
) {
    companion object {
        private var renderView: ESPRenderOverlayView? = null
        fun setRenderView(view: ESPRenderOverlayView?) { renderView = view }
    }

    private val mode by stringValue("Mode", defaultMode, listOf("Ores", "Storage", "Spawners", "Xray"))
    private val range by intValue("Range", 12, 4..24)
    private val maxBlocks by intValue("Max Blocks", 180, 20..500)

    private val cachedBlocks = ArrayList<Vector3f>()
    private var lastScan = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return
        if (interceptablePacket.packet is PlayerAuthInputPacket) {
            val now = System.currentTimeMillis()
            if (now - lastScan >= 500L) {
                lastScan = now
                scanBlocks()
            }
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated && renderView == null) {
            renderView = ESPRenderOverlayView.createAndShow()
            ESPElement.setRenderView(renderView!!)
            renderView?.postInvalidate()
        }
        scanBlocks()
    }

    override fun onDisabled() {
        super.onDisabled()
        synchronized(cachedBlocks) { cachedBlocks.clear() }
        if (!ESPElementActive()) {
            renderView?.let { ESPRenderOverlayView.dismissOverlay(it) }
            renderView = null
        }
    }

    private fun ESPElementActive(): Boolean =
        GameManager.elements.any { it is ESPElement && it.isEnabled && it.isSessionCreated }

    private fun scanBlocks() {
        if (!isSessionCreated || !session::blockMapping.isInitialized) return

        val p = session.localPlayer.vec3Position
        val centerX = p.x.toInt()
        val centerY = p.y.toInt()
        val centerZ = p.z.toInt()
        val found = ArrayList<Vector3f>()

        val minY = (centerY - range).coerceAtLeast(0)
        val maxY = centerY + range

        loop@ for (x in centerX - range..centerX + range) {
            for (z in centerZ - range..centerZ + range) {
                for (y in minY..maxY) {
                    val runtimeId = session.world.getBlockId(x, y, z)
                    if (runtimeId == session.blockMapping.airId) continue
                    val id = session.blockMapping.getDefinition(runtimeId).identifier
                    if (matches(id)) {
                        found.add(Vector3f.from(x + 0.5f, y + 0.5f, z + 0.5f))
                        if (found.size >= maxBlocks) break@loop
                    }
                }
            }
        }

        synchronized(cachedBlocks) {
            cachedBlocks.clear()
            cachedBlocks.addAll(found)
        }
    }

    private fun matches(identifier: String): Boolean {
        val id = identifier.lowercase()
        return when (mode.lowercase()) {
            "ores" -> id.contains("_ore") || id.contains("deepslate_ore") ||
                    id == "minecraft:ancient_debris"
            "storage" -> id.contains("chest") || id.contains("barrel") ||
                    id.contains("shulker_box") || id.contains("ender_chest")
            "spawners" -> id.contains("mob_spawner") || id.contains("monster_egg")
            "xray" -> id.contains("_ore") || id.contains("ancient_debris") ||
                    id.contains("spawner") || id.contains("chest") ||
                    id.contains("barrel") || id.contains("shulker_box") ||
                    id.contains("ender_chest")
            else -> false
        }
    }

    fun render(canvas: Canvas) {
        if (!isEnabled || !isSessionCreated) return
        val player = session.localPlayer
        val width = canvas.width
        val height = canvas.height
        if (width <= 0 || height <= 0) return

        val viewProj = Matrix4f.createPerspective(110f, width.toFloat() / height, 0.1f, 128f)
            .mul(Matrix4f.createTranslation(player.vec3Position)
                .mul(rotateY(-player.rotationYaw - 180f))
                .mul(rotateX(-player.rotationPitch))
                .invert())

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = when (mode.lowercase()) {
                "storage" -> Color.YELLOW
                "spawners" -> Color.MAGENTA
                "xray" -> Color.CYAN
                else -> Color.GREEN
            }
            isAntiAlias = true
        }

        val snapshot = synchronized(cachedBlocks) { cachedBlocks.toList() }
        snapshot.forEach { pos ->
            val top = worldToScreen(Vector3f.from(pos.x, pos.y + .5f, pos.z), viewProj, width, height)
            val bottom = worldToScreen(Vector3f.from(pos.x, pos.y - .5f, pos.z), viewProj, width, height)
            if (top != null && bottom != null) {
                val size = (bottom.y - top.y).toFloat().coerceIn(4f, 80f)
                val left = top.x - size / 2f
                val right = top.x + size / 2f
                canvas.drawRect(left, top.y, right, bottom.y, paint)
            }
        }
    }

    private fun rotateX(angle: Float): Matrix4f {
        val r = Math.toRadians(angle.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return Matrix4f.from(1f,0f,0f,0f, 0f,c,-s,0f, 0f,s,c,0f, 0f,0f,0f,1f)
    }

    private fun rotateY(angle: Float): Matrix4f {
        val r = Math.toRadians(angle.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return Matrix4f.from(c,0f,s,0f, 0f,1f,0f,0f, -s,0f,c,0f, 0f,0f,0f,1f)
    }

    private fun worldToScreen(pos: Vector3f, m: Matrix4f, w: Int, h: Int): Vector2f? {
        val clipW = m.get(3,0)*pos.x + m.get(3,1)*pos.y + m.get(3,2)*pos.z + m.get(3,3)
        if (clipW <= 0.01f) return null
        val inv = 1f / clipW
        val sx = w/2f + .5f * (m.get(0,0)*pos.x + m.get(0,1)*pos.y + m.get(0,2)*pos.z + m.get(0,3))*inv*w
        val sy = h/2f - .5f * (m.get(1,0)*pos.x + m.get(1,1)*pos.y + m.get(1,2)*pos.z + m.get(1,3))*inv*h
        return Vector2f.from(sx, sy)
    }
}
