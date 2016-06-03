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
import org.springframework.stereotype.Component;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageCleaner;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.StoreLoaderInternal;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.impl.attribute.AttributeHzStore;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.entities.EntityHzStore;
import pl.edu.icm.unity.store.impl.groups.GroupHzStore;
import pl.edu.icm.unity.store.impl.identities.IdentityHzStore;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeHzStore;
import pl.edu.icm.unity.store.impl.membership.MembershipHzStore;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectHzStore;
import pl.edu.icm.unity.store.impl.tokens.TokenHzStore;
import pl.edu.icm.unity.store.rdbms.DB;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;

/**
 * Loads Hazelcast data from RDBMS at startup.
 * @author K. Benedyczak
 */
@Component(HzStoreLoader.NAME)
public class HzStoreLoader implements StoreLoaderInternal
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, HzStoreLoader.class);
	
	public static final String NAME = StorageCleaner.BEAN_PFX + "hz";
	
	
	@Autowired
	private AttributeTypeHzStore attributeTypeDAO;
	
	@Autowired
	private IdentityTypeHzStore identityTypeDAO;
	
	@Autowired
	private EntityHzStore entityDAO;
	
	@Autowired
	private IdentityHzStore identityDAO;
	
	@Autowired
	private GroupHzStore groupDAO;
	
	@Autowired
	private MembershipHzStore membershipDAO;
	
	@Autowired
	private AttributeHzStore attributeDAO;

	@Autowired
	private TokenHzStore tokenDAO;
	
	@Autowired
	private GenericObjectHzStore genericObjDAO;

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
	public void init()
	{
		if (cfg.getEngine() != StorageEngine.hz)
			return;
		loadTransactional();
	}

	private void loadFromPersistentStore()
	{
		log.info("Loading data from the persistent data store");
		attributeTypeDAO.populateFromRDBMS(hzInstance);
		identityTypeDAO.populateFromRDBMS(hzInstance);
		entityDAO.populateFromRDBMS(hzInstance);
		identityDAO.populateFromRDBMS(hzInstance);
		groupDAO.populateFromRDBMS(hzInstance);
		membershipDAO.populateFromRDBMS(hzInstance);
		attributeDAO.populateFromRDBMS(hzInstance);
		tokenDAO.populateFromRDBMS(hzInstance);
		genericObjDAO.populateFromRDBMS(hzInstance);
		log.info("Population of the in-memory data store completed");
	}
	
	private void loadTransactional()
	{
		rdbmstx.runInTransaction(() -> {
			hztx.runInTransaction(() -> {
				loadFromPersistentStore();
			}); 
		});
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
		HzTransactionTL.resetTransaction();
		loadTransactional();
	}
}
