package de.Keyle.MyPet.compat.v1_21_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyArmadillo;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.7F, height = 0.65F)
public class EntityMyArmadillo extends EntityMyPet {

    private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMySniffer.class, EntityDataSerializers.BOOLEAN);

    public EntityMyArmadillo(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getLivingSound() {
        return "entity.armadillo.ambient";
    }

    @Override
    protected String getHurtSound() {
        return "entity.armadillo.hurt";
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.armadillo.death";
    }

    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        if (Configuration.MyPet.Armadillo.GROW_UP_ITEM.compare(itemStack) && ((MyArmadillo)getMyPet()).isBaby() && getOwner().getPlayer().isSneaking()) {
            if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
                itemStack.shrink(1);
                if (itemStack.getCount() <= 0) {
                    entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
                }
            }
            ((MyArmadillo)getMyPet()).setBaby(false);
            return InteractionResult.CONSUME;
        }
		return InteractionResult.PASS;
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.armadillo.step", 0.15F, 1.0F);
    }


    @Override
    public void updateVisuals() {
        this.getEntityData().set(AGE_WATCHER, ((MyArmadillo)getMyPet()).isBaby());
    }
}
