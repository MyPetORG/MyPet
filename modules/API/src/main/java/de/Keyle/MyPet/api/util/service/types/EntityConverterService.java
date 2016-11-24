package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.keyle.knbt.TagCompound;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public abstract class EntityConverterService implements ServiceContainer {
    protected Random random = new Random();

    public abstract TagCompound convertEntity(LivingEntity entity);

    @Override
    public boolean onEnable() {
        return false;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getServiceName() {
        return "EntityConverterService";
    }
}
