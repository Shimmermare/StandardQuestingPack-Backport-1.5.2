package bq_standard;

import betterquesting.api.placeholders.ItemPlaceholder;
import betterquesting.api.utils.BigItemStack;
import betterquesting.core.BetterQuesting;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public class NbtBlockType // TODO: Make a version of this for the base mod and give it a dedicated editor
{
    @Nullable
    public Block b = Block.wood; // null means air
    public int m = -1;
    public int n = 1;
    public String oreDict = "";
    public NBTTagCompound tags = new NBTTagCompound();
    
    public NbtBlockType()
    {
    }
    
    public NbtBlockType(@Nullable Block block)
    {
        this.b = block;
        this.oreDict = "";
        this.tags = new NBTTagCompound();
    }
    
    public NbtBlockType(@Nullable Block block, int meta)
    {
        this.b = block;
        this.m = meta;
        this.oreDict = "";
        this.tags = new NBTTagCompound();
    }
    
    public NBTTagCompound writeToNBT(NBTTagCompound json)
    {
        json.setShort("blockID", b == null ? 0 : (short) b.blockID);
        json.setInteger("meta", m);
        json.setTag("nbt", tags);
        json.setInteger("amount", n);
        json.setString("oreDict", oreDict == null ? "" : oreDict);
        return json;
    }
    
    public void readFromNBT(NBTTagCompound json)
    {
        short id = json.getShort("blockID");
        if (id <= 0 || id >= Block.blocksList.length) {
            b = null;
        } else {
            b = Block.blocksList[id];
        }
        m = json.getInteger("meta");
        tags = json.getCompoundTag("nbt");
        n = json.getInteger("amount");
        oreDict = json.getString("oreDict");
    }
    
    @Nullable
    public BigItemStack getItemStack()
    {
        BigItemStack stack;
        
        if(b == null)
        {
            stack = new BigItemStack(BetterQuesting.placeholder, n, m);
            ItemStack baseStack = stack.getBaseStack();
            if (!baseStack.hasTagCompound()) {
                baseStack.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound baseStackTag = baseStack.getTagCompound();
            baseStackTag.setCompoundTag("display", new NBTTagCompound());
            baseStackTag.getCompoundTag("display").setString("Name", "NULL");
        } else
        {
            if(Item.itemsList[b.blockID] == null) return null;
            stack = new BigItemStack(b, n, m);
        }
        
        stack.setOreDict(oreDict);
        return stack;
    }
}
