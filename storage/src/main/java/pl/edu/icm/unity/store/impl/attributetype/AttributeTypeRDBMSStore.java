/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCachingCRUD;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * RDBMS storage of {@link AttributeType}
 * @author K. Benedyczak
 */
@Repository(AttributeTypeRDBMSStore.BEAN)
public class AttributeTypeRDBMSStore extends NamedCachingCRUD<AttributeType, AttributeTypeDAOInternal, NamedCache<AttributeType>> 
		implements AttributeTypeDAOInternal
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public AttributeTypeRDBMSStore(AttributeTypeRDBMSSerializer jsonSerializer, CacheManager cacheMananger)
	{
		super(new AttributeTypeRDBMSStoreInt(jsonSerializer), new HashMapNamedCache<>(at -> at.clone()));
		cacheMananger.registerCacheWithFlushingPropagation(cache);
	}

	@Override
	public void addRemovalHandler(ReferenceRemovalHandler handler)
	{
		wrapped.addRemovalHandler(handler);
	}

	@Override
	public void addUpdateHandler(ReferenceUpdateHandler<AttributeType> handler)
	{
		wrapped.addUpdateHandler(handler);
	}
}
