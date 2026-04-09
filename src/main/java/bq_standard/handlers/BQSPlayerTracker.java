package bq_standard.handlers;

import bq_standard.network.handlers.NetLootSync;
import cpw.mods.fml.common.IPlayerTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class BQSPlayerTracker implements IPlayerTracker {
    @Override
    public void onPlayerLogin(EntityPlayer player) {
        if(!player.worldObj.isRemote && player instanceof EntityPlayerMP)
        {
            NetLootSync.sendSync((EntityPlayerMP)player);
        }
    }

    @Override
    public void onPlayerLogout(EntityPlayer entityPlayer) {

    }

    @Override
    public void onPlayerChangedDimension(EntityPlayer entityPlayer) {

    }

    @Override
    public void onPlayerRespawn(EntityPlayer entityPlayer) {

    }
}
