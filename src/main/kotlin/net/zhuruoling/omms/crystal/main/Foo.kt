package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.permission.PermissionManager


fun main() {
    PermissionManager.init()
    PermissionManager.setPermission("wdnmd")
    PermissionManager.setPermission("114514", Permission.ADMIN)
    PermissionManager.writePermission()
    println(PermissionManager.getPermission("wdnmd"))
    println(PermissionManager.getPermission("114514"))
    PermissionManager.setPermission("114514", Permission.USER)
    println(PermissionManager.getPermission("114514"))
    PermissionManager.forEach { player, _ -> PermissionManager.deletePlayer(player) }
    PermissionManager.writePermission()
}