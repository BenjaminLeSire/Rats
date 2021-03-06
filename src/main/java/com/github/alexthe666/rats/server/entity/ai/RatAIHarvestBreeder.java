package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatCommand;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.IShearable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class RatAIHarvestBreeder extends EntityAIBase {
    private final EntityRat entity;
    private Entity targetSheep = null;
    private boolean reachedSheep = false;
    private int fishingCooldown = 1000;
    private int throwCooldown = 0;
    private static final ItemStack SHEAR_STACK = new ItemStack(Items.SHEARS);
    private Random rand = new Random();
    private Predicate<EntityLivingBase> perRatPredicate;
    public RatAIHarvestBreeder(EntityRat entity) {
        super();
        this.entity = entity;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.entity.canMove() || !this.entity.isTamed() || this.entity.getCommand() != RatCommand.HARVEST || this.entity.isInCage() || !entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_BREEDER)) {
            return false;
        }
        if (this.entity.getHeldItemMainhand().isEmpty()) {
            return false;
        }
        resetTarget();
        return targetSheep != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetSheep != null && !this.entity.getHeldItemMainhand().isEmpty();
    }

    public void resetTask() {
        this.entity.getNavigator().clearPath();
        resetTarget();
    }

    @Override
    public void updateTask() {
        if (this.targetSheep != null && !this.targetSheep.isDead && !this.entity.getHeldItemMainhand().isEmpty()) {
            this.entity.getNavigator().tryMoveToEntityLiving(this.targetSheep, 1D);
            if (entity.getDistance(targetSheep) < 1.5D) {
                if(targetSheep instanceof EntityAnimal && !((EntityAnimal) targetSheep).isInLove()){
                    ((EntityAnimal) targetSheep).setInLove(null);
                    this.entity.getHeldItemMainhand().shrink(1);
                }
                this.targetSheep = null;
                this.resetTask();
            }
        }else{
            this.resetTask();
        }
    }

    private void resetTarget() {
        perRatPredicate = new com.google.common.base.Predicate<EntityLivingBase>() {
            public boolean apply(@Nullable EntityLivingBase entity) {
                return entity != null && entity instanceof EntityAnimal && !entity.isChild() && !((EntityAnimal) entity).isInLove() && ((EntityAnimal) entity).getGrowingAge() == 0 && RatAIHarvestBreeder.canBreedMob(((EntityAnimal) entity), RatAIHarvestBreeder.this.entity.getHeldItemMainhand());
            }
        };
        int radius = this.entity.getSearchRadius();
        AxisAlignedBB bb = new AxisAlignedBB(-radius, -radius, -radius, radius, radius, radius).offset(entity.getSearchCenter());
        List<EntityLiving> list = this.entity.world.<EntityLiving>getEntitiesWithinAABB(EntityLiving.class, bb, (com.google.common.base.Predicate<? super EntityLiving>) perRatPredicate);
        EntityLivingBase closestSheep = null;
        if(list.size() >= 2) {
            for (EntityLivingBase base : list) {
                if (closestSheep == null || base.getDistanceSq(entity) < closestSheep.getDistanceSq(entity)) {
                    closestSheep = base;
                }
            }
            if (closestSheep != null) {
                this.targetSheep = closestSheep;
            }
        }
    }

    private static boolean canBreedMob(EntityAnimal entity, ItemStack heldItemMainhand) {
        return !(entity instanceof EntityRat) && ((EntityAnimal) entity).isBreedingItem(heldItemMainhand)
                || entity instanceof EntityLlama && heldItemMainhand.getItem() == Item.getItemFromBlock(Blocks.HAY_BLOCK)
                || entity instanceof AbstractHorse && heldItemMainhand.getItem() == Items.GOLDEN_APPLE;
    }

}