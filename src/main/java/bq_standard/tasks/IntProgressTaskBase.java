package bq_standard.tasks;

import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.utils.Tuple2;
import betterquesting.backport.NbtUtils;
import bq_standard.core.BQ_Standard;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

public abstract class IntProgressTaskBase implements ITask {
    // LinkedHashSet/Map for consistent iteration order - needed in GUI
    private final Set<UUID> completeUsers = new LinkedHashSet<UUID>();
    private final Map<UUID, Integer> userProgress = new LinkedHashMap<UUID, Integer>();

    @Override
    public boolean isComplete(UUID uuid) {
        return completeUsers.contains(uuid);
    }

    @Override
    public void setComplete(UUID uuid) {
        completeUsers.add(uuid);
    }

    @Override
    public void readProgressFromNBT(NBTTagCompound nbt, boolean merge) {
        if (!merge) {
            completeUsers.clear();
            userProgress.clear();
        }

        NBTTagList cList = NbtUtils.getTagList(nbt, "completeUsers", 8);
        for (int i = 0; i < cList.tagCount(); i++) {
            try {
                completeUsers.add(UUID.fromString(NbtUtils.getStringTagAt(cList, i)));
            } catch (Exception e) {
                BQ_Standard.logger.log(Level.SEVERE, "Unable to load UUID for task", e);
            }
        }

        NBTTagList pList = NbtUtils.getTagList(nbt, "userProgress", 10);
        for (int n = 0; n < pList.tagCount(); n++) {
            try {
                NBTTagCompound pTag = NbtUtils.getCompoundTagAt(pList, n);
                UUID uuid = UUID.fromString(pTag.getString("uuid"));
                userProgress.put(uuid, pTag.getInteger("value"));
            } catch (Exception e) {
                BQ_Standard.logger.log(Level.SEVERE, "Unable to load user progress for task", e);
            }
        }
    }

    @Override
    public NBTTagCompound writeProgressToNBT(NBTTagCompound nbt, @Nullable List<UUID> users) {
        NBTTagList jArray = new NBTTagList();
        NBTTagList progArray = new NBTTagList();

        if (users != null) {
            for (UUID uuid : users) {
                if (completeUsers.contains(uuid)) jArray.appendTag(new NBTTagString(null, uuid.toString()));

                Integer data = userProgress.get(uuid);
                if (data != null) {
                    NBTTagCompound pJson = new NBTTagCompound();
                    pJson.setString("uuid", uuid.toString());
                    pJson.setInteger("value", data);
                    progArray.appendTag(pJson);
                }
            }
        } else {
            for (UUID uuid : completeUsers) {
                jArray.appendTag(new NBTTagString(null, uuid.toString()));
            }

            for (Map.Entry<UUID, Integer> entry : userProgress.entrySet()) {
                UUID uuid = entry.getKey();
                int data = entry.getValue();
                NBTTagCompound pJson = new NBTTagCompound();
                pJson.setString("uuid", uuid.toString());
                pJson.setInteger("value", data);
                progArray.appendTag(pJson);
            }
        }

        nbt.setTag("completeUsers", jArray);
        nbt.setTag("userProgress", progArray);

        return nbt;
    }

    @Override
    public void resetUser(@Nullable UUID uuid) {
        if (uuid == null) {
            completeUsers.clear();
            userProgress.clear();
        } else {
            completeUsers.remove(uuid);
            userProgress.remove(uuid);
        }
    }

    public void setUserProgress(UUID uuid, int progress)
    {
        userProgress.put(uuid, progress);
    }

    public int getUserProgress(UUID uuid)
    {
        Integer n = userProgress.get(uuid);
        return n == null? 0 : n;
    }

    public List<Tuple2<UUID, Integer>> getBulkProgress(@Nonnull List<UUID> uuids)
    {
        if(uuids.size() <= 0) return Collections.emptyList();
        List<Tuple2<UUID, Integer>> list = new ArrayList<Tuple2<UUID, Integer>>();
        for (UUID key : uuids) {
            list.add(new Tuple2<UUID, Integer>(key, getUserProgress(key)));
        }
        return list;
    }
}
