package net.zhuruoling.omms.crystal.util;

import kotlin.NotImplementedError;
import net.zhuruoling.omms.crystal.event.Event;
import net.zhuruoling.omms.crystal.parser.Parser;
import net.zhuruoling.omms.crystal.plugin.PluginInstance;
import net.zhuruoling.omms.crystal.plugin.PluginMain;
import net.zhuruoling.omms.crystal.plugin.api.annotations.EventListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;

public class PluginUtil {
    public static HashMap<String, Method> getPluginDeclaredEventHandlerMethod(@NotNull Class<? extends PluginMain> clazz) {
        var map = new HashMap<String, Method>();
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            var annotation = declaredMethod.getAnnotation(EventListener.class);
            if (annotation != null) {
                String e = annotation.event();
                map.put(e, declaredMethod);
            }
        }
        return map;
    }

    public static HashMap<String, Parser> getPluginDeclaredParser(@NotNull Class<? extends PluginMain> clazz) {
        throw new NotImplementedError();
    }


}
