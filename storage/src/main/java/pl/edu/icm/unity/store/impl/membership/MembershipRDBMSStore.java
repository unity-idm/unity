/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.rdbms.RDBMSDAO;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.types.basic.GroupMembership;


/**
 * Cache over actual RDBMS storage of {@link GroupMembership}
 * @author K. Benedyczak
 */
@Repository(MembershipRDBMSStore.BEAN)
public class MembershipRDBMSStore implements MembershipDAO, RDBMSDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	
	private MembershipRDBMSStoreNoCache wrapped;
	private MembershipRDBMSCache cache;
	
	@Autowired
	public MembershipRDBMSStore(MembershipJsonSerializer jsonSerializer, GroupDAO groupDAO, CacheManager cacheManager)
	{
		wrapped = new MembershipRDBMSStoreNoCache(jsonSerializer, groupDAO);
		cache = new MembershipRDBMSCache();
		cacheManager.registerCache(cache);
	}

	@Override
	public void create(GroupMembership obj)
	{
		cache.flush();
		wrapped.create(obj);
	}

	@Override
	public void deleteByKey(long entityId, String group)
	{
		cache.flush();
		wrapped.deleteByKey(entityId, group);
	}

	@Override
	public boolean isMember(long entityId, String group)
	{
		Optional<Boolean> cached = cache.isMember(entityId, group);
		if (cached.isPresent())
			return cached.get();
		getAll();
		cached = cache.isMember(entityId, group);
		if (cached.isPresent())
			return cached.get();
		return wrapped.isMember(entityId, group);
	}

	@Override
	public List<GroupMembership> getEntityMembership(long entityId)
	{
		Optional<List<GroupMembership>> cached = cache.getEntityMembership(entityId);
		if (cached.isPresent())
			return cached.get();
		getAll();
		cached = cache.getEntityMembership(entityId);
		if (cached.isPresent())
			return cached.get();
		return wrapped.getEntityMembership(entityId);
	}

	@Override
	public List<GroupMembership> getMembers(String group)
	{
		Optional<List<GroupMembership>> cached = cache.getMembers(group);
		if (cached.isPresent())
			return cached.get();
		getAll();
		cached = cache.getMembers(group);
		if (cached.isPresent())
			return cached.get();
		return wrapped.getMembers(group);
	}

	@Override
	public List<GroupMembership> getAll()
	{
		Optional<List<GroupMembership>> cached = cache.getAll();
		if (cached.isPresent())
			return cached.get();
		List<GroupMembership> all = wrapped.getAll();
		cache.storeAll(all);
		return all;
	}
}
