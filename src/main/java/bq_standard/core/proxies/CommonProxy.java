package bq_standard.core.proxies;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.registry.IFactoryData;
import betterquesting.api2.registry.IRegistry;
import bq_standard.core.BQ_Standard;
import bq_standard.handlers.BQSCraftingHandler;
import bq_standard.handlers.BQSPlayerTracker;
import bq_standard.handlers.EventHandler;
import bq_standard.handlers.ServerTaskExecutor;
import bq_standard.network.handlers.*;
import bq_standard.rewards.factory.*;
import bq_standard.rewards.loot.LootRegistry;
import bq_standard.tasks.factory.*;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy
{
	public boolean isClient()
	{
		return false;
	}
	
	public void registerHandlers()
	{
		MinecraftForge.EVENT_BUS.register(LootRegistry.INSTANCE);
		MinecraftForge.EVENT_BUS.register(new EventHandler());
        GameRegistry.registerCraftingHandler(new BQSCraftingHandler());
        GameRegistry.registerPlayerTracker(new BQSPlayerTracker());
        TickRegistry.registerTickHandler(new ServerTaskExecutor(), Side.SERVER);
	}
	
	public void registerRenderers()
	{
	}
	
	public void registerExpansion()
	{
		IRegistry<IFactoryData<ITask, NBTTagCompound>, ITask> taskReg = QuestingAPI.getAPI(ApiReference.TASK_REG);
		taskReg.register(FactoryTaskBlockBreak.INSTANCE);
		taskReg.register(FactoryTaskCheckbox.INSTANCE);
		taskReg.register(FactoryTaskCrafting.INSTANCE);
		taskReg.register(FactoryTaskFluid.INSTANCE);
		taskReg.register(FactoryTaskHunt.INSTANCE);
		taskReg.register(FactoryTaskLocation.INSTANCE);
		taskReg.register(FactoryTaskMeeting.INSTANCE);
		taskReg.register(FactoryTaskRetrieval.INSTANCE);
		taskReg.register(FactoryTaskXP.INSTANCE);
		taskReg.register(FactoryTaskInteractItem.INSTANCE);
		taskReg.register(FactoryTaskInteractEntity.INSTANCE);

		IRegistry<IFactoryData<IReward, NBTTagCompound>, IReward> rewardReg = QuestingAPI.getAPI(ApiReference.REWARD_REG);
		rewardReg.register(FactoryRewardChoice.INSTANCE);
		rewardReg.register(FactoryRewardCommand.INSTANCE);
		rewardReg.register(FactoryRewardItem.INSTANCE);
		rewardReg.register(FactoryRewardXP.INSTANCE);
		
		NetLootSync.registerHandler();
		NetLootClaim.registerHandler();
		NetTaskCheckbox.registerHandler();
		NetRewardChoice.registerHandler();
		NetLootImport.registerHandler();
		NetTaskInteract.registerHandler();
		
		BQ_Standard.lootChest.setCreativeTab(QuestingAPI.getAPI(ApiReference.CREATIVE_TAB));
	}
}
