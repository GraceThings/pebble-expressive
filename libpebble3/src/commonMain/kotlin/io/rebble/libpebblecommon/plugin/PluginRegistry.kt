package io.rebble.libpebblecommon.plugin

import io.rebble.libpebblecommon.WatchConfigFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

/**
 * App-wide registry of available plugins. Built-ins are injected via the Koin `Set<NativePlugin>`
 * binding; JS plugins [register] themselves once their manifest has been loaded.
 */
class PluginRegistry(
    builtIns: Set<NativePlugin>,
    private val watchConfig: WatchConfigFlow,
) {
    private val registered = MutableStateFlow<Map<Uuid, Plugin>>(builtIns.associateBy { it.pluginUuid })

    /** Every lookup goes through here, so disabling plugins hides them all. */
    private val plugins: Collection<Plugin>
        get() = if (watchConfig.value.enablePlugins) registered.value.values else emptyList()

    fun nativePlugins(): Flow<List<PluginMetadata>> =
        registered.map { all -> all.values.filterIsInstance<NativePlugin>().map { it.metadata } }

    fun configMessageTarget(pluginUuid: String): ConfigMessageTarget? =
        findPlugin(pluginUuid) as? ConfigMessageTarget

    fun registerPlugin(plugin: Plugin) {
        registered.update { it + (plugin.pluginUuid to plugin) }
    }

    fun unregisterPlugin(pluginUuid: Uuid): Plugin? {
        val plugin = registered.value[pluginUuid]
        registered.update { it - pluginUuid }
        return plugin
    }

    /**
     * Find a plugin serving the given (category, item) tuple. If [preferredPluginUuid] is set
     * and that plugin serves the tuple, prefer it; otherwise fall back to any plugin that does.
     * Sources are fungible — actions are not, so action lookup uses [findPlugin] by uuid.
     */
    fun findSourcePlugin(
        category: String,
        item: String,
        preferredPluginUuid: String? = null,
    ): Plugin? {
        val all = plugins
        if (preferredPluginUuid != null) {
            all.firstOrNull {
                it.pluginUuid.toString() == preferredPluginUuid && it.serves(category, item)
            }?.let { return it }
        }
        return all.firstOrNull { it.serves(category, item) }
    }

    /** Exact lookup. Actions must never fall back to a different plugin. */
    fun findPlugin(pluginUuid: String): Plugin? =
        plugins.firstOrNull { it.pluginUuid.toString() == pluginUuid }

    fun getAllPlugins(): List<PluginMetadata> = plugins.map { it.metadata }
}
