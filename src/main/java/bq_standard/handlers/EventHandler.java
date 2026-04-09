package bq_standard.handlers;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.events.BQLivingUpdateEvent;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import bq_standard.network.handlers.NetLootSync;
import bq_standard.tasks.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.WorldEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

@SuppressWarnings("unused")
public class EventHandler
{
    @ForgeSubscribe(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.entityPlayer == null || event.entityPlayer.worldObj.isRemote || event.isCanceled()) return;

		EntityPlayer player = event.entityPlayer;
        ParticipantInfo pInfo = new ParticipantInfo(player);

        int blockId = player.worldObj.getBlockId(event.x, event.y, event.z);
		Block block = Block.blocksList[blockId];
		int meta = player.worldObj.getBlockMetadata(event.x, event.y, event.z);
		boolean isHit = event.action == Action.LEFT_CLICK_BLOCK;

		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof TaskInteractItem) ((TaskInteractItem)task.getValue()).onInteract(pInfo, entry, player.getHeldItem(), block, meta, event.x, event.y, event.z, isHit);
            }
		}
    }

    @ForgeSubscribe(priority = EventPriority.LOWEST)
    public void onEntityAttack(AttackEntityEvent event)
    {
        if(event.entityPlayer == null || event.target == null || event.entityPlayer.worldObj.isRemote || event.isCanceled()) return;

		EntityPlayer player = event.entityPlayer;
        ParticipantInfo pInfo = new ParticipantInfo(player);

		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof TaskInteractEntity) ((TaskInteractEntity)task.getValue()).onInteract(pInfo, entry, player.getHeldItem(), event.target, true);
            }
		}
    }

    @ForgeSubscribe(priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event)
    {
        if(event.entityPlayer == null || event.target == null || event.entityPlayer.worldObj.isRemote || event.isCanceled()) return;

		EntityPlayer player = event.entityPlayer;
        ParticipantInfo pInfo = new ParticipantInfo(player);

		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof TaskInteractEntity) ((TaskInteractEntity)task.getValue()).onInteract(pInfo, entry, player.getHeldItem(), event.target, false);
            }
		}
    }

    // FIXME idk how to replace yet
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemAnvil(AnvilRepairEvent event) // Somehow actually works as intended unlike other crafting methods
	{
		if(event.entityPlayer == null || event.entityPlayer.worldObj.isRemote) return;

        ParticipantInfo pInfo = new ParticipantInfo(event.entityPlayer);

		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof TaskCrafting) ((TaskCrafting)task.getValue()).onItemAnvil(pInfo, entry, event.output.copy());
            }
		}
	}

	@ForgeSubscribe(priority = EventPriority.LOWEST)
	public void onEntityKilled(LivingDeathEvent event)
	{
		if(event.source == null || !(event.source.getEntity() instanceof EntityPlayer) || event.source.getEntity().worldObj.isRemote || event.isCanceled()) return;

		EntityPlayer player = (EntityPlayer)event.source.getEntity();
        ParticipantInfo pInfo = new ParticipantInfo(player);

		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof TaskHunt) ((TaskHunt)task.getValue()).onKilledByPlayer(pInfo, entry, event.entityLiving, event.source);
            }
		}
	}

    /**
     * This is the best replacement for BreakEvent I could find.
     * If another mod fires this but doesn't break the block, we're screwed.
     */
	@ForgeSubscribe(priority = EventPriority.LOWEST)
	public void onHarvestCheck(PlayerEvent.HarvestCheck event)
	{
		if(event.entityPlayer == null || event.entityPlayer.worldObj.isRemote || event.isCanceled()) return;

        ParticipantInfo pInfo = new ParticipantInfo(event.entityPlayer);
        List<Tuple2<DBEntry<IQuest>, TaskBlockBreak>> blockBreakTasks = new ArrayList<Tuple2<DBEntry<IQuest>, TaskBlockBreak>>();
		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof TaskBlockBreak) {
                    blockBreakTasks.add(new Tuple2<DBEntry<IQuest>, TaskBlockBreak>(entry, (TaskBlockBreak)task.getValue()));
                }
            }
		}
        if (blockBreakTasks.isEmpty()) return;

        int x = 0;
        int y = 0;
        int z = 0;
        int meta = 0;
        MovingObjectPosition mop = event.entityPlayer.rayTrace(5.0D, 1.0F);
        if (mop != null && mop.typeOfHit == EnumMovingObjectType.TILE) {
            x = mop.blockX;
            y = mop.blockY;
            z = mop.blockZ;

            // Verify the raytraced block ID matches the block ID from the event
            if (event.entityPlayer.worldObj.getBlockId(x, y, z) == event.block.blockID) {
                meta = event.entityPlayer.worldObj.getBlockMetadata(x, y, z);
            }
        }

        for (Tuple2<DBEntry<IQuest>, TaskBlockBreak> task : blockBreakTasks) {
            task.getSecond().onBlockBreak(pInfo, task.getFirst(), event.block, meta, x, y, z);
        }
	}

	@ForgeSubscribe
    public void onEntityLiving(BQLivingUpdateEvent event)
    {
        if(!(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote || event.entityLiving.ticksExisted%20 != 0 || QuestingAPI.getAPI(ApiReference.SETTINGS).getProperty(NativeProps.EDIT_MODE)) return;

        EntityPlayer player = (EntityPlayer)event.entityLiving;
        ParticipantInfo pInfo = new ParticipantInfo(player);

		for(DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests()))
		{
		    for(DBEntry<ITask> task : entry.getValue().getTasks().getEntries())
            {
                if(task.getValue() instanceof ITaskTickable)
                {
                    ((ITaskTickable)task.getValue()).tickTask(pInfo, entry);
                }
            }
		}
    }

    @ForgeSubscribe
    public void onEntityCreated(EntityJoinWorldEvent event)
    {
        if(!(event.entity instanceof EntityPlayer) || event.entity.worldObj.isRemote) return;

		PlayerContainerListener.refreshListener((EntityPlayer)event.entity);
    }

    // FIXME replace with IPlayerTracker
	@SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event)
    {
		if(!event.player.worldObj.isRemote && event.player instanceof EntityPlayerMP)
		{
            NetLootSync.sendSync((EntityPlayerMP)event.player);
		}
    }

	@ForgeSubscribe
    public void onWorldSave(WorldEvent.Save event)
    {
        if(!event.world.isRemote && LootSaveLoad.INSTANCE.worldDir != null && event.world.provider.dimensionId == 0)
        {
            LootSaveLoad.INSTANCE.SaveLoot();
        }
    }

	private static final ArrayDeque<FutureTask> serverTasks = new ArrayDeque<FutureTask>();
	private static Thread serverThread = null;

    // FIXME move out to ITickHandler
	// NOTE: This is slightly different to the version in the base mod. This one will not immediately run tasks even if it's from the same thread.
    public static <T> ListenableFuture<T> scheduleServerTask(Callable<T> task)
    {
        if (task == null) {
            throw new NullPointerException("task cannot be null");
        }

        ListenableFutureTask<T> listenablefuturetask = ListenableFutureTask.create(task);

        synchronized (serverTasks)
        {
            serverTasks.add(listenablefuturetask);
            return listenablefuturetask;
        }
    }

	@SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
        if(event.phase != Phase.START) return;
        if(serverThread == null) serverThread = Thread.currentThread();

        synchronized(serverTasks)
        {
            while(!serverTasks.isEmpty()) serverTasks.poll().run();
        }
    }
}
