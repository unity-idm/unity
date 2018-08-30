/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.cache.Cache;

import pl.edu.icm.unity.store.rdbms.cache.GuavaBasicCache;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Cache dedicated to group membership
 *  
 * @author K. Benedyczak
 */
class MembershipRDBMSCache extends GuavaBasicCache<GroupMembership> 
{
	private boolean cacheComplete;
	private Cache<Long, List<GroupMembership>> allByEntity;
	private Cache<String, List<GroupMembership>> allByGroup;
	
	MembershipRDBMSCache()
	{
		super(sa -> new GroupMembership(sa));
	}
	
	@Override
	public synchronized void configure(int ttl, int max)
	{
		super.configure(ttl, max);
		if (disabled)
			return;
		allByEntity = getBuilder(ttl, max).build();
		allByGroup = getBuilder(ttl, max).build();
	}
	
	@Override
	public synchronized void flushWithoutEvent()
	{
		if (disabled)
			return;
		super.flushWithoutEvent();
		allByEntity.invalidateAll();
		allByGroup.invalidateAll();
		cacheComplete = false;
	}
	
	@Override
	public synchronized void storeAll(List<GroupMembership> elements)
	{
		if (disabled)
			return;
		super.storeAll(elements);
		allByEntity.invalidateAll();
		allByGroup.invalidateAll();
		for (GroupMembership gm: elements)
		{
			GroupMembership gmCloned = cloner.apply(gm);
			
			List<GroupMembership> list = allByEntity.getIfPresent(gmCloned.getEntityId());
			if (list == null)
			{
				list = new ArrayList<>();
				allByEntity.put(gmCloned.getEntityId(), list);
			}
			list.add(gmCloned);

			list = allByGroup.getIfPresent(gmCloned.getGroup());
			if (list == null)
			{
				list = new ArrayList<>();
				allByGroup.put(gmCloned.getGroup(), list);
			}
			list.add(gmCloned);
		}
		cacheComplete = true;
	}
	
	synchronized Optional<Boolean> isMember(long entityId, String group)
	{
		if (disabled)
			return Optional.empty();
		if (!cacheComplete)
			return Optional.empty();
		List<GroupMembership> list = allByEntity.getIfPresent(entityId);
		if (list == null)
			return Optional.of(false);
		for (GroupMembership gm: list)
			if (group.equals(gm.getGroup()))
				return Optional.of(true);
		return Optional.of(false);
	}

	synchronized Optional<List<GroupMembership>> getEntityMembership(long entityId)
	{
		if (disabled)
			return Optional.empty();
		if (!cacheComplete)
			return Optional.empty();
		List<GroupMembership> list = allByEntity.getIfPresent(entityId);
		if (list == null)
			return Optional.of(Collections.emptyList());
		return Optional.of(cloneList(list));
	}

	synchronized Optional<List<GroupMembership>> getMembers(String group)
	{
		if (disabled)
			return Optional.empty();
		if (!cacheComplete)
			return Optional.empty();
		List<GroupMembership> list = allByGroup.getIfPresent(group);
		if (list == null)
			return Optional.of(Collections.emptyList());
		return Optional.of(cloneList(list));
	}
}
