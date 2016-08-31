package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.NotImplemented;
import de.Keyle.MyPet.api.util.inventory.IconMenuItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.admin.CommandOptionCreate;
import de.Keyle.MyPet.util.selectionmenu.SpawnerEggTypes;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.UUID;

public class ShopMyPet implements StoredMyPet {
    protected double price = 0;
    protected IconMenuItem icon;
    protected String name;
    protected int position = -1;

    protected UUID uuid = null;
    protected MyPetPlayer petOwner = null;
    protected String petName = "";
    protected String worldGroup = "";
    protected double exp = 0;
    protected MyPetType petType = MyPetType.Wolf;
    protected SkillTree skillTree = null;
    protected TagCompound NBTSkills;
    protected TagCompound NBTextendetInfo;

    public ShopMyPet(String name) {
        this.name = name;
        this.icon = new IconMenuItem().setMaterial(Material.MONSTER_EGG);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public IconMenuItem getIcon() {
        IconMenuItem icon = this.icon.clone();
        if (icon.getMaterial() == Material.MONSTER_EGG) {
            SpawnerEggTypes egg = SpawnerEggTypes.getEggType(petType);
            icon.setGlowing(egg.isGlowing());
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                TagCompound entityTag = new TagCompound();
                entityTag.put("id", new TagString(egg.getEggName()));
                icon.addTag("EntityTag", entityTag);
            } else {
                icon.setData(egg.getColor());
            }
        }
        icon.setTitle(ChatColor.AQUA + Colorizer.setColors(getPetName()));

        return icon;
    }

    public void setIcon(IconMenuItem icon) {
        this.icon = icon;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getExp() {
        return exp;
    }

    public void setExp(double exp) {
        this.exp = exp;
    }

    public double getHealth() {
        return MyPetApi.getMyPetInfo().getStartHP(getPetType());
    }

    @NotImplemented
    public void setHealth(double health) {
    }

    @Override
    public double getSaturation() {
        return 100;
    }

    @Deprecated
    public double getHungerValue() {
        return getSaturation();
    }

    @NotImplemented
    public void setSaturation(double value) {
    }

    @Deprecated
    public void setHungerValue(double value) {
        setSaturation(value);
    }

    public TagCompound getInfo() {
        if (NBTextendetInfo == null) {
            NBTextendetInfo = new TagCompound();
        }
        return NBTextendetInfo;
    }

    public void setInfo(TagCompound info) {
        NBTextendetInfo = info;
    }

    public void setOwner(MyPetPlayer owner) {
        petOwner = owner;
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    public String getPetName() {
        if (petName != null) {
            return Colorizer.setColors(petName);
        }
        if (petOwner != null) {
            return Colorizer.setColors(Translation.getString("Name." + petType.name(), petOwner));
        }
        return "MyPet";
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public MyPetType getPetType() {
        return petType;
    }

    public void setPetType(MyPetType petType) {
        this.petType = petType;
    }

    public boolean wantsToRespawn() {
        return true;
    }

    @NotImplemented
    public void setWantsToRespawn(boolean wantsToRespawn) {
    }

    public int getRespawnTime() {
        return 0;
    }

    @NotImplemented
    public void setRespawnTime(int respawnTime) {
    }

    public SkillTree getSkilltree() {
        return skillTree;
    }

    public boolean setSkilltree(SkillTree skillTree) {
        this.skillTree = skillTree;
        return true;
    }

    public TagCompound getSkillInfo() {
        if (NBTSkills == null) {
            NBTSkills = new TagCompound();
        }
        return NBTSkills;
    }

    public void setSkills(TagCompound skills) {
        NBTSkills = skills;
    }

    public UUID getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    @NotImplemented
    public void setUUID(UUID uuid) {
    }

    @Override
    public String getWorldGroup() {
        return worldGroup;
    }

    @Override
    public long getLastUsed() {
        return System.currentTimeMillis();
    }

    @NotImplemented
    public void setLastUsed(long lastUsed) {
    }

    public void setWorldGroup(String worldGroup) {
        if (worldGroup != null) {
            this.worldGroup = worldGroup;
        }
    }

    public void load(ConfigurationSection config) {
        price = config.getDouble("Price", 0);
        position = config.getInt("Position", -1); //TODO -1 = free space
        petType = MyPetType.byName(config.getString("PetType", "Pig"));
        exp = config.getDouble("EXP");
        petName = config.getString("Name", null);
        skillTree = SkillTreeMobType.byPetType(petType).getSkillTree(config.getString("Skilltree", null));
        for (String line : config.getStringList("Description")) {
            icon.addLoreLine(Colorizer.setColors(line));
        }
        List<String> options = config.getStringList("Options");
        if (options != null && options.size() > 0) {
            TagCompound compound = new TagCompound();
            String[] optionsArray = options.toArray(new String[options.size()]);
            CommandOptionCreate.createInfo(petType, optionsArray, compound);
            this.NBTextendetInfo = compound;
        }
    }

    @Override
    public String toString() {
        return "ShopMyPet{type=" + getPetType().name() + ", owner=" + getOwner().getName() + ", exp=" + getExp() + ", worldgroup=" + worldGroup + (skillTree != null ? ", skilltree=" + skillTree.getName() : "") + "}";
    }
}