/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.rdbms.cache.BasicCache;
import pl.edu.icm.unity.store.rdbms.cache.BasicCachingCRUD;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.GuavaBasicCache;
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
		super(new EntityRDBMSStore(jsonSerializer), new GuavaBasicCache<>(ei -> ei.clone()));
		cacheManager.registerCacheWithFlushingPropagation(cache);
	}
	
	@Override
	public long create(EntityInformation obj)
	{
		long created = wrapped.create(obj);
		cache.flushWithEvent();
		return created;
	}

	@Override
	public List<EntityInformation> getByGroup(String group)
	{
		return wrapped.getByGroup(group);
	}
}
