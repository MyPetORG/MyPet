package de.Keyle.MyPet.services;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

@ServiceName("CreakingService")
public abstract class CreakingService implements ServiceContainer {

    public abstract Location getCreakingHome(Entity entity);

    public abstract boolean isSupported();
}
