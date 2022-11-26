package net.zhuruoling.omms.crystal.plugin

import java.lang.module.ModuleDescriptor

class PluginDependency {
    private var dependencies: List<Dependency> = ArrayList()
    override fun toString(): String {
        return "PluginDependency{" +
                "dependencies=" + dependencies +
                '}'
    }

    enum class Operator {
        EQUAL, GREATER, LESS
    }

    abstract class Dependency {
        protected abstract val id: String
        protected abstract val operator: Operator
        protected abstract val version: ModuleDescriptor.Version


        companion object {
            fun of(id: String, operator: Operator, version: ModuleDescriptor.Version): Dependency {
                return object : Dependency() {
                    override val id: String
                        get() = id
                    override val operator: Operator
                        get() = operator
                    override val version: ModuleDescriptor.Version
                        get() = version
                }
            }

            fun of(id: String, operator: Operator, version: String?): Dependency {
                return object : Dependency() {
                    override val id: String
                        get() = id
                    override val operator: Operator
                        get() = operator
                    override val version: ModuleDescriptor.Version
                        get() = ModuleDescriptor.Version.parse(version)
                }
            }
        }

        override fun toString(): String {
            return "Dependency(id='$id', operator=$operator, version=$version)"
        }
    }
}