package net.zhuruoling.omms.crystal.permission

import cn.hutool.core.io.FileUtil
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import net.zhuruoling.omms.crystal.util.joinFilePaths
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

val defaultPermissionFileContent = """
    {
        "defaultPermissionLevel":"USER",
        "owner":[],
        "admin":[],
        "user":[],
        "guest":[]
    }
""".trimIndent()

data class PermissionStorage(
    @SerializedName("defaultPermissionLevel")
    val defaultPermissionLevel: Permission,
    val owner: ArrayList<String>,
    val admin: ArrayList<String>,
    val user: ArrayList<String>,
    val guest: ArrayList<String>
)

enum class Permission {
    OWNER, ADMIN, USER, GUEST
}

object PermissionManager {
    val gson = GsonBuilder().serializeNulls().create()
    val permissionMap:ConcurrentHashMap<String, Permission> = ConcurrentHashMap<String, Permission>()
    private var defaultPermissionLevel = Permission.USER
    val filePath = joinFilePaths("permissions.json")
    @Synchronized
    fun init() {
        println(filePath)
        if (!FileUtil.exist(filePath)) {
            Files.createFile(Path(filePath))
            FileUtils.write(File(filePath), defaultPermissionFileContent, Charset.defaultCharset())
        }
        val fileContent = FileUtils.readFileToString(File(filePath), Charset.defaultCharset())
        val permissionStorage = gson.fromJson(fileContent, PermissionStorage::class.java)
        permissionStorage.guest.forEach {
            permissionMap[it] = Permission.GUEST
        }
        permissionStorage.user.forEach {
            permissionMap[it] = Permission.USER
        }
        permissionStorage.admin.forEach {
            permissionMap[it] = Permission.ADMIN
        }
        permissionStorage.owner.forEach {
            permissionMap[it] = Permission.OWNER
        }
        defaultPermissionLevel = permissionStorage.defaultPermissionLevel
    }

    @Synchronized
    fun setPermission(player: String, permissionLevel: Permission = defaultPermissionLevel) {
        permissionMap[player] = permissionLevel
    }

    @Synchronized
    fun deletePlayer(player: String) {
        permissionMap.remove(player)
    }

    @Synchronized
    fun forEach(function: (player: String, permission: Permission) -> Unit) {
        permissionMap.forEach { (t, u) -> function(t, u) }
    }

    @Synchronized
    fun writePermission() {
        val defaultPermissionLevel: Permission = defaultPermissionLevel
        val owner: ArrayList<String> = arrayListOf()
        val admin: ArrayList<String> = arrayListOf()
        val user: ArrayList<String> = arrayListOf()
        val guest: ArrayList<String> = arrayListOf()
        permissionMap.forEach { (player, permission) ->
            when (permission) {
                Permission.USER -> {
                    user.add(player)
                }

                Permission.OWNER -> {
                    owner.add(player)
                }

                Permission.ADMIN -> {
                    admin.add(player)
                }

                Permission.GUEST -> {
                    guest.add(player)
                }
            }
        }
        owner.sort()
        user.sort()
        admin.sort()
        guest.sort()
        val permissionStorage = PermissionStorage(defaultPermissionLevel, owner, admin, user, guest)
        val writer = java.io.FileWriter(filePath, false)
        gson.toJson(permissionStorage, writer)
        writer.flush()
        writer.close()
    }

    @Synchronized
    fun getPermission(player: String): Permission {
        return permissionMap[player] ?: defaultPermissionLevel
    }
}