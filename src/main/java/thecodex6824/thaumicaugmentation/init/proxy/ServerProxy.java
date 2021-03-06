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

package thecodex6824.thaumicaugmentation.init.proxy;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.common.golems.client.gui.SealBaseContainer;
import thecodex6824.thaumicaugmentation.ThaumicAugmentation;
import thecodex6824.thaumicaugmentation.api.impetus.node.IImpetusNode;
import thecodex6824.thaumicaugmentation.api.ward.storage.IWardStorage;
import thecodex6824.thaumicaugmentation.api.ward.storage.WardStorageServer;
import thecodex6824.thaumicaugmentation.common.container.ContainerArcaneTerraformer;
import thecodex6824.thaumicaugmentation.common.container.ContainerAutocaster;
import thecodex6824.thaumicaugmentation.common.container.ContainerWardedChest;
import thecodex6824.thaumicaugmentation.common.entity.EntityAutocaster;
import thecodex6824.thaumicaugmentation.common.event.PlayerEventHandler;
import thecodex6824.thaumicaugmentation.common.network.PacketBoostState;
import thecodex6824.thaumicaugmentation.common.network.PacketElytraBoost;
import thecodex6824.thaumicaugmentation.common.network.PacketInteractGUI;
import thecodex6824.thaumicaugmentation.common.network.TANetwork;
import thecodex6824.thaumicaugmentation.common.tile.TileArcaneTerraformer;
import thecodex6824.thaumicaugmentation.common.tile.TileWardedChest;
import thecodex6824.thaumicaugmentation.common.util.ISoundHandle;
import thecodex6824.thaumicaugmentation.common.util.ITARenderHelper;
import thecodex6824.thaumicaugmentation.common.util.TARenderHelperServer;
import thecodex6824.thaumicaugmentation.init.GUIHandler.TAInventory;

public class ServerProxy implements ISidedProxy {

    protected static ITARenderHelper renderHelper;
    protected static Cache<UUID, Integer> invalidBoosts = CacheBuilder.newBuilder().concurrencyLevel(1).
            expireAfterAccess(5, TimeUnit.MINUTES).build();

    @Override
    public IAnimationStateMachine loadASM(ResourceLocation loc, ImmutableMap<String, ITimeValue> params) {
        return null;
    }

    @Override
    public ITARenderHelper getRenderHelper() {
        if (renderHelper == null)
            renderHelper = new TARenderHelperServer();

        return renderHelper;
    }
    
    @Override
    public IWardStorage createWardStorageInstance(World world) {
        return new WardStorageServer();
    }
    
    @Override
    public void registerRenderableImpetusNode(IImpetusNode node) {}
    
    @Override
    public boolean deregisterRenderableImpetusNode(IImpetusNode node) {
        return false;
    }
    
    @Override
    public boolean isOpenToLAN() {
        return false;
    }
    
    @Override
    public boolean isSingleplayer() {
        return false;
    }
    
    @Override
    public boolean isJumpDown() {
        return false;
    }
    
    @Override
    public boolean isPvPEnabled() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled();
    }
    
    @Override
    public boolean isEntityClientPlayer(Entity e) {
        return false;
    }
    
    @Override
    public boolean isEntityRenderView(Entity e) {
        return false;
    }
    
    @Override
    public float getPartialTicks() {
        return 1.0F;
    }
    
    @Override
    public Container getServerGUIElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (TAInventory.values()[ID]) {
            case WARDED_CHEST: return new ContainerWardedChest(player.inventory, 
                (TileWardedChest) world.getTileEntity(new BlockPos(x, y, z)));
            case ARCANE_TERRAFORMER: return new ContainerArcaneTerraformer(player.inventory, 
                    (TileArcaneTerraformer) world.getTileEntity(new BlockPos(x, y, z)));
            case AUTOCASTER: return new ContainerAutocaster(player.inventory, (EntityAutocaster) world.getEntityByID(x));
            default: return null;
        }
    }
    
    @Override
    public Object getClientGUIElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        throw new UnsupportedOperationException("Cannot get client GUI element on the server side!");
    }
    
    @Override
    public Object getSealContainer(World world, EntityPlayer player, BlockPos pos, EnumFacing face, ISealEntity seal) {
        return new SealBaseContainer(player.inventory, world, seal);
    }
    
    @Override
    public Object getSealGUI(World world, EntityPlayer player, BlockPos pos, EnumFacing face, ISealEntity seal) {
        throw new UnsupportedOperationException("Cannot get client GUI element on the server side!");
    }
    
    @Override
    public ISoundHandle playSpecialSound(SoundEvent sound, SoundCategory category, Function<Vec3d, Vec3d> tick, float x,
            float y, float z, float vol, float pitch, boolean repeat, int repeatDelay) {
        
        return new ISoundHandle.Noop();
    }
    
    @Override
    public void handlePacketClient(IMessage message, MessageContext context) {
        ThaumicAugmentation.getLogger().warn("A packet was received on the wrong side: " + message.getClass().toString());
        if (!isSingleplayer()) {
            EntityPlayerMP player = context.getServerHandler().player;
            ThaumicAugmentation.getLogger().info("Player {} ({}) kicked for protocol violation: sent invalid client packet", player.getName(), player.getGameProfile().getId());
            context.getServerHandler().disconnect(new TextComponentTranslation("thaumicaugmentation.text.network_kick"));
        }
    }
    
    @Override
    public void handlePacketServer(IMessage message, MessageContext context) {
        if (message instanceof PacketElytraBoost)
            handleElytraBoostPacket((PacketElytraBoost) message, context);
        else if (message instanceof PacketInteractGUI)
            handleInteractGUIPacket((PacketInteractGUI) message, context);
        else {
            ThaumicAugmentation.getLogger().warn("An unknown packet was received and will be dropped: " + message.getClass().toString());
            if (!isSingleplayer()) {
                EntityPlayerMP player = context.getServerHandler().player;
                ThaumicAugmentation.getLogger().info("Player {} ({}) kicked for protocol violation: sent unknown packet", player.getName(), player.getGameProfile().getId());
                context.getServerHandler().disconnect(new TextComponentTranslation("thaumicaugmentation.text.network_kick"));
            }
        }
    }
    
    protected void handleElytraBoostPacket(PacketElytraBoost message, MessageContext context) {
        EntityPlayerMP entity = context.getServerHandler().player;
        if (!message.isStarting()) {
            if (PlayerEventHandler.getBoostState(entity)) {
                PlayerEventHandler.updateBoostState(entity, false);
                PacketBoostState packet = new PacketBoostState(entity.getEntityId(), false);
                if (entity instanceof EntityPlayerMP)
                    TANetwork.INSTANCE.sendTo(packet, (EntityPlayerMP) entity);
                
                TANetwork.INSTANCE.sendToAllTracking(packet, entity);
            }
        }
        else {
            if (PlayerEventHandler.playerCanBoost(entity)) {
                PlayerEventHandler.updateBoostState(entity, true);
                PacketBoostState packet = new PacketBoostState(entity.getEntityId(), true);
                if (entity instanceof EntityPlayerMP)
                    TANetwork.INSTANCE.sendTo(packet, (EntityPlayerMP) entity);
                
                TANetwork.INSTANCE.sendToAllTracking(packet, entity);
            }
            else {
                if (!isSingleplayer()) {
                    int fails = 5;
                    try {
                        fails = invalidBoosts.get(entity.getGameProfile().getId(), () -> 0);
                    }
                    catch (ExecutionException ex) {}
                    
                    if (fails >= 5) {
                        ThaumicAugmentation.getLogger().info("Player {} ({}) kicked for protocol violation: exceeded maximum acceptable incorrect elytra boost requests", entity.getName(), entity.getGameProfile().getId());
                        context.getServerHandler().disconnect(new TextComponentTranslation("thaumicaugmentation.text.network_kick"));
                    }
                    else {
                        invalidBoosts.put(entity.getGameProfile().getId(), fails + 1);
                        entity.connection.sendPacket(new SPacketEntityVelocity(entity));
                    }
                }
                else
                    entity.connection.sendPacket(new SPacketEntityVelocity(entity));
            }
        }
    }
    
    protected void handleInteractGUIPacket(PacketInteractGUI message, MessageContext context) {
        EntityPlayerMP sender = context.getServerHandler().player;
        if (sender != null && sender.openContainer instanceof ContainerArcaneTerraformer) {
            ContainerArcaneTerraformer terraformer = (ContainerArcaneTerraformer) sender.openContainer;
            if (!terraformer.getTile().isRunning()) {
                if (message.getComponentID() == 0)
                    terraformer.getTile().setRadius(Math.max(Math.min(message.getSelectionValue(), 32), 1));
                else if (message.getComponentID() == 1)
                    terraformer.getTile().setCircle(message.getSelectionValue() != 0);
                else if (!isSingleplayer()) {
                    ThaumicAugmentation.getLogger().info("Player {} ({}) kicked for protocol violation: invalid component ID", sender.getName(), sender.getGameProfile().getId());
                    context.getServerHandler().disconnect(new TextComponentTranslation("thaumicaugmentation.text.network_kick"));
                }
            }
        }
        else if (sender != null && sender.openContainer instanceof ContainerAutocaster) {
            EntityAutocaster autocaster = ((ContainerAutocaster) sender.openContainer).getEntity();
            switch (message.getComponentID()) {
                case 0: {
                    autocaster.setTargetAnimals(message.getSelectionValue() > 0);
                    break;
                }
                case 1: {
                    autocaster.setTargetMobs(message.getSelectionValue() > 0);
                    break;
                }
                case 2: {
                    autocaster.setTargetPlayers(message.getSelectionValue() > 0);
                    break;
                }
                case 3: {
                    autocaster.setTargetFriendly(message.getSelectionValue() > 0);
                    break;
                }
                case 4: {
                    autocaster.setRedstoneControl(message.getSelectionValue() > 0);
                    break;
                }
                default: {
                    if (!isSingleplayer()) {
                        ThaumicAugmentation.getLogger().info("Player {} ({}) kicked for protocol violation: invalid component ID", sender.getName(), sender.getGameProfile().getId());
                        context.getServerHandler().disconnect(new TextComponentTranslation("thaumicaugmentation.text.network_kick"));
                    }
                    break;
                }
            }
        }
        else if (!isSingleplayer()) {
            ThaumicAugmentation.getLogger().info("Player {} ({}) kicked for protocol violation: invalid container", sender.getName(), sender.getGameProfile().getId());
            context.getServerHandler().disconnect(new TextComponentTranslation("thaumicaugmentation.text.network_kick"));
        }
    }

    @Override
    public void preInit() {}

    @Override
    public void init() {}

    @Override
    public void postInit() {}

}
