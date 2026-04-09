package bq_standard.deps;

import bq_standard.core.BQ_Standard;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.liquids.LiquidStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Wrapper for NEI.
 * TODO: Voldeloom can't compile without deobfuscated version of NEI and I'm too lazy to resolve it, so reflection for now.
 */
public class NeiWrapper {
    private static boolean initialized;
    private static Method openRecipeGuiItemStack;
    private static Method openRecipeGuiLiquidStack;

    public void openRecipeGui(String outputId, ItemStack stack) {
        // noop
    }

    public void openRecipeGui(String outputId, LiquidStack stack) {
        // noop
    }

    public static void lookupRecipe(ItemStack stack) {
        if (openRecipeGuiItemStack != null) {
            try {
                openRecipeGuiItemStack.invoke(null, stack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void lookupRecipe(LiquidStack stack) {
        if (openRecipeGuiLiquidStack != null) {
            try {
                openRecipeGuiLiquidStack.invoke(null, stack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void initIfNot() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!Loader.isModLoaded("NotEnoughItems")) {
            return;
        }

        try {
            Class<?> guiCraftingRecipe = Class.forName("codechicken.nei.recipe.GuiCraftingRecipe");
            openRecipeGuiItemStack = guiCraftingRecipe.getMethod("openRecipeGui", String.class, ItemStack.class);
            openRecipeGuiLiquidStack = guiCraftingRecipe.getMethod("openRecipeGui", String.class, LiquidStack.class);
        } catch (Exception e) {
            BQ_Standard.logger.log(Level.SEVERE, "Failed to init NEI API wrapper, integration will be disabled", e);
        }
    }
}
