package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.backport.NbtUtils;
import betterquesting.backport.ResourceLocation;
import bq_standard.client.gui.tasks.PanelTaskLocation;
import bq_standard.tasks.factory.FactoryTaskLocation;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class TaskLocation extends NoProgressTaskBase implements ITaskTickable
{
	public String name = "New Location";
	public String structure = "";
	public int biome = -1;
	public int x = 0;
	public int y = 0;
	public int z = 0;
	public int dim = 0;
	public int range = -1;
	public boolean visible = false;
	public boolean hideInfo = false;
	public boolean invert = false;
	public boolean taxiCab = false;
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskLocation.INSTANCE.getRegistryName();
	}
	
	@Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.location";
	}
	
	@Override
	public void tickTask(@Nonnull ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
		if(pInfo.PLAYER.ticksExisted%100 == 0) internalDetect(pInfo, quest);
	}
	
	@Override
	public void detect(@Nonnull ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
		internalDetect(pInfo, quest);
	}
	
	private void internalDetect(@Nonnull ParticipantInfo pInfo, DBEntry<IQuest> quest)
	{
		if(!pInfo.PLAYER.isEntityAlive() || !(pInfo.PLAYER instanceof EntityPlayerMP)) return;
		
		EntityPlayerMP playerMP = (EntityPlayerMP)pInfo.PLAYER;
		
		boolean flag = false;
		
		if(playerMP.dimension == dim && (range <= 0 || getDistance(playerMP) <= range))
		{
			if(biome >= 0 && biome != playerMP.getServerForPlayer().getBiomeGenForCoords(MathHelper.floor_double(playerMP.posX), MathHelper.floor_double(playerMP.posZ)).biomeID)
            {
                if(!invert) return;
            } else if(structure != null && !structure.isEmpty() && playerMP.getServerForPlayer().getChunkProvider().findClosestStructure(playerMP.getServerForPlayer(), structure, MathHelper.floor_double(playerMP.posX), MathHelper.floor_double(playerMP.posY), MathHelper.floor_double(playerMP.posZ)) == null)
            {
                if(!invert) return;
            } else if(visible && range > 0) // Do not do ray casting with infinite range!
			{
				Vec3 pPos = Vec3.createVectorHelper(playerMP.posX, playerMP.posY + playerMP.getEyeHeight(), playerMP.posZ);
				Vec3 tPos = Vec3.createVectorHelper(x, y, z);
				MovingObjectPosition mop = playerMP.worldObj.rayTraceBlocks_do_do(pPos, tPos, false, true);
				
				flag = mop == null || mop.typeOfHit != EnumMovingObjectType.TILE;
			} else
			{
				flag = true;
			}
		}
		
		if(flag != invert)
        {
            for (UUID uuid : pInfo.ALL_UUIDS) {
                if(!isComplete(uuid)) setComplete(uuid);
            }
            pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
        }
	}
	
	private double getDistance(EntityPlayer player)
    {
        if(!taxiCab)
        {
            return player.getDistance(x, y, z);
        } else
        {
            return Math.abs(player.posX - x) + Math.abs(player.posY - y) + Math.abs(player.posZ - z);
        }
    }
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setString("name", name);
		nbt.setInteger("posX", x);
		nbt.setInteger("posY", y);
		nbt.setInteger("posZ", z);
		nbt.setInteger("dimension", dim);
		nbt.setInteger("biome", biome);
		nbt.setString("structure", structure);
		nbt.setInteger("range", range);
		nbt.setBoolean("visible", visible);
		nbt.setBoolean("hideInfo", hideInfo);
		nbt.setBoolean("invert", invert);
		nbt.setBoolean("taxiCabDist", taxiCab);
		
		return nbt;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		name = nbt.getString("name");
		x = nbt.getInteger("posX");
		y = nbt.getInteger("posY");
		z = nbt.getInteger("posZ");
		dim = nbt.getInteger("dimension");
		biome = NbtUtils.hasKey(nbt,"biome", 99) ? nbt.getInteger("biome") : -1;
		structure = nbt.getString("structure");
		range = nbt.getInteger("range");
		visible = nbt.getBoolean("visible");
		hideInfo = nbt.getBoolean("hideInfo");
		invert = nbt.getBoolean("invert") || nbt.getBoolean("invertDistance");
		taxiCab = nbt.getBoolean("taxiCabDist");
	}
 
	@Override
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskLocation(rect, this);
	}
 
	@Override
	public GuiScreen getTaskEditor(GuiScreen parent, DBEntry<IQuest> quest)
	{
		return null;
	}
    
    private static final HashMap<Integer,String> dimNameCache = new HashMap<Integer,String>();
	
    public static String getDimName(int dim)
	{
	    if(dimNameCache.containsKey(dim))
        {
            return dimNameCache.get(dim);
        }
        
	    try
        {
            WorldProvider prov = DimensionManager.createProviderFor(dim);
            if(prov != null)
            {
                dimNameCache.put(dim, prov.getDimensionName());
                return prov.getDimensionName();
            } else
            {
                dimNameCache.put(dim, "" + dim);
                return "" + dim;
            }
        } catch(Exception e)
        {
            dimNameCache.put(dim, "" + dim);
            return "" + dim;
        }
	}
}
