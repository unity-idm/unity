/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.rdbms.cache.BasicCache;
import pl.edu.icm.unity.store.rdbms.cache.BasicCachingCRUD;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.HashMapBasicCache;
import pl.edu.icm.unity.types.basic.EntityInformation;


/**
 * RDBMS storage of {@link StoredEntity}
 * @author K. Benedyczak
 */
@Repository(EntityRDBMSCachingStore.BEAN)
public class EntityRDBMSCachingStore extends BasicCachingCRUD<EntityInformation, EntityDAO, BasicCache<EntityInformation>> 
					implements EntityDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public EntityRDBMSCachingStore(EntityJsonSerializer jsonSerializer, CacheManager cacheManager)
	{
		super(new EntityRDBMSStore(jsonSerializer, cacheManager), new HashMapBasicCache<>(ei -> ei.clone()));
		cacheManager.registerCache(cache);
	}
	
	@Override
	public long create(EntityInformation obj)
	{
		cache.flush();
		return wrapped.create(obj);
	}
}
