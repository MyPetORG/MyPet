package de.Keyle.MyPet.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Material;

@ServiceName("EggIconService")
public class EggIconService implements ServiceContainer {

    protected static String toUpperSnake(String in) {
        return in.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    public void updateIcon(MyPetType type, IconMenuItem icon) {
        icon.setGlowing(false);

        String upperSnake = toUpperSnake(type.name());
        String matName = upperSnake + "_SPAWN_EGG";

        Material material = Material.matchMaterial(matName);

        if (material == null) {
            switch (type.name()) {
                case "EnderDragon":
                    material = Material.DRAGON_EGG;
                    break;
                case "SnowGolem":
                    material = Material.PUMPKIN;
                    break;
                case "Giant":
                    material = Material.ZOMBIE_SPAWN_EGG;
                    break;
                case "Illusioner":
                    material = Material.SQUID_SPAWN_EGG;
                    icon.setGlowing(true);
                    break;
                case "IronGolem":
                    material = Material.SKELETON_SPAWN_EGG;
                    icon.setGlowing(true);
                    break;
                case "Wither":
                    material = Material.ENDERMITE_SPAWN_EGG;
                    icon.setGlowing(true);
                    break;
                default:
                    material = Material.BARRIER;
                    icon.setGlowing(true);
                    break;
            }
        }

        icon.setMaterial(material);
    }
}
