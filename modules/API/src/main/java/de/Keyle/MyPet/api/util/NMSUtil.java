package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.NotImplemented;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NMSUtil {


	private static Class<?> SoundEffectClazz;
	private static Field SoundEffectB;
	private static Class<?> MinecraftKeyClazz;
	private  static Method MinecraftKeyGetKey;


	static{

		MinecraftKeyClazz = getNMSClass("MinecraftKey");
		try {
			MinecraftKeyGetKey = MinecraftKeyClazz.getDeclaredMethod("getKey");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		SoundEffectClazz = getNMSClass("SoundEffect");
		for(Field f : SoundEffectClazz.getDeclaredFields()){
			if(f.getType().toString().contains("MinecraftKey")){
				f.setAccessible(true);
				SoundEffectB = f;
			}
		}
		if(SoundEffectB == null){

			try {
				throw new Throwable("Unable to find sound effect key field.");
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}

		}
	}
	public static String getSoundEffectId(Object s){
		try {
			return (String) MinecraftKeyGetKey.invoke(SoundEffectB.get(s));
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	private static Class<?> getNMSClass(String name)  {
		String clazz = "net.minecraft.server." + MyPetApi.getCompatUtil().getInternalVersion() + "." + name;
		try {
			return Class.forName(clazz);
		} catch (ClassNotFoundException e) {
			try {
				throw new Throwable("Unable to find class " + clazz + "." );
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
		return null;
	}
}
