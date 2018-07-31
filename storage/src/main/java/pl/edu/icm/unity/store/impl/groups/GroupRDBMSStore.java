/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCachingCRUD;
import pl.edu.icm.unity.types.basic.Group;


/**
 * RDBMS storage of {@link Group} with caching
 * @author K. Benedyczak
 */
@Repository(GroupRDBMSStore.BEAN)
public class GroupRDBMSStore extends NamedCachingCRUD<Group, GroupDAOInternal, NamedCache<Group>> implements GroupDAOInternal
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public GroupRDBMSStore(GroupJsonSerializer jsonSerializer, CacheManager cacheManager)
	{
		super(new GroupRDBMSStoreNoCache(jsonSerializer), new HashMapNamedCache<>(group -> group.clone()));
		cacheManager.registerCache(cache);
	}
	
	@Override
	public long create(Group obj)
	{
		cache.flush();
		return wrapped.create(obj);
	}
	
	@Override
	public void updateByKey(long key, Group obj)
	{
		cache.flush();
		wrapped.updateByKey(key, obj);
	}

	@Override
	public void addRemovalHandler(ReferenceRemovalHandler handler)
	{
		wrapped.addRemovalHandler(handler);
	}

	@Override
	public void addUpdateHandler(ReferenceUpdateHandler<Group> handler)
	{
		wrapped.addUpdateHandler(handler);
	}
}
