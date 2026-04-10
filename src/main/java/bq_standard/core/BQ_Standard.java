package bq_standard.core;

import bq_standard.commands.BQS_Commands;
import bq_standard.core.proxies.CommonProxy;
import bq_standard.handlers.ConfigHandler;
import bq_standard.handlers.GuiHandler;
import bq_standard.handlers.LootSaveLoad;
import bq_standard.items.ItemLootChest;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Mod(modid = BQ_Standard.MODID, name = BQ_Standard.NAME)
public class BQ_Standard
{
    public static final String MODID = "bq_standard";
    public static final String NAME = "Standard Expansion";
    public static final String PROXY = "bq_standard.core.proxies";
    public static final String CHANNEL = "BQ_STANDARD";
	
	@Instance(MODID)
	public static BQ_Standard instance;
	
	@SidedProxy(clientSide = PROXY + ".ClientProxy", serverSide = PROXY + ".CommonProxy")
	public static CommonProxy proxy;
	public static Logger logger;

    // FIXME make configurable
	public static Item lootChest = new ItemLootChest(8300);
    
    @Mod.PreInit
    public void preInit(FMLPreInitializationEvent event)
    {
    	logger = event.getModLog();
    	
    	ConfigHandler.config = new Configuration(event.getSuggestedConfigurationFile(), true);
    	ConfigHandler.initConfigs();
    	
    	proxy.registerHandlers();
    	
    	NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());

        loadLocalizations();
    }
    
    @Mod.Init
    public void init(FMLInitializationEvent event)
    {
    	GameRegistry.registerItem(lootChest, "loot_chest");
    	
    	proxy.registerRenderers();
    }
    
    @Mod.PostInit
    public void postInit(FMLPostInitializationEvent event)
    {
        if(Loader.isModLoaded("betterquesting"))
        {
            proxy.registerExpansion();
        }
    }
	
	@Mod.ServerStarting
	public void serverStart(FMLServerStartingEvent event)
	{
		MinecraftServer server = event.getServer();
		ICommandManager command = server.getCommandManager();
		ServerCommandManager manager = (ServerCommandManager) command;
		
		manager.registerCommand(new BQS_Commands());
		
		LootSaveLoad.INSTANCE.LoadLoot(event.getServer());
	}
	
	@Mod.ServerStopped
    public void serverStopped(FMLServerStoppedEvent event)
    {
        LootSaveLoad.INSTANCE.UnloadLoot();
    }


    private void loadLocalizations() {
        @SuppressWarnings("unchecked")
        Map<String, String> languages = StringTranslate.getInstance().getLanguageList();
        List<String> loaded = new ArrayList<String>();
        for (Map.Entry<String, String> entry : languages.entrySet()) {
            String lang = entry.getKey();
            String file = "/mods/bq_standard/lang/" + lang + ".lang";
            if (this.getClass().getResource(file) != null) {
                LanguageRegistry.instance().loadLocalization(file, lang, false);
                loaded.add(lang);
            }
        }
        logger.info("Loaded localizations: " + loaded);
    }
}
