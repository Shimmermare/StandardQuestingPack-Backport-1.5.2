package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.ResourceLocation;
import bq_standard.client.gui.editors.tasks.GuiEditTaskHunt;
import bq_standard.client.gui.tasks.PanelTaskHunt;
import bq_standard.core.BQ_Standard;
import bq_standard.tasks.factory.FactoryTaskHunt;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TaskHunt extends IntProgressTaskBase
{
	public String idName = "Zombie";
	public String damageType = "";
	public int required = 1;
	public boolean ignoreNBT = true;
	public boolean subtypes = true;
	
	/**
	 * NBT representation of the intended target. Used only for NBT comparison checks
	 */
	public NBTTagCompound targetTags = new NBTTagCompound();
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskHunt.INSTANCE.getRegistryName();
	}

	
	@Override
	public String getUnlocalisedName()
	{
		return BQ_Standard.MODID + ".task.hunt";
	}
	
	@Override
	public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
        final List<Tuple2<UUID, Integer>> progress = getBulkProgress(pInfo.ALL_UUIDS);

        for (Tuple2<UUID, Integer> value : progress) {
            if(value.getSecond() >= required) setComplete(value.getFirst());
        }
        
		pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
	}
	
	@SuppressWarnings("unchecked")
	public void onKilledByPlayer(ParticipantInfo pInfo, DBEntry<IQuest> quest, EntityLiving entity, DamageSource source)
	{
		if(!damageType.isEmpty() && (source == null || !damageType.equalsIgnoreCase(source.damageType))) return;
		
		Class<? extends Entity> subject = entity.getClass();
		Class<? extends Entity> target = (Class<? extends Entity>)EntityList.stringToClassMapping.get(idName);
		String subjectID = EntityList.getEntityString(entity);
		
		if(subjectID == null || target == null)
		{
			return; // Missing necessary data
		} else if(subtypes && !target.isAssignableFrom(subject))
		{
			return; // This is not the intended target or sub-type
		} else if(!subtypes && !subjectID.equals(idName))
		{
			return; // This isn't the exact target required
		}
		
		NBTTagCompound subjectTags = new NBTTagCompound();
		entity.addEntityID(subjectTags);
		if(!ignoreNBT && !ItemComparison.CompareNBTTag(targetTags, subjectTags, true)) return;
		
		final List<Tuple2<UUID, Integer>> progress = getBulkProgress(pInfo.ALL_UUIDS);

        for (Tuple2<UUID, Integer> value : progress) {
            if(isComplete(value.getFirst())) return;
            int np = Math.min(required, value.getSecond() + 1);
            setUserProgress(value.getFirst(), np);
            if(np >= required) setComplete(value.getFirst());
        }
        
		pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setString("target", idName);
		nbt.setInteger("required", required);
		nbt.setBoolean("subtypes", subtypes);
		nbt.setBoolean("ignoreNBT", ignoreNBT);
		nbt.setTag("targetNBT", targetTags);
		nbt.setString("damageType", damageType);
		
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		idName = nbt.getString("target");
		required = nbt.getInteger("required");
		subtypes = nbt.getBoolean("subtypes");
		ignoreNBT = nbt.getBoolean("ignoreNBT");
		targetTags = nbt.getCompoundTag("targetNBT");
		damageType = nbt.getString("damageType");
	}
	
	/**
	 * Returns a new editor screen for this Reward type to edit the given data
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen parent, DBEntry<IQuest> quest)
	{
	    return new GuiEditTaskHunt(parent, quest, this);
	}
 
	@Override
	@SideOnly(Side.CLIENT)
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskHunt(rect, this);
	}
}
