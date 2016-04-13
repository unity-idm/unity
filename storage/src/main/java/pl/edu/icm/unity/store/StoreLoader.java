/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attribute.AttributeTypeDAOImpl;
import pl.edu.icm.unity.store.impl.identity.IdentityTypeDAOImpl;
import pl.edu.icm.unity.store.rdbms.InitDB;

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
	private AttributeTypeDAOImpl atTypeStore;
	@Autowired
	private IdentityTypeDAOImpl idTypeStore;
	@Autowired
	private InitDB initDB;
	@Autowired
	private HazelcastInstance hzInstance;
	
	@PostConstruct
	public void initializeStores()
	{
		log.info("Loading identity types");
		idTypeStore.initHazelcast();
		log.info("Loading attribute types");
		atTypeStore.initHazelcast();
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
