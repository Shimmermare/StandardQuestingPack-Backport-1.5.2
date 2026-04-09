package bq_standard.core.proxies;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import bq_standard.client.theme.BQSTextures;
import bq_standard.importers.NativeFileImporter;

public class ClientProxy extends CommonProxy
{
	@Override
	public boolean isClient()
	{
		return true;
	}
	
	@Override
	public void registerHandlers()
	{
		super.registerHandlers();
	}
	
	@Override
	public void registerExpansion()
	{
		super.registerExpansion();
		
		QuestingAPI.getAPI(ApiReference.IMPORT_REG).registerImporter(NativeFileImporter.INSTANCE);
        
        BQSTextures.registerTextures();
	}
}
