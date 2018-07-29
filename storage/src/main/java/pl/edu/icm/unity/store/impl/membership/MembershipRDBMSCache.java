/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pl.edu.icm.unity.store.rdbms.cache.HashMapBasicCache;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Cache dedicated to group membership
 *  
 * @author K. Benedyczak
 */
class MembershipRDBMSCache extends HashMapBasicCache<GroupMembership> 
{
	private Map<Long, List<GroupMembership>> allByEntity = new HashMap<>();
	private Map<String, List<GroupMembership>> allByGroup = new HashMap<>();
	
	MembershipRDBMSCache()
	{
		super(sa -> new GroupMembership(sa));
	}
	
	@Override
	public synchronized void flush()
	{
		super.flush();
		allByEntity.clear();
		allByGroup.clear();
	}
	
	@Override
	public synchronized void storeAll(List<GroupMembership> elements)
	{
		super.storeAll(elements);
		allByEntity.clear();
		allByGroup.clear();
		for (GroupMembership gm: elements)
		{
			GroupMembership gmCloned = cloner.apply(gm);
			
			List<GroupMembership> list = allByEntity.get(gmCloned.getEntityId());
			if (list == null)
			{
				list = new ArrayList<>();
				allByEntity.put(gmCloned.getEntityId(), list);
			}
			list.add(gmCloned);

			list = allByGroup.get(gmCloned.getGroup());
			if (list == null)
			{
				list = new ArrayList<>();
				allByGroup.put(gmCloned.getGroup(), list);
			}
			list.add(gmCloned);
		}
	}
	
	synchronized Optional<Boolean> isMember(long entityId, String group)
	{
		if (!cacheComplete)
			return Optional.empty();
		List<GroupMembership> list = allByEntity.get(entityId);
		if (list == null)
			return Optional.of(false);
		for (GroupMembership gm: list)
			if (group.equals(gm.getGroup()))
				return Optional.of(true);
		return Optional.of(false);
	}

	synchronized Optional<List<GroupMembership>> getEntityMembership(long entityId)
	{
		if (!cacheComplete)
			return Optional.empty();
		List<GroupMembership> list = allByEntity.get(entityId);
		if (list == null)
			return Optional.of(Collections.emptyList());
		return Optional.of(cloneList(list));
	}

	synchronized Optional<List<GroupMembership>> getMembers(String group)
	{
		if (!cacheComplete)
			return Optional.empty();
		List<GroupMembership> list = allByGroup.get(group);
		if (list == null)
			return Optional.of(Collections.emptyList());
		return Optional.of(cloneList(list));
	}
	
	private List<GroupMembership> cloneList(List<GroupMembership> src)
	{
		List<GroupMembership> clone = new ArrayList<>(src.size());
		for (GroupMembership gm: src)
			clone.add(cloner.apply(gm));
		return clone;
	}
}
