package bq_standard.rewards.loot;

import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.storage.INBTPartial;
import betterquesting.api2.storage.SimpleDatabase;
import betterquesting.backport.NbtUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.util.*;

public class LootRegistry extends SimpleDatabase<LootGroup> implements INBTPartial<NBTTagCompound, Integer>
{
    // TODO: Add localised group names
    // TODO: Use a better UI updating method
    // TODO: Add claim limits and store by UUID
    
    public static final LootRegistry INSTANCE = new LootRegistry();
    
    private final Comparator<DBEntry<LootGroup>> groupSorter = new Comparator<DBEntry<LootGroup>>() {
        @Override
        public int compare(DBEntry<LootGroup> o1, DBEntry<LootGroup> o2) {
            return Integer.valueOf(o1.getValue().weight).compareTo(o2.getValue().weight);
        }
    };

	public boolean updateUI = false;
	
	public synchronized LootGroup createNew(int id)
    {
        LootGroup group = new LootGroup();
        if(id >= 0) this.add(id, group);
        return group;
    }
    
    public int getTotalWeight()
    {
        int i = 0;
        
        for(DBEntry<LootGroup> lg : this.getEntries())
        {
            i += lg.getValue().weight;
        }
        
        return i;
    }
    
    /**
	 *
	 * @param weight A value between 0 and 1 that represents how common this reward is (i.e. higher values mean rarer loot)
	 * @param rand The random instance used to pick the group
	 * @return a loot group with the corresponding rarity of loot
	 */
    public LootGroup getWeightedGroup(float weight, Random rand)
    {
        final int total = getTotalWeight();
        
        if(total <= 0) return null;
		
		float r = rand.nextFloat() * total/4F + weight*total*0.75F;
		int cnt = 0;
		
		List<DBEntry<LootGroup>> sorted = new ArrayList<DBEntry<LootGroup>>(getEntries());
		Collections.sort(sorted, groupSorter);
		
		for(DBEntry<LootGroup> entry : sorted)
		{
			cnt += entry.getValue().weight;
			if(cnt >= r) return entry.getValue();
		}
		
		return null;
    }
    
    @Override
    public synchronized NBTTagCompound writeToNBT(NBTTagCompound tag, @Nullable List<Integer> subset)
    {
		NBTTagList jRew = new NBTTagList();
		for(DBEntry<LootGroup> entry : getEntries())
		{
		    if(subset != null && !subset.contains(entry.getID())) continue;
			NBTTagCompound jGrp = entry.getValue().writeToNBT(new NBTTagCompound());
			jGrp.setInteger("ID", entry.getID());
			jRew.appendTag(jGrp);
		}
		tag.setTag("groups", jRew);
		
        return tag;
    }
    
    @Override
    public synchronized void readFromNBT(NBTTagCompound tag, boolean merge)
    {
		if(!merge) this.reset();
		
		List<LootGroup> legacyGroups = new ArrayList<LootGroup>();
		
		NBTTagList list = NbtUtils.getTagList(tag,"groups", 10);
		for(int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound entry = NbtUtils.getCompoundTagAt(list, i);
			int id = NbtUtils.hasKey(entry,"ID", 99) ? entry.getInteger("ID") : -1;
			
			LootGroup group = getValue(id);
			if(group == null) group = createNew(id);
			group.readFromNBT(entry);
			if(id < 0) legacyGroups.add(group);
		}
		
		for(LootGroup group : legacyGroups)
        {
            this.add(this.nextID(), group);
        }
		
		updateUI = true;
    }
}
