package bq_standard.network.handlers;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.Consumer;
import betterquesting.backport.NbtUtils;
import bq_standard.tasks.TaskCheckbox;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import betterquesting.backport.ResourceLocation;

public class NetTaskCheckbox
{
	private static final ResourceLocation ID_NAME = new ResourceLocation("bq_standard:task_checkbox");
	
	public static void registerHandler()
    {
        QuestingAPI.getAPI(ApiReference.PACKET_REG).registerServerHandler(ID_NAME, new Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>>() {
            @Override
            public void accept(Tuple2<NBTTagCompound, EntityPlayerMP> value) {
                NetTaskCheckbox.onServer(value);
            }
        });
    }
    
    @SideOnly(Side.CLIENT)
    public static void requestClick(int questID, int taskID)
    {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("questID", questID);
        payload.setInteger("taskID", taskID);
        QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToServer(new QuestingPacket(ID_NAME, payload));
    }
    
	private static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message)
	{
	    NBTTagCompound data = message.getFirst();
	    EntityPlayerMP sender = message.getSecond();
	    
		int qId = !NbtUtils.hasKey(data,"questID", 99)? -1 : data.getInteger("questID");
		int tId = !NbtUtils.hasKey(data,"taskID", 99)? -1 : data.getInteger("taskID");
		
		if(qId >= 0 && tId >= 0)
		{
            QuestCache qc = (QuestCache)sender.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
            IQuest quest = QuestingAPI.getAPI(ApiReference.QUEST_DB).getValue(qId);
            ITask task = quest == null ? null : quest.getTasks().getValue(tId);
            
            if(task instanceof TaskCheckbox)
            {
                task.setComplete(QuestingAPI.getQuestingUUID(sender));
                if(qc != null) qc.markQuestDirty(qId);
            }
		}
	}
}
