package de.Keyle.MyPet.util;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.entity.*;

import java.util.List;

public class MyPetCaptureHelper
{
    public static boolean checkTamable(LivingEntity leashTarget, double damage, Player attacker)
    {
        double newHealth = leashTarget.getHealth() - damage;

        boolean tameNow = true;
        List<LeashFlag> leashFlags = MyPet.getLeashFlags(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass());

        flagloop:
        for (LeashFlag flag : leashFlags)
        {
            switch (flag)
            {
                case Impossible:
                    tameNow = false;
                    attacker.sendMessage(MyPetLocales.getString("Message.NotLeashable", attacker));
                    break flagloop;
                case Adult:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).isAdult())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetLocales.getString("Message.NotAdult", attacker));
                    }
                    break;
                case Baby:
                    if (leashTarget instanceof Ageable && ((Ageable) leashTarget).isAdult())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetLocales.getString("Message.NotBaby", attacker));
                    }
                    break;
                case LowHp:
                    if (newHealth > 2)
                    {
                        tameNow = false;
                    }
                    if (newHealth <= leashTarget.getMaxHealth() && newHealth > 2)
                    {
                        attacker.sendMessage(String.format("%1.2f", newHealth) + "/" + String.format("%1.2f", leashTarget.getMaxHealth()) + " " + MyPetLocales.getString("Name.HP", attacker));
                    }
                    break;
                case Angry:
                    if (leashTarget instanceof Wolf && !((Wolf) leashTarget).isAngry())
                    {
                        attacker.sendMessage(MyPetLocales.getString("Message.NotAngry", attacker));
                    }
                    break;
                case CanBreed:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).canBreed())
                    {
                        attacker.sendMessage(MyPetLocales.getString("Message.CanNotBreed", attacker));
                    }
                    break;
                case None:
                    tameNow = false;
                    break flagloop;
                case Tamed:
                    if (leashTarget instanceof Tameable && !((Tameable) leashTarget).isTamed())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetLocales.getString("Message.NotTamed", attacker));
                    }
                    break;
                case UserCreated:
                    if (leashTarget instanceof IronGolem && !((IronGolem) leashTarget).isPlayerCreated())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetLocales.getString("Message.NotUserCreated", attacker));
                    }
                    break;
                case Wild:
                    if (leashTarget instanceof IronGolem && ((IronGolem) leashTarget).isPlayerCreated())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetLocales.getString("Message.NotWild", attacker));
                    }
                    else if (leashTarget instanceof Tameable && ((Tameable) leashTarget).isTamed())
                    {
                        tameNow = false;
                        attacker.sendMessage(MyPetLocales.getString("Message.NotWild", attacker));
                    }
            }
        }
        if (tameNow)
        {
            attacker.sendMessage(MyPetLocales.getString("Message.TameNow", attacker));
        }
        return tameNow;
    }
}