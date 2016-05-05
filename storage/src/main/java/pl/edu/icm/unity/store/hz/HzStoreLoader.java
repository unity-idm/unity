/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.base.internal.StorageEngine;
import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageCleaner;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StoreLoaderInternal;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
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
	private ApplicationContext appContext;
	
	@Autowired
	private List<HzDAO> hzStores;
	
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
		rdbmstx.runInTransaction(() -> {
			hztx.runInTransaction(() -> {
				loadFromPersistentStore();
			}); 
		});
	}

	private LinkedHashSet<HzDAO> getSortedDaos()
	{
		GenericApplicationContext ctx = (GenericApplicationContext) appContext;
		DefaultListableBeanFactory beanFactory = ctx.getDefaultListableBeanFactory();
		
		Map<String, HzDAO> beansOfType = beanFactory.getBeansOfType(HzDAO.class, true, true);
		log.debug("All HzDAOs: " + beansOfType.keySet());
		
		Map<String, Set<String>> dependencies = new HashMap<>();
		for (String bean: beansOfType.keySet())
			dependencies.put(bean, Sets.newHashSet(
					beanFactory.getDependenciesForBean(bean)));
		
		Comparator<String> depCmp = new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2)
			{
				if (dependencies.get(o1).contains(o2))
					return -1;
				if (dependencies.get(o2).contains(o1))
					return 1;
				return 0;
			}
		};

		List<String> sortedBeans = new ArrayList<>(beansOfType.keySet());
		Collections.sort(sortedBeans, depCmp);

		LinkedHashSet<HzDAO> ret = new LinkedHashSet<>();
		for (String bean: sortedBeans)
			ret.add(beansOfType.get(bean));
		log.debug("All HzDAOs injected as list: " + hzStores);
		log.debug("All HzDAOs sorted: " + sortedBeans);
		return ret;
	}
	
	private void loadFromPersistentStore()
	{
		Set<HzDAO> sortedDaos = getSortedDaos();
		for (HzDAO dao: sortedDaos)
			dao.populateFromRDBMS(hzInstance);
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
