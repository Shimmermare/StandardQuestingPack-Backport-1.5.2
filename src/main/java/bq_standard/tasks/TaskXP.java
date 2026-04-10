package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.backport.NbtUtils;
import betterquesting.backport.ResourceLocation;
import bq_standard.XPHelper;
import bq_standard.client.gui.tasks.PanelTaskXP;
import bq_standard.tasks.factory.FactoryTaskXP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import java.util.Collections;

public class TaskXP extends IntProgressTaskBase implements ITaskTickable
{
	public boolean levels = true;
	public int amount = 30;
	public boolean consume = true;
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskXP.INSTANCE.getRegistryName();
	}

	@Override
	public void tickTask(@Nonnull ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
	    if(consume || pInfo.PLAYER.ticksExisted%60 != 0) return; // Every 3 seconds
        
        int curProg = getUserProgress(pInfo.UUID);
        int nxtProg = (int) XPHelper.getPlayerXP(pInfo.PLAYER);
        
        if(curProg != nxtProg)
        {
            setUserProgress(pInfo.UUID, (int) XPHelper.getPlayerXP(pInfo.PLAYER));
            pInfo.markDirty(Collections.singletonList(quest.getID()));
        }

        int rawXP = levels? (int) XPHelper.getLevelXP(amount) : amount;
        int totalXP = getUserProgress(pInfo.UUID);
        
        if(totalXP >= rawXP) setComplete(pInfo.UUID);
	}
	
	@Override
	public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
		if(isComplete(pInfo.UUID)) return;

        int progress = getUserProgress(pInfo.UUID);
        int rawXP = levels? (int) XPHelper.getLevelXP(amount) : amount;
        int plrXP = (int) XPHelper.getPlayerXP(pInfo.PLAYER);
        int remaining = rawXP - progress;
        int cost = Math.min(remaining, plrXP);
		
		boolean changed = false;
		
		if(consume && cost != 0)
        {
            progress += cost;
            setUserProgress(pInfo.UUID, progress);
            XPHelper.addXP(pInfo.PLAYER, -cost);
            changed = true;
		} else if(!consume && progress != plrXP)
        {
            setUserProgress(pInfo.UUID, plrXP);
            changed = true;
        }
		
		int totalXP = getUserProgress(pInfo.UUID);
		
		if(totalXP >= rawXP)
        {
            setComplete(pInfo.UUID);
            changed = true;
        }
		
		if(changed) // Needs to be here because even if no additional progress was added, a party memeber may have completed the task anyway
        {
            pInfo.markDirty(Collections.singletonList(quest.getID()));
        }
	}
	
	@Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.xp";
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setInteger("amount", amount);
		nbt.setBoolean("isLevels", levels);
		nbt.setBoolean("consume", consume);
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		amount = NbtUtils.hasKey(nbt,"amount", 99) ? nbt.getInteger("amount") : 30;
		levels = nbt.getBoolean("isLevels");
		consume = nbt.getBoolean("consume");
	}
	
	@Override
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskXP(rect, this);
	}
	
	@Override
	public GuiScreen getTaskEditor(GuiScreen screen, DBEntry<IQuest> quest)
	{
		return null;
	}
}
