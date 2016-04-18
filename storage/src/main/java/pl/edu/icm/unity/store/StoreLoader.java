/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.impl.attribute.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.attribute.AttributeTypeRDBMSStore;
import pl.edu.icm.unity.store.impl.identity.IdentityTypeHzStore;
import pl.edu.icm.unity.store.impl.identity.IdentityTypeRDBMSStore;
import pl.edu.icm.unity.store.rdbms.InitDB;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;

/**
 * Loads Hazelcast data from RDBMS at startup.
 * @author K. Benedyczak
 */
@Component
@DependsOn(value = {"AttributeSyntaxFactoriesRegistry", "IdentityTypesRegistry"})
public class StoreLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, StoreLoader.class);
	
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
	private InitDB initDB;
	@Autowired
	private HazelcastInstance hzInstance;
	
	@PostConstruct
	public void initializeStores()
	{
		rdbmstx.runInTransaction(() -> {
			hztx.runInTransaction(() -> {
				loadFromPersistentStore();
			}); 
		});
	}

	private void loadFromPersistentStore()
	{
		log.info("Loading identity types");
		idTypeStore.initHazelcast(rdbmsIdTypeStore);
		log.info("Loading attribute types");
		atTypeStore.initHazelcast(rdbmsAtTypeStore);
		log.info("Population of the in-memory data store completed");
	}
	
	/**
	 * Use with care - only for maintenance in case of tests or expert tools.
	 */
	public void resetDatabase()
	{
		initDB.resetDatabase();
		Collection<DistributedObject> distributedObjects = hzInstance.getDistributedObjects();
		for (DistributedObject obj: distributedObjects)
				obj.destroy();
		initializeStores();
	}
}
