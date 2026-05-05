/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.service;

import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.ErrorUtil;

import java.util.*;

/**
 * The {@link ServiceManager} manages all interactions with other plugins. Services are stored by class and by the
 * interfaces they implement so they can also be retrieved by them. You can get instances of other plugins and check if
 * other plugins are active.
 */
public class ServiceManager {
    final Map<Class<? extends ServiceContainer>, ServiceContainer> services = new HashMap<>();
    final Map<String, ServiceContainer> serviceByName = new HashMap<>();

    final ArrayListMultimap<Load.State, ServiceContainer> registeredServices = ArrayListMultimap.create();

    public void listServices() {
        MyPetApi.getLogger().info("Loaded services: " + serviceByName.keySet());
    }

    /**
     * register new services here. A service needs the {@link ServiceName} annotation to be accepted.
     *
     * @param serviceClass the service class
     */
    public void registerService(Class<? extends ServiceContainer> serviceClass) {
        Load.State loadingState = Load.State.OnEnable;
        if (serviceClass.isAnnotationPresent(Load.class)) {
            loadingState = serviceClass.getAnnotation(Load.class).value();
        }
        try {
            ServiceContainer service = serviceClass.getDeclaredConstructor().newInstance();
            registeredServices.put(loadingState, service);
        } catch (Throwable e) {
            ErrorUtil.report("Error occured while creating the " + serviceClass.getName() + " service.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerService(ServiceContainer service) {
        boolean genericService = true;
        for (Class<?> iface : Util.getAllInterfaces(service.getClass())) {
            if (iface != ServiceContainer.class && ServiceContainer.class.isAssignableFrom(iface)) {
                services.put((Class<? extends ServiceContainer>) iface, service);
                genericService = false;
            }
        }
        for (Class<?> superclass : Util.getAllSuperclasses(service.getClass())) {
            if (superclass != ServiceContainer.class && ServiceContainer.class.isAssignableFrom(superclass)) {
                services.put((Class<? extends ServiceContainer>) superclass, service);
                genericService = false;
            }
        }
        if (genericService) {
            services.put(ServiceContainer.class, service);
        }
        serviceByName.put(service.getServiceName(), service);
        services.put(service.getClass(), service);
    }

    public void activate(Load.State state) {
        List<ServiceContainer> services = registeredServices.get(state);

        for (ServiceContainer service : services) {
            if (service.onEnable()) {
                registerService(service);
            }
        }

        registeredServices.removeAll(state);
    }

    public void disableServices() {
        for (ServiceContainer service : serviceByName.values()) {
            service.onDisable();
        }
    }

    /**
     * returns all services that inherit from a specific class/interface
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return list of instances of the service class
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceContainer> List<T> getServices(Class<? extends T> serviceClass) {
        List<T> list = new ArrayList<>();

        for (ServiceContainer service : services.values()) {
            if (serviceClass.isInstance(service)) {
                T typed = (T) service;
                if (!list.contains(typed)) {
                    list.add(typed);
                }
            }
        }

        return list;
    }

    /**
     * returns if services that inherit from a specific class/interface are available
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return if any service was found
     */
    public boolean hasServices(Class<? extends ServiceContainer> serviceClass) {
        return services.containsKey(serviceClass);
    }

    /**
     * returns the services of a specific class
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return instance of the service class
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceContainer> Optional<T> getService(Class<? extends T> serviceClass) {
        return Optional.ofNullable((T) services.get(serviceClass));
    }

    /**
     * returns the services with a specific {@link ServiceName}
     *
     * @param name name of the plugin
     * @return instance of a service class associated with the plugin name
     */
    public Optional<ServiceContainer> getService(String name) {
        return Optional.ofNullable(serviceByName.get(name));
    }

    /**
     * returns if a services with a specific {@link ServiceName} is available
     *
     * @param name name of the plugin
     * @return if any service was found
     */
    public boolean isServiceActive(String name) {
        return serviceByName.containsKey(name);
    }

    /**
     * returns if a service that inherit from a specific class/interface is available
     *
     * @param serviceClass class that implements from {@link ServiceContainer}
     * @return if any service was found
     */
    public boolean isServiceActive(Class<? extends ServiceContainer> serviceClass) {
        return services.containsKey(serviceClass);
    }
}