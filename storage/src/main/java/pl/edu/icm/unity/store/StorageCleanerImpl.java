/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.StorageCleaner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

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

	private Set<CachingDAO> cachingDAOs;

	private boolean resetDone = false;

	private TransactionalRunner tx;
	
	@Autowired
	public StorageCleanerImpl(Map<String, StoreLoaderInternal> impl, StorageConfiguration cfg, 
			Set<CachingDAO> cachingDAOs, TransactionalRunner tx) throws Exception
	{
		this.tx = tx;
		storeLoaderInternal = impl.get(BEAN_PFX + cfg.getEngine().name());
		this.cachingDAOs =  cachingDAOs;
	}
	
	@Override
	public void reset()
	{
		storeLoaderInternal.reset();
		clearCache();
	}

	@Override
	public void cleanOrDelete()
	{
		if (resetDone)
		{
			tx.runInTransaction(() -> deleteEverything());
		} else
		{
			reset();
			resetDone = true;
		}
	}

	
	@Override
	public void deleteEverything()
	{
		storeLoaderInternal.deleteEverything();
		clearCache();
	}

	@Override
	public void runPostImportCleanup()
	{
		storeLoaderInternal.runPostImportCleanup();
	}

	@Override
	public void shutdown()
	{
		storeLoaderInternal.shutdown();
	}
	
	private void clearCache()
	{
		for (CachingDAO dao: cachingDAOs) 
			dao.invalidateCache();
	}

	@Override
	public void deletePreImport(List<String> content)
	{
		storeLoaderInternal.deletePreImport(content);	
	}
}
