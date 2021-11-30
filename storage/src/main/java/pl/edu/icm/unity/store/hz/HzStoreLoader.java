/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.StoreLoaderInternal;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSEventSink;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.impl.attribute.AttributeHzStore;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.entities.EntityHzStore;
import pl.edu.icm.unity.store.impl.events.EventHzStore;
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
	
	public static final String NAME = StorageCleanerImpl.BEAN_PFX + "hz";
	
	private AttributeTypeHzStore attributeTypeDAO;
	private IdentityTypeHzStore identityTypeDAO;
	private EntityHzStore entityDAO;
	private IdentityHzStore identityDAO;
	private GroupHzStore groupDAO;
	private MembershipHzStore membershipDAO;
	private AttributeHzStore attributeDAO;
	private TokenHzStore tokenDAO;
	private EventHzStore eventDAO;
	private GenericObjectHzStore genericObjDAO;
	private TransactionalRunner rdbmstx;
	private TransactionalRunner hztx;
	private DB initDB;
	private HazelcastInstance hzInstance;
	private RDBMSEventSink sink;
	private StorageConfiguration cfg;

	@Autowired
	public HzStoreLoader(AttributeTypeHzStore attributeTypeDAO, IdentityTypeHzStore identityTypeDAO,
			EntityHzStore entityDAO, IdentityHzStore identityDAO, GroupHzStore groupDAO,
			MembershipHzStore membershipDAO, AttributeHzStore attributeDAO, TokenHzStore tokenDAO,
			EventHzStore eventDAO, GenericObjectHzStore genericObjDAO, 
			@Qualifier(SQLTransactionalRunner.NAME) TransactionalRunner rdbmstx,
			@Qualifier(HzTransactionalRunner.NAME) TransactionalRunner hztx, 
			DB initDB, HazelcastInstance hzInstance, RDBMSEventSink sink,
			StorageConfiguration cfg)
	{
		this.attributeTypeDAO = attributeTypeDAO;
		this.identityTypeDAO = identityTypeDAO;
		this.entityDAO = entityDAO;
		this.identityDAO = identityDAO;
		this.groupDAO = groupDAO;
		this.membershipDAO = membershipDAO;
		this.attributeDAO = attributeDAO;
		this.tokenDAO = tokenDAO;
		this.eventDAO = eventDAO;
		this.genericObjDAO = genericObjDAO;
		this.rdbmstx = rdbmstx;
		this.hztx = hztx;
		this.initDB = initDB;
		this.hzInstance = hzInstance;
		this.sink = sink;
		this.cfg = cfg;
		
		init();
	}

	public void init()
	{
		if (cfg.getEngine() != StorageEngine.hz)
			return;
		//in principle we should be able to simply run loadTransactional()
		//  but during tests when spring cashes beans bit 'randomly' it is safer bet 
		reloadHzFromRDBMSInternal();
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
		eventDAO.populateFromRDBMS(hzInstance);
		genericObjDAO.populateFromRDBMS(hzInstance);
		sink.start();
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
		sink.stop();
		initDB.reset();
		reloadHzFromRDBMSInternal();
	}

	@Override
	public void deleteEverything()
	{
		sink.stop();
		rdbmstx.runInTransaction(() -> {
			initDB.deleteEverything();
		});
		reloadHzFromRDBMSInternal();
	}

	void reloadHzFromRDBMS()
	{
		sink.stop();
		reloadHzFromRDBMSInternal();
	}

	private void reloadHzFromRDBMSInternal()
	{
		Collection<DistributedObject> distributedObjects = hzInstance.getDistributedObjects();
		for (DistributedObject obj: distributedObjects)
			obj.destroy();
		HzTransactionTL.resetTransaction();
		loadTransactional();
	}
	
	@Override
	public void runPostImportCleanup()
	{
		rdbmstx.runInTransaction(() -> {
			initDB.runPostImportCleanup();
		});
	}

	@Override
	public void shutdown()
	{
		sink.stop();
	}

	@Override
	public void deletePreImport(List<String> content)
	{
		rdbmstx.runInTransaction(() -> {
			initDB.deletePreImport(content);
		});
	}
}
