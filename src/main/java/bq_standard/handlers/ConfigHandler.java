package bq_standard.handlers;

import bq_standard.core.BQS_Settings;
import bq_standard.core.BQ_Standard;
import net.minecraftforge.common.Configuration;

import java.util.logging.Level;

public class ConfigHandler
{
	public static Configuration config;
	
	public static void initConfigs()
	{
		if(config == null)
		{
			BQ_Standard.logger.log(Level.SEVERE, "Config attempted to be loaded before it was initialised!");
			return;
		}
		
		config.load();

        BQS_Settings.lootChestItemId = config.getItem("LootChest", BQS_Settings.lootChestItemId).getInt();
		
		config.save();
		
		BQ_Standard.logger.log(Level.INFO, "Loaded configs...");
	}
}
