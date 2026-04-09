package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api.utils.NBTConverter;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.NbtUtils;
import bq_standard.NbtBlockType;
import bq_standard.client.gui.tasks.PanelTaskBlockBreak;
import bq_standard.core.BQ_Standard;
import bq_standard.tasks.factory.FactoryTaskBlockBreak;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import betterquesting.backport.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TaskBlockBreak implements ITask
{
	private final Set<UUID> completeUsers = new TreeSet<UUID>();
	private final TreeMap<UUID, int[]> userProgress = new TreeMap<UUID, int[]>();
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
	public boolean isComplete(UUID uuid)
	{
		return completeUsers.contains(uuid);
	}
	
	@Override
	public void setComplete(UUID uuid)
	{
		completeUsers.add(uuid);
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

            int[] tmp = getUsersProgress(uuid);
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
			boolean oreMatch = targetBlock.oreDict.length() > 0 && OreDictionary.getOres(targetBlock.oreDict).contains(new ItemStack(block, 1, tmpMeta));
			final int index = i;
			
			if((oreMatch || (block == targetBlock.b && (targetBlock.m < 0 || meta == targetBlock.m))) && ItemComparison.CompareNBTTag(targetBlock.tags, tags, true))
			{
                for (Tuple2<UUID, int[]> entry : progress) {
                    if(entry.getSecond()[index] >= targetBlock.n) return;
                    entry.getSecond()[index]++;
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
	public void readProgressFromNBT(NBTTagCompound nbt, boolean merge)
	{
		if(!merge)
        {
            completeUsers.clear();
            userProgress.clear();
        }
		
		NBTTagList cList = NbtUtils.getTagList(nbt,"completeUsers", 8);
		for(int i = 0; i < cList.tagCount(); i++)
		{
			try
			{
				completeUsers.add(UUID.fromString(NbtUtils.getStringTagAt(cList, i)));
			} catch(Exception e)
			{
				BQ_Standard.logger.log(Level.SEVERE, "Unable to load UUID for task", e);
			}
		}
		
		NBTTagList pList = NbtUtils.getTagList(nbt,"userProgress", 10);
		for(int n = 0; n < pList.tagCount(); n++)
		{
			try
			{
                NBTTagCompound pTag = NbtUtils.getCompoundTagAt(pList, n);
                UUID uuid = UUID.fromString(pTag.getString("uuid"));
                
                int[] data = new int[blockTypes.size()];
                List<NBTBase> dNbt = NBTConverter.getTagList(NbtUtils.getTagList(pTag,"data", 3));
                for(int i = 0; i < data.length && i < dNbt.size(); i++) // TODO: Change this to an int array. This is dumb...
                {
                    data[i] = NbtUtils.intValue(dNbt.get(i));
                }
                
			    userProgress.put(uuid, data);
			} catch(Exception e)
			{
				BQ_Standard.logger.log(Level.SEVERE, "Unable to load user progress for task", e);
			}
		}
	}
	
	@Override
	public NBTTagCompound writeProgressToNBT(NBTTagCompound nbt, @Nullable List<UUID> users)
	{
		NBTTagList jArray = new NBTTagList();
		NBTTagList progArray = new NBTTagList();
		
		if(users != null)
        {
            for (UUID uuid : users) {
                if(completeUsers.contains(uuid)) jArray.appendTag(new NBTTagString(null, uuid.toString()));

                int[] data = userProgress.get(uuid);
                if(data != null)
                {
                    NBTTagCompound pJson = new NBTTagCompound();
                    pJson.setString("uuid", uuid.toString());
                    NBTTagList pArray = new NBTTagList(); // TODO: Why the heck isn't this just an int array?!
                    for(int i : data) pArray.appendTag(new NBTTagInt(null, i));
                    pJson.setTag("data", pArray);
                    progArray.appendTag(pJson);
                }
            }
        } else
        {
            for (UUID uuid : completeUsers) {
                jArray.appendTag(new NBTTagString(null, uuid.toString()));
            }
            for (Map.Entry<UUID, int[]> entry : userProgress.entrySet()) {
                UUID uuid = entry.getKey();
                int[] data = entry.getValue();
                NBTTagCompound pJson = new NBTTagCompound();
                pJson.setString("uuid", uuid.toString());
                NBTTagList pArray = new NBTTagList(); // TODO: Why the heck isn't this just an int array?!
                for(int i : data) pArray.appendTag(new NBTTagInt(null, i));
                pJson.setTag("data", pArray);
                progArray.appendTag(pJson);
            }
        }
		
		nbt.setTag("completeUsers", jArray);
		nbt.setTag("userProgress", progArray);
		
		return nbt;
	}
	
	@Override
	public void resetUser(@Nullable UUID uuid)
	{
	    if(uuid == null)
        {
            completeUsers.clear();
            userProgress.clear();
        } else
        {
            completeUsers.remove(uuid);
            userProgress.remove(uuid);
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
	
	private void setUserProgress(UUID uuid, int[] progress)
	{
		userProgress.put(uuid, progress);
	}
	
	public int[] getUsersProgress(UUID uuid)
	{
		int[] progress = userProgress.get(uuid);
		return progress == null || progress.length != blockTypes.size()? new int[blockTypes.size()] : progress;
	}
	
	private List<Tuple2<UUID, int[]>> getBulkProgress(@Nonnull List<UUID> uuids)
    {
        if(uuids.size() <= 0) return Collections.emptyList();
        List<Tuple2<UUID, int[]>> list = new ArrayList<Tuple2<UUID, int[]>>();
        for (UUID key : uuids) {
            list.add(new Tuple2<UUID, int[]>(key, getUsersProgress(key)));
        }
        return list;
    }
    
    private void setBulkProgress(@Nonnull List<Tuple2<UUID, int[]>> list)
    {
        for (Tuple2<UUID, int[]> entry : list) {
            setUserProgress(entry.getFirst(), entry.getSecond());
        }
    }
}
