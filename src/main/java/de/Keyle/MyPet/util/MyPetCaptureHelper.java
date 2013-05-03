package de.Keyle.MyPet.util;


import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class MyPetCaptureHelper
{
    public static enum CaptureHelperMode
    {
        Deactivated, Normal, Half, TameableOnly;
    }

    public static void checkTamable(LivingEntity livingEntity, int damage, Player attacker)
    {
        int newHealth = livingEntity.getHealth() - damage;

        switch (MyPetPlayer.getMyPetPlayer(attacker).getCaptureHelperMode())
        {
            case Deactivated:
                return;
            case Normal:
                if (newHealth > 2)
                {
                    attacker.sendMessage(newHealth + "/" + livingEntity.getMaxHealth() + " " + MyPetLanguage.getString("Name_HP"));
                }
                else
                {
                    attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_TameNow")));
                }
                break;
            case Half:
                if (newHealth <= 2)
                {
                    attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_TameNow")));
                }
                else if (newHealth <= livingEntity.getMaxHealth() * 0.5)
                {
                    attacker.sendMessage(newHealth + "/" + livingEntity.getMaxHealth() + " " + MyPetLanguage.getString("Name_HP"));
                }
                break;
            case TameableOnly:
                if (newHealth <= 2)
                {
                    attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_TameNow")));
                }
                break;
        }
    }
}