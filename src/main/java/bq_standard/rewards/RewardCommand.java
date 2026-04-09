package bq_standard.rewards;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import bq_standard.AdminExecute;
import bq_standard.client.gui.rewards.PanelRewardCommand;
import bq_standard.handlers.EventHandler;
import bq_standard.rewards.factory.FactoryRewardCommand;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import betterquesting.backport.ResourceLocation;

import java.util.concurrent.Callable;

public class RewardCommand implements IReward
{
	public String command = "/say VAR_NAME Claimed a reward";
	public boolean hideCmd = false;
	public boolean viaPlayer = false;
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryRewardCommand.INSTANCE.getRegistryName();
	}
	
	@Override
	public String getUnlocalisedName()
	{
		return "bq_standard.reward.command";
	}
	
	@Override
	public boolean canClaim(EntityPlayer player, DBEntry<IQuest> quest)
	{
		return true;
	}
	
	@Override
	public void claimReward(final EntityPlayer player, DBEntry<IQuest> quest)
	{
		if(player.worldObj.isRemote) return;
		
		String tmp = command.replaceAll("VAR_NAME", player.getCommandSenderName());
		final String finCom = tmp.replaceAll("VAR_UUID", QuestingAPI.getQuestingUUID(player).toString());
		final MinecraftServer server = MinecraftServer.getServer();
		
		if(viaPlayer)
		{
			EventHandler.scheduleServerTask(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return server.getCommandManager().executeCommand(new AdminExecute(player), finCom);
                }
            });
		} else
		{
			final RewardCommandSender cmdSender = new RewardCommandSender((int)player.posX, (int)player.posY, (int)player.posZ);
			
			EventHandler.scheduleServerTask(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return server.getCommandManager().executeCommand(cmdSender, finCom);
                }
            });
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		command = nbt.getString("command");
		hideCmd = nbt.getBoolean("hideCommand");
		viaPlayer = nbt.getBoolean("viaPlayer");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setString("command", command);
		nbt.setBoolean("hideCommand", hideCmd);
		nbt.setBoolean("viaPlayer", viaPlayer);
		return nbt;
	}
	
	@Override
	public IGuiPanel getRewardGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelRewardCommand(rect, this);
	}
	
	@Override
	public GuiScreen getRewardEditor(GuiScreen screen, DBEntry<IQuest> quest)
	{
		return null;
	}
	
	public static class RewardCommandSender implements ICommandSender
	{
		private final ChunkCoordinates blockLoc;
		
		private RewardCommandSender(int x, int y, int z)
	    {
	    	blockLoc = new ChunkCoordinates(x, y, z);
	    }
     
		@Override
		public ChunkCoordinates getPlayerCoordinates()
		{
			return blockLoc;
		}
  
		@Override
	    public String getCommandSenderName()
	    {
	        return "BetterQuesting";
	    }

        @Override
        public void sendChatToPlayer(String s) {
        }

        @Override
        public boolean canCommandSenderUseCommand(int i, String s) {
            return i <= 2;
        }

        @Override
        public String translateString(String s, Object... objects) {
            return s;
        }
    }
}
