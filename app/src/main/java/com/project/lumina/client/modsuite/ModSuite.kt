package com.project.lumina.client.modsuite

/**
 * Lumina Bedrock-style module registry.
 *
 * This layer deliberately contains no authentication bypass, anti-cheat bypass,
 * or server-protection evasion. It provides the common module lifecycle and
 * configuration surface so transport/game hooks can be wired in safely.
 */
class ModSuite {
    private val modules = linkedMapOf<String, Module>()

    init {
        register("Fly", Category.MOTION)
        register("AirJump", Category.MOTION)
        register("JumpFly", Category.MOTION)
        register("Speed", Category.MOTION)
        register("Jetpack", Category.MOTION)
        register("Glide", Category.MOTION)
        register("NoFall", Category.MOTION)
        register("Velocity", Category.COMBAT)
        register("Freecam", Category.VISUAL)
        register("ESP", Category.VISUAL)
        register("Fullbright", Category.VISUAL)
        register("Scaffold", Category.WORLD)
        register("Nuker", Category.WORLD)
        register("AutoMine", Category.WORLD)
        register("AutoTotem", Category.PLAYER)
        register("AutoArmor", Category.PLAYER)
        register("AutoEat", Category.PLAYER)
        register("ShulkerNesting", Category.INVENTORY)
    }

    fun all(): List<Module> = modules.values.toList()

    fun enabled(): List<Module> = modules.values.filter { it.enabled }

    fun setEnabled(name: String, enabled: Boolean): Boolean {
        val module = modules[name] ?: return false
        module.enabled = enabled
        return true
    }

    fun setSetting(name: String, key: String, value: String): Boolean {
        val module = modules[name] ?: return false
        module.settings[key] = value
        return true
    }

    private fun register(name: String, category: Category) {
        modules[name] = Module(name, category)
    }
}

data class Module(
    val name: String,
    val category: Category,
    var enabled: Boolean = false,
    val settings: MutableMap<String, String> = linkedMapOf()
)

enum class Category {
    COMBAT,
    MOTION,
    PLAYER,
    WORLD,
    VISUAL,
    INVENTORY
}
