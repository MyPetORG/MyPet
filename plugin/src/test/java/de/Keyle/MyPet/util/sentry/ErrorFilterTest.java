/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.util.sentry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorFilterTest {

    private static StackTraceElement frame(String className, String method) {
        return new StackTraceElement(className, method, className.substring(className.lastIndexOf('.') + 1) + ".java", 1);
    }

    private static <T extends Throwable> T withFrames(T t, StackTraceElement... frames) {
        t.setStackTrace(frames);
        return t;
    }

    // ---- isMyPetError: which plugin owns the error ----

    @Test
    void thirdPartyHookCompiledAgainstOldApiIsNotOurs() {
        // Jobs Reborn's hook references the removed MyPetBukkitEntity class. MyPet is only on the
        // stack as the code that dealt the damage; the throwing frame belongs to Jobs.
        Throwable t = withFrames(new NoClassDefFoundError("de/Keyle/MyPet/api/entity/MyPetBukkitEntity"),
                frame("com.gamingmesh.jobs.hooks.MyPet.MyPetManager", "isMyPet"),
                frame("com.gamingmesh.jobs.listeners.JobsPaymentListener", "onEntityDeath"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"),
                frame("net.minecraft.world.entity.LivingEntity", "die"),
                frame("org.bukkit.craftbukkit.entity.CraftLivingEntity", "damage"),
                frame("de.Keyle.MyPet.entity.ai.attack.PetMeleeAttackGoal", "applyPetDamage"),
                frame("net.minecraft.server.MinecraftServer", "tickServer"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void pluginWhoseNameContainsMyPetIsNotOurs() {
        Throwable t = withFrames(new NullPointerException(),
                frame("java.util.concurrent.ConcurrentHashMap", "putVal"),
                frame("java.util.concurrent.ConcurrentHashMap$KeySetView", "add"),
                frame("cn.xunyuan.mypetgui.FoliaScheduler", "track"),
                frame("cn.xunyuan.mypetgui.MyPetGuiPlugin", "onCommand"),
                frame("org.bukkit.command.PluginCommand", "execute"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void anotherPluginsHookIntoMyPetIsNotOurs() {
        Throwable t = withFrames(new NullPointerException("entity is null"),
                frame("com.bgsoftware.wildstacker.utils.entity.EntityUtils", "isStackable"),
                frame("com.bgsoftware.wildstacker.hooks.MyPet4Hook$1", "lambda$onMyPetSpawn$0"),
                frame("org.bukkit.craftbukkit.scheduler.CraftTask", "run"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void myPetFrameUnderJdkFramesIsOurs() {
        Throwable t = withFrames(new java.util.ConcurrentModificationException(),
                frame("java.util.ArrayList$Itr", "checkForComodification"),
                frame("java.util.ArrayList$Itr", "next"),
                frame("de.Keyle.MyPet.entity.PetImpl", "removePet"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void myPetFrameUnderLibraryMyPetShipsIsOurs() {
        Throwable t = withFrames(new Exception("Apparent connection leak detected"),
                frame("com.zaxxer.hikari.HikariDataSource", "getConnection"),
                frame("de.Keyle.MyPet.repository.types.MySqlRepository", "openIsolatedConnection"),
                frame("de.Keyle.MyPet.migration.MigrationService", "createSqlContext"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void myPetFrameUnderServerFramesIsOurs() {
        Throwable t = withFrames(new IllegalStateException("Stack must be non-empty"),
                frame("de.Keyle.MyPet.entity.PetImpl", "onInteract"),
                frame("net.minecraft.server.network.ServerGamePacketListenerImpl", "handleInteract"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void wrapperThrownByServerWithMyPetCauseIsOurs() {
        Throwable cause = withFrames(new NullPointerException(),
                frame("de.Keyle.MyPet.commands.CommandRelease", "execute"),
                frame("org.bukkit.command.PluginCommand", "execute"));
        Throwable t = withFrames(new RuntimeException("Unhandled exception executing command", cause),
                frame("org.bukkit.command.PluginCommand", "execute"),
                frame("io.papermc.paper.command.brigadier.bukkit.BukkitCommandNode$BukkitBrigCommand", "run"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void wrapperThrownByServerWithThirdPartyCauseIsNotOurs() {
        Throwable cause = withFrames(new NullPointerException(),
                frame("cn.xunyuan.mypetgui.MyPetGui", "open"),
                frame("org.bukkit.command.PluginCommand", "execute"));
        Throwable t = withFrames(new RuntimeException("Unhandled exception executing command", cause),
                frame("org.bukkit.command.PluginCommand", "execute"),
                frame("cn.handyplus.menu.lib.internal.PlayerSchedulerUtil", "executeCommand"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void pluginApiThrowingWhileMyPetCallsItIsOurs() {
        // MyPet's ItemsAdder hook called the ItemsAdder API without checking it was available.
        Throwable t = withFrames(new RuntimeException("Entities API not available"),
                frame("dev.lone.itemsadder.api.CustomEntity", "byAlreadySpawned"),
                frame("dev.lone.itemsadder.internal.EntitiesApi", "check"),
                frame("de.Keyle.MyPet.util.hooks.ItemsAdderHook", "sourceIdOf"),
                frame("de.Keyle.MyPet.util.hooks.ItemsAdderHook", "on"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void pluginThatReachedIntoMyPetFromItsOwnTaskIsNotOurs() {
        Throwable t = withFrames(new IllegalStateException(),
                frame("com.loohp.holomobhealth.utils.MyPetUtils", "isMyPet"),
                frame("com.loohp.holomobhealth.listeners.EntityListener", "onSpawn"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"),
                frame("de.Keyle.MyPet.entity.spawn.VanillaMobSpawner", "spawn"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void pluginCalledThroughAnotherPluginIsNotOurs() {
        Throwable t = withFrames(new IllegalStateException(),
                frame("dev.lone.itemsadder.api.CustomStack", "byItemStack"),
                frame("com.example.shop.ItemLookup", "resolve"),
                frame("de.Keyle.MyPet.util.hooks.ShopHook", "price"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void relocatedBstatsCountsAsLibraryNotAsMyPet() {
        Throwable t = withFrames(new IllegalStateException(),
                frame("de.Keyle.MyPet.util.metrics.bukkit.Metrics", "submitData"),
                frame("org.bukkit.craftbukkit.scheduler.CraftTask", "run"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void errorWithoutAnyMyPetFrameIsNotOurs() {
        Throwable t = withFrames(new IllegalStateException(),
                frame("com.example.other.Thing", "run"));
        assertFalse(ErrorFilter.isMyPetError(t));
        assertFalse(ErrorFilter.touchesMyPet(t));
    }

    @Test
    void touchesMyPetSeesFramesAnywhereInTheChain() {
        Throwable cause = withFrames(new NullPointerException(),
                frame("de.Keyle.MyPet.entity.PetImpl", "tick"));
        Throwable t = withFrames(new RuntimeException(cause),
                frame("net.minecraft.server.MinecraftServer", "tickServer"));
        assertTrue(ErrorFilter.touchesMyPet(t));
    }

    // ---- isIgnored: errors that are nobody's bug, or not MyPet's to fix ----

    @Test
    void linkageErrorSubclassesAreIgnored() {
        assertTrue(ErrorFilter.isIgnored(new NoClassDefFoundError("x")));
        assertTrue(ErrorFilter.isIgnored(new NoSuchMethodError("x")));
        assertTrue(ErrorFilter.isIgnored(new VerifyError("x")));
    }

    @Test
    void linkageErrorAsCauseIsIgnored() {
        assertTrue(ErrorFilter.isIgnored(new RuntimeException("failed to load a class", new NoClassDefFoundError("x"))));
    }

    @Test
    void ioExceptionSubclassesAreIgnored() {
        assertTrue(ErrorFilter.isIgnored(new NoSuchFileException("plugins/MyPet/exp.cache")));
        assertTrue(ErrorFilter.isIgnored(new AccessDeniedException("plugins/MyPet/exp.cache")));
        assertTrue(ErrorFilter.isIgnored(new IOException("x")));
    }

    @Test
    void outOfMemoryIsIgnored() {
        assertTrue(ErrorFilter.isIgnored(new OutOfMemoryError()));
    }

    @Test
    void sqliteHostProblemsAreIgnored() {
        assertTrue(ErrorFilter.isIgnored(new SQLiteException("[SQLITE_READONLY_DBMOVED] The database file has been moved since it was opened")));
        assertTrue(ErrorFilter.isIgnored(new SQLiteException("[SQLITE_IOERR_WRITE] disk I/O error")));
        assertTrue(ErrorFilter.isIgnored(new SQLiteException("[SQLITE_CANTOPEN] unable to open database file")));
        assertTrue(ErrorFilter.isIgnored(new SQLiteException("[SQLITE_BUSY] database is locked")));
    }

    @Test
    void sqliteLogicErrorsAreNotIgnored() {
        assertFalse(ErrorFilter.isIgnored(new SQLiteException("[SQLITE_CONSTRAINT_PRIMARYKEY] UNIQUE constraint failed")));
        assertFalse(ErrorFilter.isIgnored(new SQLiteException("[SQLITE_ERROR] no such table: pets")));
    }

    @Test
    void databaseConnectionProblemsAreIgnored() {
        assertTrue(ErrorFilter.isIgnored(new SQLNonTransientConnectionException("Connection exception, SQL-server rejected establishment of SQL-connection")));
        assertTrue(ErrorFilter.isIgnored(new SQLException("Access denied for user 'mypet'@'localhost'")));
        assertTrue(ErrorFilter.isIgnored(new CommunicationsException("Communications link failure")));
    }

    @Test
    void databaseLogicErrorsAreNotIgnored() {
        assertFalse(ErrorFilter.isIgnored(new SQLException("Duplicate entry '0000' for key 'PRIMARY'")));
        assertFalse(ErrorFilter.isIgnored(new SQLException("Table 'scpetpets' already exists")));
    }

    @Test
    void hotSwappedJarIsIgnored() {
        assertTrue(ErrorFilter.isIgnored(new IllegalStateException("zip file closed")));
        assertTrue(ErrorFilter.isIgnored(new IllegalStateException("The plugin classloader for ItemsAdder has thrown a zip file error.", new IllegalStateException("zip file closed"))));
        assertTrue(ErrorFilter.isIgnored(new IllegalStateException("Cannot get plugin for class de.Keyle.MyPet.MyPetPlugin from a static initializer")));
        assertTrue(ErrorFilter.isIgnored(new IllegalPluginAccessException("Plugin attempted to register task while disabled")));
    }

    @Test
    void ordinaryMyPetBugsAreNotIgnored() {
        assertFalse(ErrorFilter.isIgnored(new NullPointerException()));
        assertFalse(ErrorFilter.isIgnored(new IllegalStateException("Stack must be non-empty")));
        assertFalse(ErrorFilter.isIgnored(new IllegalArgumentException("Health value (2000.0) must be between 0 and 1024.0")));
        assertFalse(ErrorFilter.isIgnored(new Exception("Apparent connection leak detected")));
        assertFalse(ErrorFilter.isIgnored(new RuntimeException("wrapped", new IllegalStateException("Stack must be non-empty"))));
    }

    @Test
    void existingFiltersStillApply() {
        assertTrue(ErrorFilter.isIgnored(new AuthenticationException("x")));
        Throwable adapter = withFrames(new NullPointerException(),
                frame("com.sk89q.worldedit.bukkit.BukkitAdapter", "adapt"),
                frame("de.Keyle.MyPet.util.hooks.WorldGuardHook", "canHurt"));
        assertTrue(ErrorFilter.isIgnored(adapter));
    }

    @Test
    void myPetsOwnNpcStorageCodeIsReported() {
        // The first integration dropped any frame with both "mypet" and "npc" in its name, which
        // also covered MyPet's roster NPC storage menus.
        Throwable t = withFrames(new IllegalStateException("x"),
                frame("de.Keyle.MyPet.gui.menus.NpcStorageConfirmMenuHandler", "onClick"),
                frame("de.Keyle.MyPet.commands.admin.CommandOptionNpc", "execute"));
        assertTrue(ErrorFilter.isMyPetError(t));
        assertFalse(ErrorFilter.isIgnored(t));
    }

    @Test
    void stackOverflowIsReported() {
        assertFalse(ErrorFilter.isIgnored(new StackOverflowError()));
    }

    @Test
    void serverWrapperAroundAnotherPluginsListenerIsNotOurs() {
        // Spigot wraps a listener's exception in an EventException whose own stack points back at
        // the code that fired the event, which is MyPet for MyPet's events. The cause decides.
        Throwable cause = withFrames(new NullPointerException(),
                frame("com.example.quests.listeners.PetListener", "onPetSpawn"),
                frame("org.bukkit.plugin.java.JavaPluginLoader$1", "execute"));
        Throwable t = withFrames(new RuntimeException("EventException", cause),
                frame("org.bukkit.plugin.java.JavaPluginLoader$1", "execute"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"),
                frame("org.bukkit.plugin.SimplePluginManager", "callEvent"),
                frame("de.Keyle.MyPet.entity.PetImpl", "createEntity"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void causeWithoutAStackLeavesTheDecisionToTheWrapper() {
        Throwable cause = withFrames(new IllegalStateException("no stack"));
        Throwable t = withFrames(new RuntimeException(cause),
                frame("de.Keyle.MyPet.entity.PetImpl", "tick"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void pluginsUnderSharedGithubRootAreToldApart() {
        // com.github.a is the owner; com.github.b is a different plugin, not a continuation of it,
        // so MyPet below it is not the direct caller.
        Throwable t = withFrames(new IllegalStateException(),
                frame("com.github.a.Thing", "fail"),
                frame("com.github.b.Bridge", "call"),
                frame("de.Keyle.MyPet.util.hooks.SomeHook", "use"));
        assertFalse(ErrorFilter.isMyPetError(t));
        Throwable direct = withFrames(new IllegalStateException(),
                frame("com.github.a.Thing", "fail"),
                frame("com.github.a.api.Api", "call"),
                frame("de.Keyle.MyPet.util.hooks.SomeHook", "use"));
        assertTrue(ErrorFilter.isMyPetError(direct));
    }

    @Test
    void cyclicCauseChainTerminates() {
        Throwable t = withFrames(new IllegalStateException("x"),
                frame("de.Keyle.MyPet.entity.PetImpl", "tick"));
        Throwable looped = withFrames(new RuntimeException(t), frame("com.example.other.Thing", "run"));
        t.initCause(looped);
        // The chain is t -> looped -> (t again, stop); the innermost distinct cause decides.
        assertFalse(ErrorFilter.isMyPetError(t));
        assertTrue(ErrorFilter.isMyPetError(looped));
        assertTrue(ErrorFilter.touchesMyPet(t));
        assertFalse(ErrorFilter.isIgnored(t));
    }

    // ---- MyPet 3 specifics: more relocated libraries, Bukkit's EventException ----

    @Test
    void relocatedHikariCountsAsLibrary() {
        // MyPet 3 relocates HikariCP under its own package; its frames are library frames, so the
        // MyPet repository that called it owns the error.
        Throwable t = withFrames(new Exception("Apparent connection leak detected"),
                frame("de.Keyle.MyPet.util.hikari.HikariDataSource", "getConnection"),
                frame("de.Keyle.MyPet.repository.types.MySqlRepository$5", "run"),
                frame("org.bukkit.craftbukkit.scheduler.CraftAsyncTask", "run"));
        assertTrue(ErrorFilter.isMyPetError(t));
        Throwable alone = withFrames(new IllegalStateException(),
                frame("de.Keyle.MyPet.util.gson.internal.Excluder", "create"),
                frame("org.bukkit.craftbukkit.scheduler.CraftTask", "run"));
        assertFalse(ErrorFilter.isMyPetError(alone));
    }

    @Test
    void eventExceptionWithAStacklessCauseIsNotOurs() {
        // MYPET-DZ0: another plugin's item-spawn listener threw a NullPointerException without a
        // stack. The EventException's own stack runs down into the MyPet code that spawned the
        // item, which says nothing about whose listener failed.
        Throwable cause = withFrames(new NullPointerException());
        Throwable t = withFrames(new EventException(cause),
                frame("org.bukkit.plugin.EventExecutor$2", "execute"),
                frame("co.aikar.timings.TimedEventExecutor", "execute"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"),
                frame("net.minecraft.server.v1_12_R1.World", "addEntity"),
                frame("de.Keyle.MyPet.compat.v1_12_R1.entity.types.EntityMyPigZombie", "handlePlayerInteraction"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void eventExceptionAroundMyPetsOwnListenerIsOurs() {
        Throwable cause = withFrames(new NullPointerException(),
                frame("de.Keyle.MyPet.listeners.EntityListener", "onEntityDamageByEntityResult"),
                frame("org.bukkit.plugin.java.JavaPluginLoader$1", "execute"));
        Throwable t = withFrames(new EventException(cause),
                frame("org.bukkit.plugin.java.JavaPluginLoader$1", "execute"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"));
        assertTrue(ErrorFilter.isMyPetError(t));
    }

    @Test
    void otherPluginsListenerOnAMyPetFiredEventIsNotOurs() {
        // MYPET-DSM: a Pet's damage killed a player and DeathMessages' death listener threw.
        Throwable cause = withFrames(new NullPointerException(),
                frame("net.joshb.deathmessages.listener.EntityDeath", "onEntityDeath"),
                frame("co.aikar.timings.TimedEventExecutor", "execute"),
                frame("org.bukkit.plugin.RegisteredListener", "callEvent"),
                frame("net.minecraft.server.v1_12_R1.EntityPlayer", "die"),
                frame("de.Keyle.MyPet.skill.skills.LightningImpl", "lambda$apply$0"));
        Throwable t = withFrames(new EventException(cause),
                frame("org.bukkit.plugin.EventExecutor$2", "execute"),
                frame("de.Keyle.MyPet.listeners.MyPetEntityListener", "onMyPetDamageMonitor"));
        assertFalse(ErrorFilter.isMyPetError(t));
    }

    @Test
    void outOfMemoryInsideACommandExceptionIsIgnored() {
        // MYPET-E7D: the server ran out of Metaspace while /petrelease spawned a mob.
        Throwable t = new RuntimeException("Unhandled exception executing command 'petrelease'",
                new OutOfMemoryError("Metaspace"));
        assertTrue(ErrorFilter.isIgnored(t));
    }

    @Test
    void mysqlNotAllowedToConnectIsIgnored() {
        assertTrue(ErrorFilter.isIgnored(new SQLException("Host '10.0.0.5' is not allowed to connect to this MySQL server")));
    }

    // Stand-ins for driver/server classes the filter matches by simple name, so the test needs
    // neither the SQLite driver, the MySQL driver nor Bukkit on its classpath.
    private static class SQLiteException extends SQLException {
        SQLiteException(String message) {
            super(message);
        }
    }

    private static class CommunicationsException extends SQLException {
        CommunicationsException(String message) {
            super(message);
        }
    }

    private static class IllegalPluginAccessException extends RuntimeException {
        IllegalPluginAccessException(String message) {
            super(message);
        }
    }

    private static class EventException extends Exception {
        EventException(Throwable cause) {
            super(cause);
        }
    }

    private static class AuthenticationException extends Exception {
        AuthenticationException(String message) {
            super(message);
        }
    }
}
