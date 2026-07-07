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

package de.Keyle.MyPet.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import de.Keyle.MyPet.entity.types.ModelPet;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.NamespacedKey;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.translatable;

/**
 * Brigadier argument that resolves a {@link PetType} from a namespaced key.
 *
 * <p>Vanilla pet types are addressed under the {@code minecraft:} namespace
 * (e.g. {@code minecraft:wolf}, {@code minecraft:snow_golem}); data-driven
 * custom creatures use the {@code mypet:} namespace (e.g.
 * {@code mypet:frostdragon}).</p>
 *
 * <p>The native type is {@link ArgumentTypes#namespacedKey()} rather than a
 * registry-resource argument. A registry-resource argument is bound to the real
 * entity-type registry and therefore cannot represent {@code mypet:} ids (which
 * are not entity types); a plain word argument cannot contain a colon. A
 * namespaced-key argument accepts any {@code namespace:value} resource location,
 * so both namespaces parse — and the namespace is mandatory, so a bare name does
 * not silently resolve.</p>
 */
public final class PetTypeArgument implements CustomArgumentType.Converted<PetType, NamespacedKey> {

    /** Thrown when the key is well-formed but does not name a usable pet type in the given namespace. */
    private static final ComponentCommandExceptionType ERROR_INVALID =
            new ComponentCommandExceptionType(translatable("argument.id.invalid"));

    @Override
    public ArgumentType<NamespacedKey> getNativeType() {
        return ArgumentTypes.namespacedKey();
    }

    @Override
    public PetType convert(NamespacedKey key) throws CommandSyntaxException {
        if ("minecraft".equals(key.getNamespace())) {
            try {
                PetType type = PetType.byEntityTypeName(key.getKey());
                // Vanilla types only under minecraft:; a custom creature's derived bukkit name
                // could otherwise be reached here. Version-gated types are rejected like the
                // entity registry would (they aren't available on this Minecraft version).
                if (type.getPetClass() != ModelPet.class && type.checkMinecraftVersion()) {
                    return type;
                }
            } catch (PetTypeNotFoundException ignored) {
            }
            throw ERROR_INVALID.create();
        }
        if ("mypet".equals(key.getNamespace())) {
            PetType type = PetType.byNameOrNull(key.getKey());
            if (type != null && type.getPetClass() == ModelPet.class) {
                return type;
            }
        }
        throw ERROR_INVALID.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        boolean hasColon = remaining.indexOf(':') >= 0;
        for (PetType type : PetType.values()) {
            if (!type.checkMinecraftVersion()) {
                continue;
            }
            boolean custom = type.getPetClass() == ModelPet.class;
            String full = custom
                    ? "mypet:" + type.name().toLowerCase(Locale.ROOT)
                    : "minecraft:" + type.getBukkitName().toLowerCase(Locale.ROOT);
            // With a colon present, match the whole namespaced key. Without one, also match the
            // value portion so typing "fros" surfaces "mypet:frostdragon" — but selecting it still
            // inserts the full namespaced form, so the namespace is always present in the result.
            String value = full.substring(full.indexOf(':') + 1);
            boolean match = hasColon
                    ? RegistryArgumentType.matchesSubStr(remaining, full)
                    : RegistryArgumentType.matchesSubStr(remaining, full)
                            || RegistryArgumentType.matchesSubStr(remaining, value);
            if (match) {
                builder.suggest(full);
            }
        }
        return builder.buildFuture();
    }
}
