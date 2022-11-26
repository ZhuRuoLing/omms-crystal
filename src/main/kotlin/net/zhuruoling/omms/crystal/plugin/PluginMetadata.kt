package net.zhuruoling.omms.crystal.plugin

import java.lang.module.ModuleDescriptor

class PluginMetadata {
    var id: String? = null
    var version: ModuleDescriptor.Version? = null
    var author: List<String>? = null
    var pluginDependencies: PluginDependency? = null

    constructor() {}
    constructor(id: String?, version: String?, author: List<String>?) {
        this.author = author
        this.id = id
        this.version = ModuleDescriptor.Version.parse(version)
        pluginDependencies = null
    }

    constructor(id: String?, version: String?, author: String) {
        this.author = listOf(author)
        this.id = id
        this.version = ModuleDescriptor.Version.parse(version)
        pluginDependencies = null
    }

    constructor(id: String?, version: String?, author: List<String>?, pluginDependencies: PluginDependency?) {
        this.id = id
        this.version = ModuleDescriptor.Version.parse(version)
        this.author = author
        this.pluginDependencies = pluginDependencies
    }

    constructor(id: String?, version: String?, author: String, pluginDependencies: PluginDependency?) {
        this.id = id
        this.version = ModuleDescriptor.Version.parse(version)
        this.author = listOf(author)
        this.pluginDependencies = pluginDependencies
    }

    constructor(id: String?, version: String?) {
        this.id = id
        this.version = ModuleDescriptor.Version.parse(version)
    }

    constructor(id: String?, version: ModuleDescriptor.Version?, author: List<String>?) {
        this.author = author
        this.id = id
        this.version = version
        pluginDependencies = null
    }

    constructor(id: String?, version: ModuleDescriptor.Version?, author: String) {
        this.author = listOf(author)
        this.id = id
        this.version = version
        pluginDependencies = null
    }

    constructor(
        id: String?,
        version: ModuleDescriptor.Version?,
        author: List<String>?,
        pluginDependencies: PluginDependency?
    ) {
        this.id = id
        this.version = version
        this.author = author
        this.pluginDependencies = pluginDependencies
    }

    constructor(
        id: String?,
        version: ModuleDescriptor.Version?,
        author: String,
        pluginDependencies: PluginDependency?
    ) {
        this.id = id
        this.version = version
        this.author = listOf(author)
        this.pluginDependencies = pluginDependencies
    }

    constructor(id: String?, version: ModuleDescriptor.Version?) {
        this.id = id
        this.version = version
    }

    override fun toString(): String {
        return "PluginMetadata{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", author=" + author +
                ", pluginDependencies=" + pluginDependencies +
                '}'
    }
}