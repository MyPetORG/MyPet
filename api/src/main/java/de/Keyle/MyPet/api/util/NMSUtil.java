package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NMSUtil {


    private static Class<?> SoundEffectClazz;
    private static Field SoundEffectB;
    private static Class<?> MinecraftKeyClazz;
    private static Method MinecraftKeyGetKey;


    static {

        MinecraftKeyClazz = getNMSClass("MinecraftKey");
        try {
            MinecraftKeyGetKey = MinecraftKeyClazz.getDeclaredMethod("getKey");
        } catch (NoSuchMethodException e) {
            ErrorUtil.report(e);
        }
        SoundEffectClazz = getNMSClass("SoundEffect");
        for (Field f : SoundEffectClazz.getDeclaredFields()) {
            if (f.getType().toString().contains("MinecraftKey")) {
                f.setAccessible(true);
                SoundEffectB = f;
            }
        }
        if (SoundEffectB == null) {

            try {
                throw new Throwable("Unable to find sound effect key field.");
            } catch (Throwable e) {
                ErrorUtil.report(e);
            }

        }
    }

    public static String getSoundEffectId(Object s) {
        try {
            return (String) MinecraftKeyGetKey.invoke(SoundEffectB.get(s));
        } catch (IllegalAccessException | InvocationTargetException e) {
            ErrorUtil.report(e);
        }
        return null;
    }

    private static Class<?> getNMSClass(String name) {
        String clazz = "net.minecraft.server." + MyPetApi.getCompatUtil().getInternalVersion() + "." + name;
        try {
            return Class.forName(clazz);
        } catch (Throwable e) {
            ErrorUtil.report(e);
        }
        return null;
    }
}
