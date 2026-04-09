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

public class NetLootImport
{
	private static final ResourceLocation ID_NAME = new ResourceLocation("bq_standard:loot_import");
	
	public static void registerHandler()
    {
        QuestingAPI.getAPI(ApiReference.PACKET_REG).registerServerHandler(ID_NAME, new Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>>() {
            @Override
            public void accept(Tuple2<NBTTagCompound, EntityPlayerMP> value) {
                NetLootImport.onServer(value);
            }
        });
    }
    
    // TODO: Rework this for partial importing/editing
    
    @SideOnly(Side.CLIENT)
    public static void importLoot(NBTTagCompound data)
    {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setTag("data", data);
        QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToServer(new QuestingPacket(ID_NAME, payload));
    }
    
	private static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message)
	{
	    EntityPlayerMP sender = message.getSecond();
	    NBTTagCompound tag = message.getFirst();
	    
		if(sender.mcServer == null) return;
		
		if(!PlayerUtils.isEffectivelyOP(sender))
		{
			BQ_Standard.logger.log(Level.WARNING, "Player " + sender.getCommandSenderName() + " (UUID:" + QuestingAPI.getQuestingUUID(sender) + ") tried to import loot without OP permissions!");
			sender.sendChatToPlayer(EnumChatFormatting.RED + "You need to be OP to edit loot!");
			return; // Player is not operator. Do nothing
		}
		
		LootRegistry.INSTANCE.readFromNBT(tag.getCompoundTag("data"), false);
		NetLootSync.sendSync(null);
	}
}
