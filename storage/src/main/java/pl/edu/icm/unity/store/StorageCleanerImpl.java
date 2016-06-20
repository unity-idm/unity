/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.StorageCleaner;

/**
 * Storage engine independent storage cleaning.
 * 
 * @author K. Benedyczak
 */
@Component
public class StorageCleanerImpl implements StorageCleaner
{
	public static final String BEAN_PFX = "StoreLoader";
	
	private StoreLoaderInternal storeLoaderInternal;
	
	@Autowired
	public StorageCleanerImpl(Map<String, StoreLoaderInternal> impl, StorageConfiguration cfg) throws Exception
	{
		storeLoaderInternal = impl.get(BEAN_PFX + cfg.getEngine().name());
	}
	
	@Override
	public void reset()
	{
		storeLoaderInternal.reset();
	}

	@Override
	public void deleteEverything()
	{
		storeLoaderInternal.deleteEverything();
	}

	@Override
	public void runPostImportCleanup()
	{
		storeLoaderInternal.runPostImportCleanup();
	}
}
