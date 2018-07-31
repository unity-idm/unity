/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.rdbms.cache.NamedCachingCRUD;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * RDBMS storage of {@link Identity} with caching
 * @author K. Benedyczak
 */
@Repository(IdentityRDBMSStore.BEAN)
public class IdentityRDBMSStore extends NamedCachingCRUD<StoredIdentity, IdentityDAO, IdentitiesCache> implements IdentityDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public IdentityRDBMSStore(IdentityJsonSerializer jsonSerializer, CacheManager cacheMananger)
	{
		super(new IdentityRDBMSStoreInt(jsonSerializer), new IdentitiesCache(id -> id.clone()));
		cacheMananger.registerCache(cache);
	}

	@Override
	public List<StoredIdentity> getByEntityFull(long entityId)
	{
		Optional<List<StoredIdentity>> cached = cache.getByEntity(entityId, this::getAll);
		if (cached.isPresent())
			return cached.get();
		return wrapped.getByEntityFull(entityId);
	}
	
	@Override
	public List<Identity> getByEntity(long entityId)
	{
		return getByEntityFull(entityId).stream().map(si -> si.getIdentity()).collect(Collectors.toList());
	}
}
