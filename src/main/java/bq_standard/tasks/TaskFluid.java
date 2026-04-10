package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.IFluidTask;
import betterquesting.api.questing.tasks.IItemTask;
import betterquesting.api.utils.JsonHelper;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.NbtUtils;
import betterquesting.backport.ResourceLocation;
import bq_standard.client.gui.tasks.PanelTaskFluid;
import bq_standard.tasks.factory.FactoryTaskFluid;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;

import javax.annotation.Nonnull;
import java.util.*;

public class TaskFluid extends IntArrayProgressTaskBase implements ITaskInventory, IFluidTask, IItemTask
{
	public final List<LiquidStack> requiredFluids = new ArrayList<LiquidStack>();

	//public boolean partialMatch = true; // Not many ideal ways of implementing this with fluid handlers
	public boolean ignoreNbt = false;
	public boolean consume = true;
	public boolean groupDetect = false;
	public boolean autoConsume = false;
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskFluid.INSTANCE.getRegistryName();
	}

    @Override
    public int getProgressDataSize() {
        return requiredFluids.size();
    }

    @Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.fluid";
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
	    
	    // Removing the consume check here would make the task cheaper on groups and for that reason sharing is restricted to detect only
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
                for(int i = 0; i < requiredFluids.size(); i++)
                {
                    final int r = requiredFluids.get(i).amount;
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
            // We do not support consuming resources from other member's invetories.
            // This could otherwise be abused to siphon items/fluids unknowingly
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
                if (!LiquidContainerRegistry.isContainer(stack)) continue;
                
                for(int j = 0; j < requiredFluids.size(); j++)
                {
                    final LiquidStack rStack = requiredFluids.get(j);
                    LiquidStack drainOG = rStack.copy();
                    if(ignoreNbt) drainOG.extra = null;
                    
                    // Pre-check
                    LiquidStack sample = getFluid(invo, i, false, drainOG.amount);
                    if(!drainOG.isLiquidEqual(sample)) continue;
                    
                    for(Tuple2<UUID, int[]> value : progress)
                    {
                        if(value.getSecond()[j] >= rStack.amount) continue;
                        int remaining = rStack.amount - value.getSecond()[j];

                        LiquidStack drain = rStack.copy();
                        drain.amount = remaining; //drain.amount = remaining / stack.stackSize;
                        if(ignoreNbt) drain.extra = null;
                        if(drain.amount <= 0) continue;

                        LiquidStack fluid = getFluid(invo, i, consume, drain.amount);
                        if(fluid == null || fluid.amount <= 0) continue;
            
                        value.getSecond()[j] += fluid.amount * stack.stackSize;
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
            for(int j = 0; j < requiredFluids.size(); j++)
            {
                if(value.getSecond()[j] >= requiredFluids.get(j).amount) continue;
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
	
	/**
	 * Returns the fluid drained (or can be drained) up to the specified amount
	 */
	private LiquidStack getFluid(InventoryPlayer invo, int slot, boolean drain, int amount)
	{
		ItemStack stack = invo.getStackInSlot(slot);
		
		if(stack == null || amount <= 0)
		{
			return null;
		}

        // TODO: Revise this math later
        LiquidStack fluid = LiquidContainerRegistry.getLiquidForFilledItem(stack);
        int tmp1 = fluid.amount;
        int tmp2 = 1;
        while(fluid.amount < amount && tmp2 < stack.stackSize)
        {
            tmp2++;
            fluid.amount += tmp1;
        }

        if(drain)
        {
            for(; tmp2 > 0; tmp2--)
            {
                ItemStack empty = stack.getItem().getContainerItemStack(stack);
                invo.decrStackSize(slot, 1);

                if(!invo.addItemStackToInventory(empty))
                {
                    invo.player.dropPlayerItemWithRandomChoice(empty, false);
                }
            }
        }

        return fluid;
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
	    //json.setBoolean("partialMatch", partialMatch);
		nbt.setBoolean("ignoreNBT", ignoreNbt);
		nbt.setBoolean("consume", consume);
		nbt.setBoolean("groupDetect", groupDetect);
		nbt.setBoolean("autoConsume", autoConsume);
		
		NBTTagList itemArray = new NBTTagList();
		for(LiquidStack stack : this.requiredFluids)
		{
			itemArray.appendTag(stack.writeToNBT(new NBTTagCompound()));
		}
		nbt.setTag("requiredFluids", itemArray);
		
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
	    //partialMatch = json.getBoolean("partialMatch");
		ignoreNbt = nbt.getBoolean("ignoreNBT");
		consume = nbt.getBoolean("consume");
		groupDetect = nbt.getBoolean("groupDetect");
		autoConsume = nbt.getBoolean("autoConsume");
		
		requiredFluids.clear();
		NBTTagList fList = NbtUtils.getTagList(nbt,"requiredFluids", 10);
		for(int i = 0; i < fList.tagCount(); i++)
		{
			requiredFluids.add(JsonHelper.JsonToFluidStack(NbtUtils.getCompoundTagAt(fList, i)));
		}
	}
 
	@Override
	@SideOnly(Side.CLIENT)
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskFluid(rect, this);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen screen, DBEntry<IQuest> quest)
	{
		return null;
	}

	@Override
	public boolean canAcceptFluid(UUID owner, DBEntry<IQuest> quest, LiquidStack fluid)
	{
		if(owner == null || fluid == null || !consume || isComplete(owner) || requiredFluids.size() <= 0)
		{
			return false;
		}
		
		int[] progress = getUserProgress(owner);
		
		for(int j = 0; j < requiredFluids.size(); j++)
		{
            LiquidStack rStack = requiredFluids.get(j).copy();
			if(ignoreNbt) rStack.extra = null;
			if(progress[j] < rStack.amount && rStack.equals(fluid)) return true;
		}
		
		return false;
	}

	@Override
	public boolean canAcceptItem(UUID owner, DBEntry<IQuest> quest, ItemStack item)
	{
		if(owner == null || item == null || !consume || isComplete(owner) || requiredFluids.size() <= 0)
		{
			return false;
		}
		
		if(LiquidContainerRegistry.isFilledContainer(item))
        {
            return canAcceptFluid(owner, quest, LiquidContainerRegistry.getLiquidForFilledItem(item));
        }
		
		return false;
	}

	@Override
	public LiquidStack submitFluid(UUID owner, DBEntry<IQuest> quest, LiquidStack fluid)
	{
		if(owner == null || fluid == null || fluid.amount <= 0 || !consume || isComplete(owner) || requiredFluids.size() <= 0)
		{
			return fluid;
		}
		
		int[] progress = getUserProgress(owner);
		
		for(int j = 0; j < requiredFluids.size(); j++)
		{
            LiquidStack rStack = requiredFluids.get(j);
			
			if(progress[j] >= rStack.amount) continue;
			
			int remaining = rStack.amount - progress[j];
			
			if(rStack.isLiquidEqual(fluid))
			{
				int removed = Math.min(fluid.amount, remaining);
				progress[j] += removed;
				fluid.amount -= removed;
				
				if(fluid.amount <= 0)
				{
					fluid = null;
					break;
				}
			}
		}
		
		if(consume)
		{
			setUserProgress(owner, progress);
		}
		
		return fluid;
	}

	@Override
	public ItemStack submitItem(UUID owner, DBEntry<IQuest> quest, ItemStack input)
	{
		if(owner == null || input == null || !consume || isComplete(owner)) return input;
		
		ItemStack item = input.splitStack(1); // Prevents issues with stack filling/draining
        
        if(LiquidContainerRegistry.isFilledContainer(item))
        {
            LiquidStack fluid = LiquidContainerRegistry.getLiquidForFilledItem(item);
            submitFluid(owner, quest, fluid);
            return item.getItem().getContainerItemStack(item);
        }
		
		return item;
	}
}
