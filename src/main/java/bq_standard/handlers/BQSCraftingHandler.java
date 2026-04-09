package bq_standard.handlers;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskCrafting;
import cpw.mods.fml.common.ICraftingHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

public class BQSCraftingHandler implements ICraftingHandler {
    @Override
    public void onCrafting(EntityPlayer player, ItemStack crafting, IInventory craftMatrix) {
        if (player == null || player.worldObj.isRemote) return;

        ParticipantInfo pInfo = new ParticipantInfo(player);

        ItemStack refStack = crafting.copy();

        if (refStack.stackSize <= 0 && craftMatrix instanceof InventoryCrafting) // Hack for broken-ass shift clicking reporting empty stacks
        {
            ItemStack result = CraftingManager.getInstance().findMatchingRecipe((InventoryCrafting) craftMatrix, player.worldObj);
            if (result != null) refStack.stackSize = result.stackSize;
        }

        for (DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests())) {
            for (DBEntry<ITask> task : entry.getValue().getTasks().getEntries()) {
                if (task.getValue() instanceof TaskCrafting)
                    ((TaskCrafting) task.getValue()).onItemCraft(pInfo, entry, refStack);
            }
        }
    }

    @Override
    public void onSmelting(EntityPlayer player, ItemStack smelting) {
        // This event is even more busted than crafting when shift clicking (only ever reports 2 empty stacks regardless of actual amount)
        if (player == null || player.worldObj.isRemote) return;

        ParticipantInfo pInfo = new ParticipantInfo(player);

        ItemStack refStack = smelting.copy();
        if (refStack.stackSize <= 0)
            refStack.stackSize = 1; // Doesn't really fix much but it's better than nothing I suppose

        for (DBEntry<IQuest> entry : QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(pInfo.getSharedQuests())) {
            for (DBEntry<ITask> task : entry.getValue().getTasks().getEntries()) {
                if (task.getValue() instanceof TaskCrafting)
                    ((TaskCrafting) task.getValue()).onItemSmelt(pInfo, entry, refStack);
            }
        }
    }
}
