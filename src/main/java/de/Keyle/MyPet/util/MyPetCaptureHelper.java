package de.Keyle.MyPet.util;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import org.bukkit.entity.*;

import java.util.List;

public class MyPetCaptureHelper
{
    public static boolean checkTamable(LivingEntity leashTarget, int damage, Player attacker)
    {
        int newHealth = leashTarget.getHealth() - damage;

        boolean tameNow = true;
        List<LeashFlag> leashFlags = MyPet.getLeashFlags(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass());

        flagloop:
        for (LeashFlag flag : leashFlags)
        {
            switch (flag)
            {
                case Impossible:
                    tameNow = false;
                    attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotLeashable")));
                    break flagloop;
                case Adult:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).isAdult())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotAdult")));
                    }
                    break;
                case Baby:
                    if (leashTarget instanceof Ageable && ((Ageable) leashTarget).isAdult())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotBaby")));
                    }
                    break;
                case LowHp:
                    if (newHealth > 2)
                    {
                        tameNow = false;
                    }
                    if (newHealth <= leashTarget.getMaxHealth() && newHealth > 2)
                    {
                        attacker.sendMessage(newHealth + "/" + leashTarget.getMaxHealth() + " " + MyPetLanguage.getString("Name_HP"));
                    }
                    break;
                case Angry:
                    if (leashTarget instanceof Wolf && !((Wolf) leashTarget).isAngry())
                    {
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotAngry")));
                    }
                    break;
                case CanBreed:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).canBreed())
                    {
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CanNotBreed")));
                    }
                    break;
                case None:
                    tameNow = false;
                    break flagloop;
                case Tamed:
                    if (leashTarget instanceof Tameable && !((Tameable) leashTarget).isTamed())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotTamed")));
                    }
                    break;
                case UserCreated:
                    if (leashTarget instanceof IronGolem && !((IronGolem) leashTarget).isPlayerCreated())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotUserCreated")));
                    }
                    break;
                case Wild:
                    if (leashTarget instanceof IronGolem && ((IronGolem) leashTarget).isPlayerCreated())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotWild")));
                    }
                    else if (leashTarget instanceof Tameable && ((Tameable) leashTarget).isTamed())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NotWild")));
                    }
            }
        }
        if (tameNow)
        {
            attacker.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_TameNow")));
        }
        return tameNow;
    }
}