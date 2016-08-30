package de.Keyle.MyPet.api.util;

public enum WalletType {
    Private, Player, Bank, None;

    public static WalletType getByName(String name) {
        for (WalletType walletType : values()) {
            if (walletType.name().equalsIgnoreCase(name)) {
                return walletType;
            }
        }
        return null;
    }
}