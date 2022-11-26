package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.event.EventDispatcher
import net.zhuruoling.omms.crystal.event.EventLoop
import net.zhuruoling.omms.crystal.server.ServerHandler

object SharedConstants {
   var serverHandler: ServerHandler? = null
   lateinit var eventDispatcher: EventDispatcher
   lateinit var eventLoop: EventLoop
   var needExit = true
}