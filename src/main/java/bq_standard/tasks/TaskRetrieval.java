package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.IItemTask;
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
import bq_standard.client.gui.tasks.PanelTaskRetrieval;
import bq_standard.core.BQ_Standard;
import bq_standard.tasks.factory.FactoryTaskRetrieval;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nonnull;
import java.util.*;

public class TaskRetrieval extends IntArrayProgressTaskBase implements ITaskInventory, IItemTask
{
	public final List<BigItemStack> requiredItems = new ArrayList<BigItemStack>();

	public boolean partialMatch = true;
	public boolean ignoreNBT = false;
	public boolean consume = true;
	public boolean groupDetect = false;
	public boolean autoConsume = false;
	
	@Override
	public String getUnlocalisedName()
	{
		return BQ_Standard.MODID + ".task.retrieval";
	}
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskRetrieval.INSTANCE.getRegistryName();
	}

    @Override
    public int getProgressDataSize() {
        return requiredItems.size();
    }

    @Override
	public void onInventoryChange(@Nonnull DBEntry<IQuest> quest, @Nonnull ParticipantInfo pInfo)
    {
        if(!consume || autoConsume)
        {
            detect(pInfo, quest);
        }
    }
    
	@Override
	public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
		if(isComplete(pInfo.UUID)) return;
		
        final List<Tuple2<UUID, int[]>> progress = getBulkProgress(consume ? Collections.singletonList(pInfo.UUID) : pInfo.ALL_UUIDS);
		boolean updated = false;
		
		if(!consume)
        {
            if(groupDetect) // Reset all detect progress
            {
                for (Tuple2<UUID, int[]> value : progress) {
                    Arrays.fill(value.getSecond(), 0);
                }
            } else
            {
                for(int i = 0; i < requiredItems.size(); i++)
                {
                    final int r = requiredItems.get(i).stackSize;
                    for(Tuple2<UUID, int[]> value : progress)
                    {
                        int n = value.getSecond()[i];
                        if(n != 0 && n < r)
                        {
                            value.getSecond()[i] = 0;
                            updated = true;
                        }
                    }
                }
            }
        }
		
		final List<InventoryPlayer> invoList;
		if(consume)
        {
            invoList = Collections.singletonList(pInfo.PLAYER.inventory);
        } else
        {
            invoList = new ArrayList<InventoryPlayer>();
            for (EntityPlayer p : pInfo.ACTIVE_PLAYERS) {
                invoList.add(p.inventory);
            }
        }
		
		for(InventoryPlayer invo : invoList)
        {
            for(int i = 0; i < invo.getSizeInventory(); i++)
            {
                ItemStack stack = invo.getStackInSlot(i);
                if(stack == null || stack.stackSize <= 0) continue;
                // Allows the stack detection to split across multiple requirements. Counts may vary per person
                int[] remCounts = new int[progress.size()];
                Arrays.fill(remCounts, stack.stackSize);
                
                for(int j = 0; j < requiredItems.size(); j++)
                {
                    BigItemStack rStack = requiredItems.get(j);
        
                    if(!ItemComparison.StackMatch(rStack.getBaseStack(), stack, !ignoreNBT, partialMatch) && !ItemComparison.OreDictionaryMatch(rStack.getOreIngredient(), rStack.GetTagCompound(), stack, !ignoreNBT, partialMatch))
                    {
                        continue;
                    }
                    
                    for(int n = 0; n < progress.size(); n++)
                    {
                        Tuple2<UUID, int[]> value = progress.get(n);
                        if(value.getSecond()[j] >= rStack.stackSize) continue;
                        
                        int remaining = rStack.stackSize - value.getSecond()[j];
                        
                        if(consume)
                        {
                            ItemStack removed = invo.decrStackSize(i, remaining);
                            value.getSecond()[j] += removed.stackSize;
                        } else
                        {
                            int temp = Math.min(remaining, remCounts[n]);
                            remCounts[n] -= temp;
                            value.getSecond()[j] += temp;
                        }
        
                        updated = true;
                    }
                }
            }
        }
		
		if(updated) setBulkProgress(progress);
		checkAndComplete(pInfo, quest, updated);
	}
	
	private void checkAndComplete(ParticipantInfo pInfo, DBEntry<IQuest> quest, boolean resync)
    {
        final List<Tuple2<UUID, int[]>> progress = getBulkProgress(consume ? Collections.singletonList(pInfo.UUID) : pInfo.ALL_UUIDS);
        boolean updated = resync;
        
        topLoop:
        for(Tuple2<UUID, int[]> value : progress)
        {
            for(int j = 0; j < requiredItems.size(); j++)
            {
                if(value.getSecond()[j] >= requiredItems.get(j).stackSize) continue;
                continue topLoop;
            }
            
            updated = true;
            
            if(consume)
            {
                setComplete(value.getFirst());
            } else
            {
                for (Tuple2<UUID, int[]> pair : progress) {
                    setComplete(pair.getFirst());
                }
                break;
            }
        }
		
		if(updated)
        {
            if(consume)
            {
                pInfo.markDirty(Collections.singletonList(quest.getID()));
            } else
            {
                pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
            }
        }
    }

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setBoolean("partialMatch", partialMatch);
		nbt.setBoolean("ignoreNBT", ignoreNBT);
		nbt.setBoolean("consume", consume);
		nbt.setBoolean("groupDetect", groupDetect);
		nbt.setBoolean("autoConsume", autoConsume);
		
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
		consume = nbt.getBoolean("consume");
		groupDetect = nbt.getBoolean("groupDetect");
		autoConsume = nbt.getBoolean("autoConsume");
		
		requiredItems.clear();
		NBTTagList iList = NbtUtils.getTagList(nbt, "requiredItems", 10);
		for(int i = 0; i < iList.tagCount(); i++)
		{
			requiredItems.add(JsonHelper.JsonToItemStack(NbtUtils.getCompoundTagAt(iList, i)));
		}
	}

	@Override
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskRetrieval(rect, this);
	}
	
	@Override
	public boolean canAcceptItem(UUID owner, DBEntry<IQuest> quest, ItemStack stack)
	{
		if(owner == null || stack == null || stack.stackSize <= 0 || !consume || isComplete(owner) || requiredItems.size() <= 0)
		{
			return false;
		}
		
		int[] progress = getUserProgress(owner);
		
		for(int j = 0; j < requiredItems.size(); j++)
		{
			BigItemStack rStack = requiredItems.get(j);
			
			if(progress[j] >= rStack.stackSize) continue;
			
			if(ItemComparison.StackMatch(rStack.getBaseStack(), stack, !ignoreNBT, partialMatch) || ItemComparison.OreDictionaryMatch(rStack.getOreIngredient(), rStack.GetTagCompound(), stack, !ignoreNBT, partialMatch))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public ItemStack submitItem(UUID owner, DBEntry<IQuest> quest, ItemStack input)
	{
		if(owner == null || input == null || !consume || isComplete(owner)) return input;
		
		ItemStack stack = input.copy();
		
		int[] progress = getUserProgress(owner);
		boolean updated = false;
		
		for(int j = 0; j < requiredItems.size(); j++)
		{
			BigItemStack rStack = requiredItems.get(j);
			
			if(progress[j] >= rStack.stackSize) continue;

			int remaining = rStack.stackSize - progress[j];
			
			if(ItemComparison.StackMatch(rStack.getBaseStack(), stack, !ignoreNBT, partialMatch) || ItemComparison.OreDictionaryMatch(rStack.getOreIngredient(), rStack.GetTagCompound(), stack, !ignoreNBT, partialMatch))
			{
				int removed = Math.min(stack.stackSize, remaining);
				stack.stackSize -= removed;
				progress[j] += removed;
				updated = true;
				if(stack.stackSize <= 0)
                {
                    stack = null;
                    break;
                }
			}
		}
		
		if(updated)
        {
            setUserProgress(owner, progress);
        }
		
		return stack;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen parent, DBEntry<IQuest> quest)
	{
		return null;
	}
}
