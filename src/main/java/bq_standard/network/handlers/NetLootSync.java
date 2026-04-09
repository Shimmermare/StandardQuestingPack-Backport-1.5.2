package bq_standard.network.handlers;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.Consumer;
import betterquesting.backport.PlayerUtils;
import bq_standard.core.BQ_Standard;
import bq_standard.rewards.loot.LootRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import betterquesting.backport.ResourceLocation;
import net.minecraft.util.EnumChatFormatting;

import java.util.logging.Level;

import javax.annotation.Nullable;

public class NetLootSync
{
	private static final ResourceLocation ID_NAME = new ResourceLocation("bq_standard:loot_database");
	
	public static void registerHandler()
    {
        QuestingAPI.getAPI(ApiReference.PACKET_REG).registerServerHandler(ID_NAME, new Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>>() {
            @Override
            public void accept(Tuple2<NBTTagCompound, EntityPlayerMP> value) {
                NetLootSync.onServer(value);
            }
        });
    
        if(BQ_Standard.proxy.isClient())
        {
            QuestingAPI.getAPI(ApiReference.PACKET_REG).registerClientHandler(ID_NAME, new Consumer<NBTTagCompound>() {
                @Override
                public void accept(NBTTagCompound compound) {
                    NetLootSync.onClient(compound);
                }
            });
        }
    }
    
    @SideOnly(Side.CLIENT)
    public static void requestEdit(NBTTagCompound data)
    {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setTag("data", data);
        QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToServer(new QuestingPacket(ID_NAME, payload));
    }
    
    public static void sendSync(@Nullable EntityPlayerMP player)
    {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setTag("data", LootRegistry.INSTANCE.writeToNBT(new NBTTagCompound(), null));
        
        if(player == null)
        {
            QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToAll(new QuestingPacket(ID_NAME, payload));
        } else
        {
            QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToPlayers(new QuestingPacket(ID_NAME, payload), player);
        }
    }
    
	private static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message)
	{
	    EntityPlayerMP sender = message.getSecond();
	    NBTTagCompound data = message.getFirst();
	    
	    if(sender.mcServer == null) return;
		if(!PlayerUtils.isEffectivelyOP(sender))
		{
			BQ_Standard.logger.log(Level.WARNING, "Player " + sender.getCommandSenderName() + " (UUID:" + QuestingAPI.getQuestingUUID(sender) + ") tried to edit loot chests without OP permissions!");
			sender.sendChatToPlayer(EnumChatFormatting.RED + "You need to be OP to edit loot!");
			return; // Player is not operator. Do nothing
		}
		
		BQ_Standard.logger.log(Level.INFO, "Player " + sender.getCommandSenderName() + " edited loot chests");
		
		LootRegistry.INSTANCE.readFromNBT(data.getCompoundTag("data"), false);
		sendSync(null);
	}
	
	@SideOnly(Side.CLIENT)
	private static void onClient(NBTTagCompound message)
	{
		LootRegistry.INSTANCE.readFromNBT(message.getCompoundTag("data"), false);
		LootRegistry.INSTANCE.updateUI = true;
	}
}
