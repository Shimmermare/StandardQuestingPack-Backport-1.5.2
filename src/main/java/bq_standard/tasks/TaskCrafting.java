package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api.utils.JsonHelper;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.NbtUtils;
import betterquesting.backport.ResourceLocation;
import bq_standard.client.gui.tasks.PanelTaskCrafting;
import bq_standard.tasks.factory.FactoryTaskCrafting;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TaskCrafting extends IntArrayProgressTaskBase
{
	public final List<BigItemStack> requiredItems = new ArrayList<BigItemStack>();

	public boolean partialMatch = true;
	public boolean ignoreNBT = false;
    // FIXME: Disabled because to detect anvil crafting a coremod is needed
	public final boolean allowAnvil = false;
	public boolean allowSmelt = true;
	public boolean allowCraft = true;

	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskCrafting.INSTANCE.getRegistryName();
	}

    @Override
    public int getProgressDataSize() {
        return requiredItems.size();
    }

    @Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.crafting";
	}
	
	@Override
	public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
        for (UUID uuid : pInfo.ALL_UUIDS) {
            if(isComplete(uuid)) return;
            
            int[] tmp = getUserProgress(uuid);
            for(int i = 0; i < requiredItems.size(); i++)
            {
                BigItemStack rStack = requiredItems.get(i);
                if(tmp[i] < rStack.stackSize) return;
            }
            setComplete(uuid);
        }
	    
	    pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
	}
	
	public void onItemCraft(ParticipantInfo pInfo, DBEntry<IQuest> quest, ItemStack stack)
    {
        if(!allowCraft) return;
        onItemInternal(pInfo, quest, stack);
    }
	
	public void onItemSmelt(ParticipantInfo pInfo, DBEntry<IQuest> quest, ItemStack stack)
    {
        if(!allowSmelt) return;
        onItemInternal(pInfo, quest, stack);
    }
	
	public void onItemAnvil(ParticipantInfo pInfo, DBEntry<IQuest> quest, ItemStack stack)
    {
        if(!allowAnvil) return;
        onItemInternal(pInfo, quest, stack);
    }
	
	private void onItemInternal(ParticipantInfo pInfo, DBEntry<IQuest> quest, ItemStack stack)
	{
	    if(stack == null || stack.stackSize <= 0) return;
		
        final List<Tuple2<UUID, int[]>> progress = getBulkProgress(pInfo.ALL_UUIDS);
        boolean changed = false;
        
		for(int i = 0; i < requiredItems.size(); i++)
		{
			final BigItemStack rStack = requiredItems.get(i);
			
			if(ItemComparison.StackMatch(rStack.getBaseStack(), stack, !ignoreNBT, partialMatch) || ItemComparison.OreDictionaryMatch(rStack.getOreIngredient(), rStack.GetTagCompound(), stack, !ignoreNBT, partialMatch))
			{
                for (Tuple2<UUID, int[]> entry : progress) {
                    if(entry.getSecond()[i] >= rStack.stackSize) return;
                    entry.getSecond()[i] = Math.min(entry.getSecond()[i] + stack.stackSize, rStack.stackSize);
                }
			    changed = true;
			}
		}
		
		if(changed)
        {
		    setBulkProgress(progress);
            detect(pInfo, quest);
        }
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setBoolean("partialMatch", partialMatch);
		nbt.setBoolean("ignoreNBT", ignoreNBT);
		nbt.setBoolean("allowCraft", allowCraft);
		nbt.setBoolean("allowSmelt", allowSmelt);
		//nbt.setBoolean("allowAnvil", allowAnvil);
		
		NBTTagList itemArray = new NBTTagList();
		for(BigItemStack stack : this.requiredItems)
		{
			itemArray.appendTag(JsonHelper.ItemStackToJson(stack, new NBTTagCompound()));
		}
		nbt.setTag("requiredItems", itemArray);
		
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		partialMatch = nbt.getBoolean("partialMatch");
		ignoreNBT = nbt.getBoolean("ignoreNBT");
		if(nbt.hasKey("allowCraft")) allowCraft = nbt.getBoolean("allowCraft");
		if(nbt.hasKey("allowSmelt")) allowSmelt = nbt.getBoolean("allowSmelt");
		//if(nbt.hasKey("allowAnvil")) allowAnvil = nbt.getBoolean("allowAnvil");
		
		requiredItems.clear();
		NBTTagList iList = NbtUtils.getTagList(nbt,"requiredItems", 10);
		for(int i = 0; i < iList.tagCount(); i++)
		{
		    requiredItems.add(JsonHelper.JsonToItemStack(NbtUtils.getCompoundTagAt(iList, i)));
		}
	}
 
	@Override
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> context)
	{
	    return new PanelTaskCrafting(rect, this);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen parent, DBEntry<IQuest> quest)
	{
		return null;
	}
}
