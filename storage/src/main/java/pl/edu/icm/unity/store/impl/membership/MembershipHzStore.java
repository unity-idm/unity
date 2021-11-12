/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.ReferenceUpdateHandler.PlannedUpdateEvent;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.hz.HzDAO;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.impl.entities.EntityHzStore;
import pl.edu.icm.unity.store.impl.groups.GroupHzStore;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;


/**
 * Hazelcast impl of {@link MembershipDAO}.
 * 
 * Implementation is based on two maps, one indexed with entities another with groups - 
 * for quick lookup (duplicated values are anyway very small). Each of maps contains
 * another map as value - indexed with group and entity respectively. This allows for 
 * quick search in all cases.
 * 
 * 
 * @author K. Benedyczak
 */
@Repository(MembershipHzStore.STORE_ID)
public class MembershipHzStore implements MembershipDAO, HzDAO
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, MembershipHzStore.class);

	public static final String STORE_ID = DAO_ID + "hz";

	private static final String RDBMS_DAO_NAME = MembershipRDBMSStore.BEAN;

	private MembershipRDBMSStore rdbmsStore;

	private GroupDAO groupDAO;
	
	@Autowired
	public MembershipHzStore(MembershipRDBMSStore rdbmsStore, GroupHzStore groupDAO, 
			EntityHzStore entityDAO)
	{
		this.rdbmsStore = rdbmsStore;
		this.groupDAO = groupDAO;
		groupDAO.addRemovalHandler(this::groupRemoved);
		groupDAO.addUpdateHandler(this::groupUpdated);
		entityDAO.addRemovalHandler(this::entityRemoved);
	}

	private void groupUpdated(PlannedUpdateEvent<Group> update)
	{
		if (update.modifiedName.equals(update.newValue.getName()))
			return;
		
		TransactionalMap<String, Map<Long, GroupMembership>> byGroupMap = getByGroupMap();
		TransactionalMap<Long, Map<String, GroupMembership>> byEntityMap = getByEntityMap();
		Map<Long, GroupMembership> map = byGroupMap.remove(update.modifiedName);
		if (map == null)
			return;
		for (Map.Entry<Long, GroupMembership> e: map.entrySet())
		{
			GroupMembership gm = e.getValue();
			gm.setGroup(update.newValue.getName());
			
			Map<String, GroupMembership> entityMemberships = byEntityMap.get(e.getKey());
			GroupMembership removed = entityMemberships.remove(update.modifiedName);
			GroupMembership cloned = new GroupMembership(removed);
			cloned.setGroup(update.newValue.getName());
			entityMemberships.put(update.newValue.getName(), cloned);
			byEntityMap.put(e.getKey(), entityMemberships);
		}
		byGroupMap.put(update.newValue.getName(), map);
		
	}
	
	private void groupRemoved(long removedId, String removedName)
	{
		cascadeRemoved(getByGroupMap().get(removedName));
	}

	private void entityRemoved(long removedId, String removedName)
	{
		cascadeRemoved(getByEntityMap().get(removedId));
	}

	private void cascadeRemoved(Map<?, GroupMembership> toRemove)
	{
		if (toRemove == null)
			return;
		for (GroupMembership gm: toRemove.values())
			deleteByKeyHzOnly(gm.getEntityId(), gm.getGroup());
	}
	
	@Override
	public void populateFromRDBMS(HazelcastInstance hzInstance)
	{
		log.info("Loading group memberships from persistent storage");
		List<Group> allGroups = groupDAO.getAll();
		for (Group group: allGroups)
		{
			List<GroupMembership> all = rdbmsStore.getMembers(group.getName());
			for (GroupMembership element: all)
				createHZOnly(element);
		}
		log.info("Loaded {} group memberships from persistent storage", allGroups.size());
	}

	@Override
	public void create(GroupMembership obj)
	{
		createHZOnly(obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(
				RDBMS_DAO_NAME, "create", new GroupMembership(obj)));
	}

	public void createHZOnly(GroupMembership obj)
	{
		createByEntity(obj);
		createByGroup(obj);
	}
	
	private void createByEntity(GroupMembership obj)
	{
		TransactionalMap<Long, Map<String, GroupMembership>> mapByEntity = getByEntityMap();
		Map<String, GroupMembership> map = mapByEntity.get(obj.getEntityId());
		if (map == null)
			map = new HashMap<>(128);
		
		map.put(obj.getGroup(), obj);
		mapByEntity.put(obj.getEntityId(), map);
	}

	private void createByGroup(GroupMembership obj)
	{
		TransactionalMap<String, Map<Long, GroupMembership>> mapByGroup = getByGroupMap();
		Map<Long, GroupMembership> map = mapByGroup.get(obj.getGroup());
		if (map == null)
			map = new HashMap<>();
		
		map.put(obj.getEntityId(), obj);
		mapByGroup.put(obj.getGroup(), map);
	}
	
	@Override
	public void deleteByKey(long entityId, String group)
	{
		deleteByKeyHzOnly(entityId, group);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(
				RDBMS_DAO_NAME, "deleteByKey", entityId, group));
	}

	public void deleteByKeyHzOnly(long entityId, String group)
	{
		removeFromEntitesMap(entityId, group);
		removeFromGroupsMap(entityId, group);
	}
	
	private void removeFromEntitesMap(long entityId, String group)
	{
		TransactionalMap<Long, Map<String, GroupMembership>> mapByEntity = getByEntityMap();
		Map<String, GroupMembership> entityMembership = mapByEntity.get(entityId);
		if (entityMembership != null)
		{
			GroupMembership removed = entityMembership.remove(group);
			if (removed != null)
			{
				if (entityMembership.isEmpty())
					mapByEntity.remove(entityId);
				else
					mapByEntity.put(entityId, entityMembership);
				return;
			}
		}
		throw new IllegalArgumentException("Entity " + entityId + 
				" is not a member of group " + group);
	}

	private void removeFromGroupsMap(long entityId, String group)
	{
		TransactionalMap<String, Map<Long, GroupMembership>> mapByGroup = getByGroupMap();
		Map<Long, GroupMembership> groupMembers = mapByGroup.get(group);
		if (groupMembers != null)
		{
			GroupMembership removed = groupMembers.remove(entityId);
			if (removed != null)
			{
				if (groupMembers.isEmpty())
					mapByGroup.remove(group);
				else
					mapByGroup.put(group, groupMembers);
			}
		}
	}
	
	@Override
	public boolean isMember(long entityId, String group)
	{
		TransactionalMap<Long, Map<String, GroupMembership>> map = getByEntityMap();
		Map<String, GroupMembership> memberships = map.get(entityId);
		if (memberships == null)
			return false;
		return memberships.containsKey(group);
	}

	@Override
	public List<GroupMembership> getEntityMembership(long entityId)
	{
		TransactionalMap<Long, Map<String, GroupMembership>> map = getByEntityMap();
		Map<String, GroupMembership> memberships = map.get(entityId);
		if (memberships == null)
			return new ArrayList<>(0);
		return new ArrayList<>(memberships.values());
	}

	@Override
	public List<GroupMembership> getMembers(String group)
	{
		TransactionalMap<String, Map<Long, GroupMembership>> byGroupMap = getByGroupMap();
		Map<Long, GroupMembership> map = byGroupMap.get(group);
		if (map == null)
			return new ArrayList<>(0);
		return new ArrayList<>(map.values());
	}

	@Override
	public List<GroupMembership> getAll()
	{
		TransactionalMap<String, Map<Long, GroupMembership>> byGroupMap = getByGroupMap();
		List<GroupMembership> ret = new ArrayList<>(1000);
		for (Map<Long, GroupMembership> map: byGroupMap.values())
			ret.addAll(map.values());
		return ret;
	}
	
	private TransactionalMap<Long, Map<String, GroupMembership>> getByEntityMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID + "_byEntity");
	}

	private TransactionalMap<String, Map<Long, GroupMembership>> getByGroupMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID + "_byGroup");
	}

	@Override
	public void createList(ArrayList<GroupMembership> memberships)
	{
		throw new UnsupportedOperationException();
	}
}
