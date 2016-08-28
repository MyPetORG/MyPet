package de.Keyle.MyPet.api.util.locale;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Properties;

public class TranslationBundle {
    HashMap<String, String> translations = new HashMap<>();

    public TranslationBundle() {
    }

    public TranslationBundle(Reader reader) {
        load(reader);
    }

    public void load(Reader reader) {
        Properties properties = new Properties();

        try {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Object o : properties.keySet()) {
            translations.put(o.toString().toLowerCase(), properties.get(o).toString());
        }
    }

    public boolean containsKey(String key) {
        return translations.containsKey(key.toLowerCase());
    }

    public String getString(String key) {
        return translations.get(key.toLowerCase());
    }
}