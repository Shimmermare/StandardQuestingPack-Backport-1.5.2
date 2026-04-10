package bq_standard.tasks;

import betterquesting.api.questing.tasks.ITask;
import betterquesting.backport.NbtUtils;
import bq_standard.core.BQ_Standard;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public abstract class NoProgressTaskBase implements ITask {
    // LinkedHashSet for consistent iteration order - needed in GUI
    private final Set<UUID> completeUsers = new LinkedHashSet<UUID>();

    @Override
    public boolean isComplete(UUID uuid) {
        return completeUsers.contains(uuid);
    }

    @Override
    public void setComplete(UUID uuid) {
        if (uuid != null) {
            completeUsers.add(uuid);
        }
    }

    @Override
    public void resetUser(@Nullable UUID uuid) {
        if (uuid == null) {
            completeUsers.clear();
        } else {
            completeUsers.remove(uuid);
        }
    }

    @Override
    public NBTTagCompound writeProgressToNBT(NBTTagCompound nbt, @Nullable List<UUID> users) {
        NBTTagList jArray = new NBTTagList();

        for (UUID uuid : completeUsers) {
            if (users == null || users.contains(uuid)) jArray.appendTag(new NBTTagString(null, uuid.toString()));
        }

        nbt.setTag("completeUsers", jArray);

        return nbt;
    }

    @Override
    public void readProgressFromNBT(NBTTagCompound json, boolean merge) {
        if (!merge) completeUsers.clear();
        NBTTagList cList = NbtUtils.getTagList(json, "completeUsers", 8);
        for (int i = 0; i < cList.tagCount(); i++) {
            try {
                completeUsers.add(UUID.fromString(NbtUtils.getStringTagAt(cList, i)));
            } catch (Exception e) {
                BQ_Standard.logger.log(Level.SEVERE, "Unable to load UUID for task", e);
            }
        }
    }
}
