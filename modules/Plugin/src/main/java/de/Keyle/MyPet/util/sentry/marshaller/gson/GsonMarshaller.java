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

package de.Keyle.MyPet.util.sentry.marshaller.gson;

import com.google.gson.*;
import de.Keyle.MyPet.util.sentry.marshaller.gson.bindings.InterfaceBinding;
import io.sentry.event.Breadcrumb;
import io.sentry.event.Event;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.SentryInterface;
import io.sentry.marshaller.Marshaller;
import io.sentry.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class GsonMarshaller implements Marshaller {

    /**
     * Hexadecimal string representing a uuid4 value.
     */
    public static final String EVENT_ID = "event_id";
    /**
     * User-readable representation of this event.
     */
    public static final String MESSAGE = "message";
    /**
     * Indicates when the logging record was created.
     */
    public static final String TIMESTAMP = "timestamp";
    /**
     * The record severity.
     */
    public static final String LEVEL = "level";
    /**
     * The name of the logger which created the record.
     */
    public static final String LOGGER = "logger";
    /**
     * A string representing the platform the client is submitting from.
     */
    public static final String PLATFORM = "platform";
    /**
     * Function call which was the primary perpetrator of this event.
     */
    public static final String CULPRIT = "culprit";
    /**
     * Name of the transaction that this event occurred inside of.
     */
    public static final String TRANSACTION = "transaction";
    /**
     * An object representing the SDK name and version.
     */
    public static final String SDK = "sdk";
    /**
     * A map or list of tags for this event.
     */
    public static final String TAGS = "tags";
    /**
     * List of breadcrumbs for this event.
     */
    public static final String BREADCRUMBS = "breadcrumbs";
    /**
     * Map of map of contexts for this event.
     */
    public static final String CONTEXTS = "contexts";
    /**
     * Identifies the host client from which the event was recorded.
     */
    public static final String SERVER_NAME = "server_name";
    /**
     * Identifies the the version of the application.
     */
    public static final String RELEASE = "release";
    /**
     * Identifies the the distribution of the application.
     */
    public static final String DIST = "dist";
    /**
     * Identifies the environment the application is running in.
     */
    public static final String ENVIRONMENT = "environment";
    /**
     * Event fingerprint, a list of strings used to dictate the deduplicating for this event.
     */
    public static final String FINGERPRINT = "fingerprint";
    /**
     * An arbitrary mapping of additional metadata to store with the event.
     */
    public static final String EXTRA = "extra";
    /**
     * Checksum for the event, allowing to group events with a similar checksum.
     */
    public static final String CHECKSUM = "checksum";
    /**
     * Default maximum length for a message.
     */
    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 1000;
    /**
     * Date format for ISO 8601.
     */
    private static final ThreadLocal<DateFormat> ISO_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    });

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final Map<Class<? extends SentryInterface>, InterfaceBinding<?>> interfaceBindings = new HashMap<>();
    /**
     * Enables disables the compression of JSON.
     */
    private boolean compression = true;
    /**
     * Maximum length for a message.
     */
    private final int maxMessageLength;

    /**
     * Create instance of JsonMarshaller with provided maximum length of the messages.
     *
     * @param maxMessageLength the maximum message length
     */
    public GsonMarshaller(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    @Override
    public void marshall(Event event, OutputStream destination) throws IOException {
        // Prevent the stream from being closed automatically
        destination = new Marshaller.UncloseableOutputStream(destination);

        if (compression) {
            destination = new GZIPOutputStream(destination);
        }


        try {
            JsonObject json = new JsonObject();
            writeContent(json, event);
            destination.write(gson.toJson(json).getBytes());
        } catch (IOException e) {
            System.err.println("An exception occurred while serialising the event.");
            e.printStackTrace();
        } finally {
            try {
                destination.close();
            } catch (IOException e) {
                System.err.println("An exception occurred while serialising the event.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public String getContentEncoding() {
        if (isCompressed()) {
            return "gzip";
        }
        return null;
    }

    private void writeContent(JsonObject generator, Event event) throws IOException {
        generator.addProperty(EVENT_ID, formatId(event.getId()));
        generator.addProperty(MESSAGE, Util.trimString(event.getMessage(), maxMessageLength));
        generator.addProperty(TIMESTAMP, ISO_FORMAT.get().format(event.getTimestamp()));
        generator.addProperty(LEVEL, formatLevel(event.getLevel()));
        generator.addProperty(LOGGER, event.getLogger());
        generator.addProperty(PLATFORM, event.getPlatform());
        generator.addProperty(CULPRIT, event.getCulprit());
        generator.addProperty(TRANSACTION, event.getTransaction());
        generator.add(SDK, writeSdk(event.getSdk()));
        generator.add(TAGS, writeTags(event.getTags()));
        generator.add(BREADCRUMBS, writeBreadcumbs(event.getBreadcrumbs()));
        generator.add(CONTEXTS, writeContexts(event.getContexts()));
        generator.addProperty(SERVER_NAME, event.getServerName());
        generator.addProperty(RELEASE, event.getRelease());
        generator.addProperty(DIST, event.getDist());
        generator.addProperty(ENVIRONMENT, event.getEnvironment());
        generator.add(EXTRA, writeExtras(event.getExtra()));
        generator.add(FINGERPRINT, writeCollection(event.getFingerprint()));
        generator.addProperty(CHECKSUM, event.getChecksum());
        writeInterfaces(generator, event.getSentryInterfaces());
    }

    private void writeInterfaces(JsonObject generator, Map<String, SentryInterface> sentryInterfaces)
            throws IOException {
        for (Map.Entry<String, SentryInterface> interfaceEntry : sentryInterfaces.entrySet()) {
            SentryInterface sentryInterface = interfaceEntry.getValue();

            if (interfaceBindings.containsKey(sentryInterface.getClass())) {
                JsonElement interfaceData = getInterfaceBinding(sentryInterface).writeInterface(interfaceEntry.getValue());
                generator.add(interfaceEntry.getKey(), interfaceData);
            } else {
                System.err.println("Couldn't parse the content of '" + interfaceEntry.getKey() + "' provided in " + sentryInterface + ".");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends SentryInterface> InterfaceBinding<? super T> getInterfaceBinding(T sentryInterface) {
        // Reduces the @SuppressWarnings to a oneliner
        return (InterfaceBinding<? super T>) interfaceBindings.get(sentryInterface.getClass());
    }

    private JsonObject writeExtras(Map<String, Object> extras) {
        JsonObject generator = new JsonObject();
        for (Map.Entry<String, Object> extra : extras.entrySet()) {
            generator.addProperty(extra.getKey(), extra.getValue().toString());
        }
        return generator;
    }

    private JsonArray writeCollection(Collection<String> value) {
        JsonArray generator = new JsonArray();
        if (value != null && !value.isEmpty()) {
            for (String element : value) {
                generator.add(element);
            }
        }
        return generator;
    }

    private JsonObject writeSdk(Sdk sdk) {
        JsonObject generator = new JsonObject();
        generator.addProperty("name", sdk.getName() + "-gson");
        generator.addProperty("version", sdk.getVersion());
        if (sdk.getIntegrations() != null && !sdk.getIntegrations().isEmpty()) {
            JsonArray integrations = new JsonArray();
            for (String integration : sdk.getIntegrations()) {
                integrations.add(integration);
            }
            generator.add("integrations", integrations);
        }
        return generator;
    }

    private JsonObject writeTags(Map<String, String> tags) throws IOException {
        JsonObject generator = new JsonObject();
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            generator.addProperty(tag.getKey(), tag.getValue());
        }
        return generator;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private JsonObject writeBreadcumbs(List<Breadcrumb> breadcrumbs) {
        JsonObject generator = new JsonObject();
        if (breadcrumbs.isEmpty()) {
            return generator;
        }

        JsonArray values = new JsonArray();
        for (Breadcrumb breadcrumb : breadcrumbs) {
            JsonObject breadcrumbObject = new JsonObject();
            // getTime() returns ts in millis, but breadcrumbs expect seconds
            breadcrumbObject.addProperty("timestamp", breadcrumb.getTimestamp().getTime() / 1000);

            if (breadcrumb.getType() != null) {
                breadcrumbObject.addProperty("type", breadcrumb.getType().getValue());
            }
            if (breadcrumb.getLevel() != null) {
                breadcrumbObject.addProperty("level", breadcrumb.getLevel().getValue());
            }
            if (breadcrumb.getMessage() != null) {
                breadcrumbObject.addProperty("message", breadcrumb.getMessage());
            }
            if (breadcrumb.getCategory() != null) {
                breadcrumbObject.addProperty("category", breadcrumb.getCategory());
            }
            if (breadcrumb.getData() != null && !breadcrumb.getData().isEmpty()) {
                JsonObject data = new JsonObject();
                for (Map.Entry<String, String> entry : breadcrumb.getData().entrySet()) {
                    data.addProperty(entry.getKey(), entry.getValue());
                }
                breadcrumbObject.add("data", data);
            }
        }
        generator.add("values", values);
        return generator;
    }

    public static void writeObject(JsonObject jsonObject, String key, Object value) {
        if (value == null) {
            jsonObject.addProperty(key, (String) null);
        } else if (value instanceof Number) {
            jsonObject.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            jsonObject.addProperty(key, (Boolean) value);
        } else {
            jsonObject.addProperty(key, String.valueOf(value));
        }
    }

    private JsonObject writeContexts(Map<String, Map<String, Object>> contexts) {
        JsonObject generator = new JsonObject();
        for (Map.Entry<String, Map<String, Object>> contextEntry : contexts.entrySet()) {
            JsonObject context = new JsonObject();
            for (Map.Entry<String, Object> innerContextEntry : contextEntry.getValue().entrySet()) {
                writeObject(context, innerContextEntry.getKey(), innerContextEntry.getValue());
            }
            generator.add(contextEntry.getKey(), context);
        }
        return generator;
    }

    /**
     * Formats the {@code UUID} to send only the 32 necessary characters.
     *
     * @param id uuid to format.
     * @return a {@code UUID} stripped from the "-" characters.
     */
    private String formatId(UUID id) {
        return id.toString().replaceAll("-", "");
    }

    /**
     * Formats a log level into one of the accepted string representation of a log level.
     *
     * @param level log level to format.
     * @return log level as a String.
     */
    private String formatLevel(Event.Level level) {
        if (level == null) {
            return null;
        }

        switch (level) {
            case DEBUG:
                return "debug";
            case FATAL:
                return "fatal";
            case WARNING:
                return "warning";
            case INFO:
                return "info";
            case ERROR:
                return "error";
            default:
                System.err.println("The level '{}' isn't supported, this should NEVER happen, contact Sentry developers -> " + level.name());
                return null;
        }
    }

    /**
     * Add an interface binding to send a type of {@link SentryInterface} through a JSON stream.
     *
     * @param sentryInterfaceClass Actual type of SentryInterface supported by the {@link InterfaceBinding}
     * @param binding              InterfaceBinding converting SentryInterfaces of type {@code sentryInterfaceClass}.
     * @param <T>                  Type of SentryInterface received by the InterfaceBinding.
     * @param <F>                  Type of the interface stored in the event to send to the InterfaceBinding.
     */
    public <T extends SentryInterface, F extends T> void addInterfaceBinding(Class<F> sentryInterfaceClass,
                                                                             InterfaceBinding<T> binding) {
        this.interfaceBindings.put(sentryInterfaceClass, binding);
    }

    /**
     * Enables the JSON compression with gzip.
     *
     * @param compression state of the compression.
     */
    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    public boolean isCompressed() {
        return compression;
    }
}