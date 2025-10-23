package de.Keyle.MyPet.entity.leashing;

import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.util.configuration.settings.Setting;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@LeashFlagName("Permission")
public class PermissionFlag implements LeashFlag {

    @Override
    public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
        for (Setting setting : settings.all()) {
            if (!player.hasPermission(setting.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        for (Setting setting : settings.all()) {
            if (!player.hasPermission(setting.getKey())) {
                return Translation.getString("Message.Command.CaptureHelper.Requirement.Permission", player);
            }
        }
        return Translation.getString("Message.Command.CaptureHelper.Requirement.Permission", player);
    }
}
