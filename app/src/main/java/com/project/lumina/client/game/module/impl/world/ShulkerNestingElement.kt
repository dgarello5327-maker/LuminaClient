package com.project.lumina.client.game.module.impl.world
import android.util.Log
import com.project.lumina.client.R
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.InterceptablePacket
import org.cloudburstmc.nbt.NbtType
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class ShulkerNestingElement : Element(
    name = "ShulkerNesting",
    category = CheatCategory.World,
    displayNameResId = R.string.module_invhelper_display_name
) {
    private val maxDepth by intValue("Max Depth", 8, 1..32)
    private val scanDelay by intValue("Scan Delay", 250, 50..2000)
    private val logContents by boolValue("Log Contents", false)
    private var lastScan = 0L
    private var nestedShulkers = 0
    private var deepestLevel = 0

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket && packet !is InventoryContentPacket && packet !is InventorySlotPacket) return
        val now = System.currentTimeMillis()
        if (now - lastScan < scanDelay) return
        lastScan = now
        scanInventory()
    }

    private fun scanInventory() {
        val inventory = session.localPlayer.inventory
        var total = 0
        var deepest = 0
        inventory.content.forEachIndexed { slot, item ->
            if (item == ItemData.AIR || !item.isValid) return@forEachIndexed
            val identifier = item.definition?.identifier?.lowercase() ?: return@forEachIndexed
            if (!identifier.contains("shulker_box")) return@forEachIndexed
            val result = inspectItem(item, 0)
            total += result.count
            deepest = maxOf(deepest, result.depth)
            if (logContents && result.count > 0) Log.d("ShulkerNesting", "slot=" + slot + " nested=" + result.count + " depth=" + result.depth)
        }
        nestedShulkers = total
        deepestLevel = deepest
    }

    private data class ScanResult(val count: Int, val depth: Int)

    private fun inspectItem(item: ItemData, depth: Int): ScanResult {
        if (depth >= maxDepth) return ScanResult(0, depth)
        val tag = item.tag ?: return ScanResult(0, depth)
        val items = findItemsList(tag) ?: return ScanResult(0, depth)
        var count = 0
        var deepest = depth
        items.forEach { entry ->
            val identifier = entry.getString("Name", "").ifEmpty { entry.getString("name", "") }.lowercase()
            if (identifier.contains("shulker_box")) {
                count++
                deepest = maxOf(deepest, depth + 1)
                val nestedTag = entry.getCompound("tag")
                val nestedItems = nestedTag?.let { findItemsList(it) }
                nestedItems?.forEach { nestedEntry ->
                    val nestedName = nestedEntry.getString("Name", "").ifEmpty { nestedEntry.getString("name", "") }.lowercase()
                    if (nestedName.contains("shulker_box")) {
                        count++
                        deepest = maxOf(deepest, depth + 2)
                    }
                }
            }
        }
        return ScanResult(count, deepest)
    }

    private fun findItemsList(tag: org.cloudburstmc.nbt.NbtMap) =
        tag.getList("Items", NbtType.COMPOUND) ?: tag.getCompound("BlockEntityTag")?.getList("Items", NbtType.COMPOUND)

    fun getNestedShulkerCount(): Int = nestedShulkers
    fun getDeepestNestingLevel(): Int = deepestLevel

    override fun onDisabled() {
        super.onDisabled()
        nestedShulkers = 0
        deepestLevel = 0
    }
}