/**
 *  Thaumic Augmentation
 *  Copyright (c) 2019 TheCodex6824.
 *
 *  This file is part of Thaumic Augmentation.
 *
 *  Thaumic Augmentation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Thaumic Augmentation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Thaumic Augmentation.  If not, see <https://www.gnu.org/licenses/>.
 */

package thecodex6824.thaumicaugmentation.common.item;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants.NBT;
import thecodex6824.thaumicaugmentation.api.TAConfig;
import thecodex6824.thaumicaugmentation.api.TAItems;
import thecodex6824.thaumicaugmentation.api.TASounds;
import thecodex6824.thaumicaugmentation.api.augment.CapabilityAugment;
import thecodex6824.thaumicaugmentation.api.augment.IAugment;
import thecodex6824.thaumicaugmentation.api.augment.builder.IImpulseCannonAugment;
import thecodex6824.thaumicaugmentation.api.entity.IStopRailgunBeam;
import thecodex6824.thaumicaugmentation.api.impetus.ImpetusAPI;
import thecodex6824.thaumicaugmentation.api.util.RaytraceHelper;
import thecodex6824.thaumicaugmentation.common.capability.provider.SimpleCapabilityProviderNoSave;
import thecodex6824.thaumicaugmentation.common.event.ScheduledTaskHandler;
import thecodex6824.thaumicaugmentation.common.item.prefab.ItemTABase;
import thecodex6824.thaumicaugmentation.common.network.PacketImpulseBurst;
import thecodex6824.thaumicaugmentation.common.network.PacketImpulseRailgunProjectile;
import thecodex6824.thaumicaugmentation.common.network.PacketRecoil;
import thecodex6824.thaumicaugmentation.common.network.PacketRecoil.RecoilType;
import thecodex6824.thaumicaugmentation.common.network.TANetwork;

public class ItemImpulseCannonAugment extends ItemTABase {

    protected static abstract class ImpulseCannonAugmentBase implements IImpulseCannonAugment {
        
        @Override
        public boolean canBeAppliedToItem(ItemStack augmentable) {
            return augmentable.getItem() == TAItems.IMPULSE_CANNON;
        }
        
        @Override
        public boolean isCompatible(ItemStack otherAugment) {
            return !(otherAugment.getCapability(CapabilityAugment.AUGMENT, null) instanceof ImpulseCannonAugmentBase);
        }
        
    }
    
    public ItemImpulseCannonAugment() {
        super("railgun", "burst");
        setMaxStackSize(1);
        setHasSubtypes(true);
    }
    
    protected IImpulseCannonAugment createAugmentForStack(ItemStack stack) {
        if (stack.getMetadata() == 0) {
            return new ImpulseCannonAugmentBase() {
                
                @Override
                public LensModelType getLensModel() {
                    return LensModelType.RAILGUN;
                }
                
                @Override
                public int getMaxUsageDuration() {
                    return 1;
                }
                
                @Override
                public boolean isTickable(EntityLivingBase user) {
                    return false;
                }
                
                @Override
                public long getImpetusCostPerUsage(EntityLivingBase user) {
                    return TAConfig.cannonRailgunCost.getValue();
                }
                
                @Override
                public void onCannonUsage(EntityLivingBase user) {
                    Vec3d target = RaytraceHelper.raytracePosition(user, TAConfig.cannonRailgunRange.getValue());
                    List<Entity> ents = RaytraceHelper.raytraceEntities(user, TAConfig.cannonRailgunRange.getValue());
                    for (Entity e : ents) {
                        ImpetusAPI.causeImpetusDamage(user, e, TAConfig.cannonRailgunDamage.getValue());
                        if (e instanceof IStopRailgunBeam && ((IStopRailgunBeam) e).shouldStopRailgunBeam(user))
                            break;
                    }
                    
                    Random rand = user.getRNG();
                    user.getEntityWorld().playSound(null, new BlockPos(user.getPositionEyes(1.0F)), TASounds.IMPULSE_CANNON_RAILGUN,
                            SoundCategory.PLAYERS, 1.0F, (rand.nextFloat() - rand.nextFloat()) / 2.0F + 1.0F);
                    PacketImpulseRailgunProjectile packet = new PacketImpulseRailgunProjectile(user.getEntityId(), target);
                    PacketRecoil recoil = new PacketRecoil(user.getEntityId(), RecoilType.IMPULSE_RAILGUN);
                    TANetwork.INSTANCE.sendToAllTracking(packet, user);
                    TANetwork.INSTANCE.sendToAllTracking(recoil, user);
                    if (user instanceof EntityPlayer) {
                        ((EntityPlayer) user).getCooldownTracker().setCooldown(TAItems.IMPULSE_CANNON, TAConfig.cannonRailgunCooldown.getValue());
                        if (user instanceof EntityPlayerMP) {
                            TANetwork.INSTANCE.sendTo(packet, (EntityPlayerMP) user);
                            TANetwork.INSTANCE.sendTo(recoil, (EntityPlayerMP) user);
                        }
                    }
                }
                
            };
        }
        else {
            return new ImpulseCannonAugmentBase() {
                
                @Override
                public LensModelType getLensModel() {
                    return LensModelType.BURST;
                }
                
                @Override
                public int getMaxUsageDuration() {
                    return 5;
                }
                
                @Override
                public boolean isTickable(EntityLivingBase user) {
                    return false;
                }
                
                @Override
                public long getImpetusCostPerUsage(EntityLivingBase user) {
                    return TAConfig.cannonBurstCost.getValue();
                }
                
                private void tick(EntityLivingBase user, int num) {
                    Entity e = RaytraceHelper.raytraceEntity(user, TAConfig.cannonBurstRange.getValue());
                    if (e != null && ImpetusAPI.causeImpetusDamage(user, e, TAConfig.cannonBurstDamage.getValue()) && num < 2
                            && e instanceof EntityLivingBase) {
                            
                        EntityLivingBase base = (EntityLivingBase) e;
                        base.hurtResistantTime = Math.min(base.hurtResistantTime, 1);
                        base.lastDamage = 0.0F;
                    }
                    
                    Random rand = user.getRNG();
                    user.getEntityWorld().playSound(null, new BlockPos(user.getPositionEyes(1.0F)), TASounds.IMPULSE_CANNON_BURST,
                            SoundCategory.PLAYERS, 1.0F, (rand.nextFloat() - rand.nextFloat()) / 2.0F + 1.0F);
                    Vec3d target = RaytraceHelper.raytracePosition(user, TAConfig.cannonBurstRange.getValue());
                    PacketImpulseBurst packet = new PacketImpulseBurst(user.getEntityId(), target, num);
                    TANetwork.INSTANCE.sendToAllTracking(packet, user);
                    if (user instanceof EntityPlayerMP)
                        TANetwork.INSTANCE.sendTo(packet, (EntityPlayerMP) user);
                    
                    if (num < 2)
                        ScheduledTaskHandler.registerTask(() -> tick(user, num + 1), 1);
                }
                
                @Override
                public void onCannonUsage(EntityLivingBase user) {
                    ScheduledTaskHandler.registerTask(() -> tick(user, 0), 0);
                    PacketRecoil recoil = new PacketRecoil(user.getEntityId(), RecoilType.IMPULSE_BURST);
                    TANetwork.INSTANCE.sendToAllTracking(recoil, user);
                    if (user instanceof EntityPlayer) {
                        ((EntityPlayer) user).getCooldownTracker().setCooldown(TAItems.IMPULSE_CANNON, TAConfig.cannonBurstCooldown.getValue());
                        if (user instanceof EntityPlayerMP)
                            TANetwork.INSTANCE.sendTo(recoil, (EntityPlayerMP) user);
                    }
                }
                
            };
        }
    }
    
    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        SimpleCapabilityProviderNoSave<IAugment> provider =
                new SimpleCapabilityProviderNoSave<>(createAugmentForStack(stack), CapabilityAugment.AUGMENT);
        if (nbt != null && nbt.hasKey("Parent", NBT.TAG_COMPOUND))
            provider.deserializeNBT(nbt.getCompoundTag("Parent"));
        
        return provider;
    }
    
}
