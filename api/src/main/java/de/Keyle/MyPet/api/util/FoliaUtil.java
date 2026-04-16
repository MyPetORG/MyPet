package de.Keyle.MyPet.api.util;

public final class FoliaUtil {
    private static final boolean IS_FOLIA = checkFolia();

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    private FoliaUtil() {}
}
