package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.NbtUtils;
import betterquesting.backport.ResourceLocation;
import bq_standard.NbtBlockType;
import bq_standard.client.gui.tasks.PanelTaskBlockBreak;
import bq_standard.tasks.factory.FactoryTaskBlockBreak;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TaskBlockBreak extends IntArrayProgressTaskBase
{
	public final List<NbtBlockType> blockTypes = new ArrayList<NbtBlockType>();
	
	public TaskBlockBreak()
	{
		blockTypes.add(new NbtBlockType());
	}

    @Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskBlockBreak.INSTANCE.getRegistryName();
	}

    @Override
    public int getProgressDataSize() {
        return blockTypes.size();
    }

	@Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.block_break";
	}
	
	@Override
	public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
        for (UUID uuid : pInfo.ALL_UUIDS) {
            if(isComplete(uuid)) return;

            int[] tmp = getUserProgress(uuid);
            for(int i = 0; i < blockTypes.size(); i++)
            {
                NbtBlockType block = blockTypes.get(i);
                if(block != null && tmp[i] < block.n) return;
            }
            setComplete(uuid);
        }
	    
	    pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
	}
	
	public void onBlockBreak(ParticipantInfo pInfo, DBEntry<IQuest> quest, Block block, int meta, int x, int y, int z)
	{
		TileEntity tile = block.hasTileEntity(meta) ? pInfo.PLAYER.worldObj.getBlockTileEntity(x, y, z) : null;
		NBTTagCompound tags = null;
		if(tile != null)
        {
            tags = new NBTTagCompound();
            tile.writeToNBT(tags);
        }
		
		final List<Tuple2<UUID, int[]>> progress = getBulkProgress(pInfo.ALL_UUIDS);
		boolean changed = false;
		
		for(int i = 0; i < blockTypes.size(); i++)
		{
			NbtBlockType targetBlock = blockTypes.get(i);
			
			int tmpMeta = (targetBlock.m < 0 || targetBlock.m == OreDictionary.WILDCARD_VALUE)? OreDictionary.WILDCARD_VALUE : meta;
			boolean oreMatch = !targetBlock.oreDict.isEmpty() && OreDictionary.getOres(targetBlock.oreDict).contains(new ItemStack(block, 1, tmpMeta));
			
			if((oreMatch || (block == targetBlock.b && (targetBlock.m < 0 || meta == targetBlock.m))) && ItemComparison.CompareNBTTag(targetBlock.tags, tags, true))
			{
                for (Tuple2<UUID, int[]> entry : progress) {
                    if(entry.getSecond()[i] >= targetBlock.n) return;
                    entry.getSecond()[i]++;
                }
			    changed = true;
				break; // NOTE: We're only tracking one break at a time so doing all the progress setting above is fine
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
		NBTTagList bAry = new NBTTagList();
		for(NbtBlockType block : blockTypes)
		{
			bAry.appendTag(block.writeToNBT(new NBTTagCompound()));
		}
		nbt.setTag("blocks", bAry);
		
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		blockTypes.clear();
		NBTTagList bList = NbtUtils.getTagList(nbt,"blocks", 10);
		for(int i = 0; i < bList.tagCount(); i++)
		{
			NbtBlockType block = new NbtBlockType();
			block.readFromNBT(NbtUtils.getCompoundTagAt(bList,i));
			blockTypes.add(block);
		}
		
		if(NbtUtils.hasKey(nbt,"blockID", 8))
		{
            short blockId = nbt.getShort("blockID");
			Block targetBlock = blockId >= 0 && blockId <= Block.blocksList.length ? Block.blocksList[blockId] : null;
			targetBlock = targetBlock != null ? targetBlock : Block.wood;
			int targetMeta = nbt.getInteger("blockMeta");
			NBTTagCompound targetNbt = nbt.getCompoundTag("blockNBT");
			int targetNum = nbt.getInteger("amount");
			
			NbtBlockType leg = new NbtBlockType();
			leg.b = targetBlock;
			leg.m = targetMeta;
			leg.tags = targetNbt;
			leg.n = targetNum;
			
			blockTypes.add(leg);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskBlockBreak(rect, this);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen screen, DBEntry<IQuest> quest)
	{
		return null;
	}
}
