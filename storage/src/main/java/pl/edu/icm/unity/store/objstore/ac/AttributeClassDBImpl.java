/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCachingCRUDWithTS;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Easy access to {@link AttributesClass} storage.
 * @author K. Benedyczak
 */
@Component
public class AttributeClassDBImpl extends NamedCachingCRUDWithTS<AttributesClass, AttributeClassDBNoChaceImpl> implements AttributeClassDB 
{
	@Autowired
	public AttributeClassDBImpl(AttributeClassHandler handler, ObjectStoreDAO dbGeneric, CacheManager cacheManager)
	{
		super(new AttributeClassDBNoChaceImpl(handler, dbGeneric), new HashMapNamedCache<>(ac -> ac.clone()));
		cacheManager.registerCache(cache);
	}
}
