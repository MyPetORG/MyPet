package de.Keyle.MyPet.api.util.configuration;

public class ConfigurationManager {

    private PetSelectionGuiCfg petSelectionGuiConfig;

    public  ConfigurationManager() {
        reloadAll();
    }

    public PetSelectionGuiCfg getPetSelectionGuiConfig() {
        if (petSelectionGuiConfig == null) {
            petSelectionGuiConfig = new PetSelectionGuiCfg();
        }
        return petSelectionGuiConfig;
    }

    public void reloadAll() {
        petSelectionGuiConfig = new PetSelectionGuiCfg();
    }
}
