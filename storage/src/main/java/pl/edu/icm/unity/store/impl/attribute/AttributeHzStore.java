/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PredicateBuilder;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.entities.EntityHzStore;
import pl.edu.icm.unity.store.impl.groups.GroupHzStore;
import pl.edu.icm.unity.store.impl.membership.MembershipHzStore;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;


/**
 * Hazelcast implementation of attribute store.
 * 
 * @author K. Benedyczak
 */
@Repository(AttributeHzStore.STORE_ID)
public class AttributeHzStore extends GenericBasicHzCRUD<StoredAttribute> implements AttributeDAO
{
	public static final String STORE_ID = DAO_ID + "hz";
	private MembershipHzStore membershipDAO;

	@Autowired
	public AttributeHzStore(AttributeRDBMSStore rdbmsDAO, AttributeTypeHzStore atDAO, EntityHzStore entityDAO,
			GroupHzStore groupDAO, MembershipHzStore membershipDAO)
	{
		super(STORE_ID, NAME, AttributeRDBMSStore.BEAN, rdbmsDAO);
		this.membershipDAO = membershipDAO;
		atDAO.addRemovalHandler(this::cascadeAttributeTypeRemoval);
		atDAO.addUpdateHandler(this::cascadeAttributeTypeUpdate);
		entityDAO.addRemovalHandler(this::cascadeEntityRemoval);
		groupDAO.addRemovalHandler(this::cascadeGroupRemoval);
		groupDAO.addUpdateHandler(this::cascadeGroupUpdate);
	}

	private void cascadeAttributeTypeRemoval(long key, String name)
	{
		PredicateBuilder pBuilder = getAttributePredicate(name, null, null);
		genericDelete(pBuilder);
	}
	
	private void cascadeEntityRemoval(long key, String name)
	{
		PredicateBuilder pBuilder = getAttributePredicate(null, key, null);
		genericDelete(pBuilder);
	}

	private void cascadeGroupRemoval(long key, String name)
	{
		PredicateBuilder pBuilder = getAttributePredicate(null, null, name);
		genericDelete(pBuilder);
	}
	
	private void cascadeAttributeTypeUpdate(long modifiedId, String modifiedName, AttributeType newValue)
	{
		String newName = newValue.getName();
		if (newName.equals(modifiedName))
			return;

		PredicateBuilder pBuilder = getAttributePredicate(modifiedName, null, null);
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		Set<Long> keys = hMap.keySet(pBuilder);
		for (Long key: keys)
		{
			StoredAttribute sa = hMap.get(key);
			StoredAttribute clone = new StoredAttribute(sa);
			clone.getAttribute().setName(newName);
			preUpdateCheck(sa, clone);
			firePreUpdate(key, null, clone, sa);
			hMap.put(key, clone);
		}
	}

	private void cascadeGroupUpdate(long modifiedId, String modifiedName, Group newValue)
	{
		String newName = newValue.getName();
		if (newName.equals(modifiedName))
			return;

		PredicateBuilder pBuilder = getAttributePredicate(null, null, modifiedName);
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		Set<Long> keys = hMap.keySet(pBuilder);
		for (Long key: keys)
		{
			StoredAttribute sa = hMap.get(key);
			StoredAttribute clone = new StoredAttribute(sa);
			clone.getAttribute().setGroupPath(newName);
			preUpdateCheck(sa, clone);
			firePreUpdate(key, null, clone, sa);
			hMap.put(key, clone);
		}
	}
	
	@Override
	public void updateAttribute(StoredAttribute a)
	{
		updateAttributeHZOnly(a);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"updateAttribute", a));
	}

	private void updateAttributeHZOnly(StoredAttribute a)
	{
		PredicateBuilder pBuilder = getAttributePredicate(a.getAttribute().getName(), 
				a.getEntityId(), a.getAttribute().getGroupPath());
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		Set<Long> values = hMap.keySet(pBuilder);
		
		if (values.isEmpty())
			throw new IllegalArgumentException("attribute [" + a.getAttribute().getName() + 
					"] does not exist");
		long id = values.iterator().next();
		StoredAttribute old = hMap.get(id);
		preUpdateCheck(old, a);
		firePreUpdate(id, null, a, old);
		hMap.put(id, a);
	}
	
	@Override
	public void deleteAttribute(String attribute, long entityId, String group)
	{
		PredicateBuilder pBuilder = getAttributePredicate(attribute, entityId, group);
		genericDelete(pBuilder);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"deleteAttribute", attribute, entityId, group));
	}

	@Override
	public void deleteAttributesInGroup(long entityId, String group)
	{
		PredicateBuilder pBuilder = getAttributePredicate(null, entityId, group);
		genericDelete(pBuilder);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"deleteAttributesInGroup", entityId, group));
	}

	private void genericDelete(PredicateBuilder pBuilder)
	{
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		Set<Long> values = hMap.keySet(pBuilder);
		for (Long key: values)
		{
			firePreRemove(key, null, hMap.get(key));
			hMap.remove(key);
		}
	}

	@Override
	public List<StoredAttribute> getAttributes(String attribute, Long entityId, String group)
	{
		PredicateBuilder pBuilder = getAttributePredicate(attribute, entityId, group);
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		Collection<StoredAttribute> values = hMap.values(pBuilder);
		return new ArrayList<>(values);
	}
	
	@Override
	public List<AttributeExt> getEntityAttributes(long entityId, String attribute, String group)
	{
		PredicateBuilder pBuilder = getAttributePredicate(attribute, entityId, group);
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		Collection<StoredAttribute> values = hMap.values(pBuilder);
		List<AttributeExt> ret = new ArrayList<>(values.size());
		for (StoredAttribute sa: values)
			ret.add(sa.getAttribute());
		return ret;
	}
	
	private PredicateBuilder getAttributePredicate(String attribute, Long entityId, String group)
	{
		EntryObject e = new PredicateBuilder().getEntryObject();
		PredicateBuilder pBuilder = null;
		if (entityId != null)
			pBuilder = safeAdd(pBuilder, e.get("entityId").equal(entityId));
		if (attribute != null)
			pBuilder = safeAdd(pBuilder, e.get("name").equal(attribute));
		if (group != null)
			pBuilder = safeAdd(pBuilder, e.get("group").equal(group));
		return pBuilder;
	}
	
	private PredicateBuilder safeAdd(PredicateBuilder existing, PredicateBuilder condition)
	{
		return existing == null ? condition : existing.and(condition);
	}

	@Override
	public List<StoredAttribute> getAttributesOfGroupMembers(String group)
	{
		Set<Long> members = membershipDAO.getMembers(group).stream()
				.map(mem -> mem.getEntityId())
				.collect(Collectors.toSet());
		TransactionalMap<Long, StoredAttribute> hMap = getMap();
		return hMap.values().stream()
				.filter(sa -> members.contains(sa.getEntityId()))
				.collect(Collectors.toList());
	}
}
