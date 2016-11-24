package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.keyle.knbt.TagCompound;
import org.bukkit.entity.LivingEntity;

public abstract class EntityConverterService implements ServiceContainer {
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
