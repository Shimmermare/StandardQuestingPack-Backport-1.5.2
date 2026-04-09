package bq_standard;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

/**
 * Elevates the player's privileges to OP level for use in command rewards
 */
public class AdminExecute implements ICommandSender
{
	private final EntityPlayer player;
	
	public AdminExecute(EntityPlayer player)
	{
		this.player = player;
	}

	@Override
	public String getCommandSenderName()
	{
		return player.getCommandSenderName();
	}

    @Override
    public void sendChatToPlayer(String s) {
        player.addChatMessage(s);
    }

    @Override
	public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_)
	{
		return true;
	}

    @Override
    public String translateString(String s, Object... objects) {
        return player.translateString(s, objects);
    }

    @Override
	public ChunkCoordinates getPlayerCoordinates()
	{
		return player.getPlayerCoordinates();
	}
}
