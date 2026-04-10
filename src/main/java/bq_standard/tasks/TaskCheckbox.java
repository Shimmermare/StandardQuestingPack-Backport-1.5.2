package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.backport.ResourceLocation;
import bq_standard.client.gui.tasks.PanelTaskCheckbox;
import bq_standard.core.BQ_Standard;
import bq_standard.tasks.factory.FactoryTaskCheckbox;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;

public class TaskCheckbox extends NoProgressTaskBase
{
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskCheckbox.INSTANCE.getRegistryName();
	}
	
	@Override
	public String getUnlocalisedName()
	{
		return BQ_Standard.MODID + ".task.checkbox";
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
	}

	@Override
	public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskCheckbox(rect, quest, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen parent, DBEntry<IQuest> quest)
	{
		return null;
	}
}
