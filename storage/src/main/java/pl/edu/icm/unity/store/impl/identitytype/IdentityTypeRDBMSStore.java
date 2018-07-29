/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCachingCRUD;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Caching implementation of {@link IdentityTypeDAO}.
 * 
 * @author K. Benedyczak
 */
@Repository(IdentityTypeRDBMSStore.BEAN)
public class IdentityTypeRDBMSStore extends NamedCachingCRUD<IdentityType, IdentityTypeDAO, NamedCache<IdentityType>> 
		implements IdentityTypeDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	
	@Autowired
	public IdentityTypeRDBMSStore(IdentityTypeJsonSerializer jsonSerializer, CacheManager cacheManager)
	{
		super(new IdentityTypeRDBMSStoreInt(jsonSerializer), 
				new HashMapNamedCache<IdentityType>(it -> it.clone()));
		cacheManager.registerCache(cache);
	}
}
