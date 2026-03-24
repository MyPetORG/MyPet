package de.Keyle.MyPet.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static net.kyori.adventure.text.Component.translatable;

/**
 * A Brigadier {@link CustomArgumentType} that resolves values from a Paper
 * {@link RegistryKey registry} and optionally filters them with a {@link Predicate}.
 *
 * <p>This argument type wraps Paper's native {@code resource} argument so the
 * client receives proper completions, while the server-side conversion step
 * rejects any value that does not pass the supplied filter. When no filter is
 * needed, the {@link #all(RegistryKey)} factory can be used instead.</p>
 *
 * <p>Tab-completion uses the same underscore-segmented substring matching
 * algorithm that vanilla Minecraft employs for resource-location arguments
 * (see {@link #matchesSubStr(String, String)}).</p>
 *
 * @param <T> the registry element type; must implement both Adventure's
 *            {@link Keyed} and Bukkit's {@link org.bukkit.Keyed} so it can
 *            be looked up by {@link Key} and used with Paper's registry API
 */
@SuppressWarnings("UnstableApiUsage")
public final class RegistryArgumentType<T extends Keyed & org.bukkit.Keyed>
        implements CustomArgumentType.Converted<T, T> {

    /** Shared exception thrown when the player provides a key that exists in the registry but is rejected by the filter. */
    private static final ComponentCommandExceptionType ERROR_INVALID =
            new ComponentCommandExceptionType(translatable("argument.id.invalid"));

    /** The Paper registry key that identifies which registry to query (e.g. {@code RegistryKey.ENTITY_TYPE}). */
    private final RegistryKey<T> registryKey;

    /** A predicate applied during both suggestion building and conversion to restrict which entries are valid. */
    private final Predicate<T> filter;

    /** Pre-computed list of {@link Key}s that pass the filter, used for tab-completion. */
    private final List<Key> suggestions;

    /**
     * Constructs a new registry argument, eagerly computing the filtered
     * suggestion list from the current state of the registry.
     *
     * @param registryKey the Paper registry to draw values from
     * @param filter      a predicate that must return {@code true} for an
     *                    entry to appear in suggestions and be accepted by
     *                    {@link #convert(Keyed)}
     */
    private RegistryArgumentType(RegistryKey<T> registryKey, Predicate<T> filter) {
        this.registryKey = registryKey;
        this.filter = filter;

        var registry = RegistryAccess.registryAccess().getRegistry(registryKey);
        this.suggestions = registry.stream()
                .filter(filter)
                .map(Keyed::key)
                .toList();
    }

    /**
     * Creates a registry argument that only accepts entries matching the given filter.
     *
     * <p>Example usage for entity types that MyPet supports:</p>
     * <pre>{@code
     * RegistryArgumentType.of(RegistryKey.ENTITY_TYPE, MyPetType::isSupported)
     * }</pre>
     *
     * @param <T>    the registry element type
     * @param key    the Paper registry key identifying the registry
     * @param filter a predicate to restrict which entries are valid
     * @return a new {@code RegistryArgumentType} with the filter applied
     */
    public static <T extends Keyed & org.bukkit.Keyed> RegistryArgumentType<T> of(RegistryKey<T> key, Predicate<T> filter) {
        return new RegistryArgumentType<>(key, filter);
    }

    /**
     * Creates a registry argument that accepts <em>all</em> entries in the registry
     * without any filtering.
     *
     * <p>This is a convenience shorthand for {@code of(key, t -> true)}.</p>
     *
     * @param <T> the registry element type
     * @param key the Paper registry key identifying the registry
     * @return a new {@code RegistryArgumentType} with no filter
     */
    public static <T extends Keyed & org.bukkit.Keyed> RegistryArgumentType<T> all(RegistryKey<T> key) {
        return new RegistryArgumentType<>(key, t -> true);
    }

    /**
     * Converts the value resolved by the native argument type, rejecting it
     * with a translatable error message if the filter does not accept it.
     *
     * <p>Because the native type and the converted type are the same
     * ({@code T}), this method acts purely as a validation gate rather than
     * a type transformation.</p>
     *
     * @param nativeType the value resolved from the player's input by the
     *                   native {@code resource} argument type
     * @return the same value, unchanged, if it passes the filter
     * @throws CommandSyntaxException if the value is rejected by the filter
     */
    @Override
    public T convert(T nativeType) throws CommandSyntaxException {
        if (!filter.test(nativeType)) {
            throw ERROR_INVALID.create();
        }
        return nativeType;
    }

    /**
     * Returns the native Paper argument type that the client uses for parsing
     * and initial validation. This delegates to
     * {@link ArgumentTypes#resource(RegistryKey)}, which tells the client to
     * expect a namespaced resource location from the specified registry.
     *
     * @return the native {@link ArgumentType} backed by the Paper registry
     */
    @Override
    public ArgumentType<T> getNativeType() {
        return ArgumentTypes.resource(registryKey);
    }

    /**
     * Provides filtered tab-completion suggestions for the argument.
     *
     * <p>The matching behaviour mirrors vanilla Minecraft's resource-location
     * completion:</p>
     * <ul>
     *     <li>If the player's input already contains a colon ({@code :}), the
     *         full namespaced key (e.g. {@code minecraft:zombie}) is matched
     *         against the remaining input using {@link #matchesSubStr}.</li>
     *     <li>If the input does <em>not</em> contain a colon, suggestions are
     *         shown when the input matches either the namespace or, for
     *         {@code minecraft:} entries, the value portion alone. This lets
     *         players type {@code zom} and receive {@code minecraft:zombie}
     *         without needing the namespace prefix.</li>
     * </ul>
     *
     * @param <S>     the command source type
     * @param context the current command context
     * @param builder the suggestion builder to populate
     * @return a future containing the built suggestions
     */
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining();
        final boolean hasColon = remaining.indexOf(':') >= 0;

        for (Key key : this.suggestions) {
            final String full = key.asString();
            if (hasColon) {
                if (matchesSubStr(remaining, full)) {
                    builder.suggest(full);
                }
            } else {
                if (matchesSubStr(remaining, key.namespace())
                        || ("minecraft".equals(key.namespace()) && matchesSubStr(remaining, key.value()))) {
                    builder.suggest(full);
                }
            }
        }

        return builder.buildFuture();
    }

    /**
     * Tests whether the player's partial input matches a candidate string
     * using vanilla Minecraft's underscore-segmented substring matching.
     *
     * <p>The algorithm checks if {@code remaining} is a prefix of
     * {@code candidate} starting at position 0, or at the character
     * immediately following any underscore ({@code _}) within the candidate.
     * This allows players to type a substring that begins at any
     * "word boundary" (underscore segment) of the candidate.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *     <li>{@code matchesSubStr("pig", "zombie_piglin")} &rarr; {@code true}
     *         (matches at the segment after {@code _})</li>
     *     <li>{@code matchesSubStr("zom", "zombie_piglin")} &rarr; {@code true}
     *         (matches at the start)</li>
     *     <li>{@code matchesSubStr("bie", "zombie_piglin")} &rarr; {@code false}
     *         (does not start at position 0 or after an underscore)</li>
     * </ul>
     *
     * @param remaining the text the player has typed so far
     * @param candidate the full string to test against
     * @return {@code true} if {@code remaining} matches {@code candidate} at
     *         the start or after any underscore segment boundary
     */
    static boolean matchesSubStr(String remaining, String candidate) {
        for (int i = 0; !candidate.startsWith(remaining, i); ++i) {
            i = candidate.indexOf('_', i);
            if (i < 0) {
                return false;
            }
        }
        return true;
    }
}
