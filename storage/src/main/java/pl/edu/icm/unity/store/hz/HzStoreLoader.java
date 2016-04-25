/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.base.internal.StorageEngine;
import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageCleaner;
import pl.edu.icm.unity.store.StoreLoaderInternal;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeRDBMSStore;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeHzStore;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeRDBMSStore;
import pl.edu.icm.unity.store.rdbms.DB;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;

/**
 * Loads Hazelcast data from RDBMS at startup.
 * @author K. Benedyczak
 */
@Component(HzStoreLoader.NAME)
@DependsOn(value = {"AttributeSyntaxFactoriesRegistry", "IdentityTypesRegistry"})
public class HzStoreLoader implements StoreLoaderInternal
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, HzStoreLoader.class);
	
	public static final String NAME = StorageCleaner.BEAN_PFX + "hz";
	
	@Autowired
	private AttributeTypeHzStore atTypeStore;
	@Autowired
	private IdentityTypeHzStore idTypeStore;
	@Autowired
	private AttributeTypeRDBMSStore rdbmsAtTypeStore;
	@Autowired
	private IdentityTypeRDBMSStore rdbmsIdTypeStore;
	
	@Autowired @Qualifier(SQLTransactionalRunner.NAME)
	private TransactionalRunner rdbmstx;
	@Autowired @Qualifier(HzTransactionalRunner.NAME)
	private TransactionalRunner hztx;
	
	@Autowired
	private DB initDB;
	@Autowired
	private HazelcastInstance hzInstance;
	@Autowired 
	private StorageConfiguration cfg;
	
	@PostConstruct
	public void init() throws Exception
	{
		if (cfg.getEngine() != StorageEngine.hz)
			return;
		initDB.initialize();
		rdbmstx.runInTransaction(() -> {
			hztx.runInTransaction(() -> {
				loadFromPersistentStore();
			}); 
		});
	}

	private void loadFromPersistentStore()
	{
		log.info("Loading identity types");
		idTypeStore.initHazelcast(rdbmsIdTypeStore, hzInstance);
		log.info("Loading attribute types");
		atTypeStore.initHazelcast(rdbmsAtTypeStore, hzInstance);
		log.info("Population of the in-memory data store completed");
	}
	
	/**
	 * Use with care - only for maintenance in case of tests or expert tools.
	 */
	@Override
	public void reset()
	{
		initDB.reset();
		Collection<DistributedObject> distributedObjects = hzInstance.getDistributedObjects();
		for (DistributedObject obj: distributedObjects)
				obj.destroy();
	}
}
