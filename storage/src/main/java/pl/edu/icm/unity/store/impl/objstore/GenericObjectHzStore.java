/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PredicateBuilder;

import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;


/**
 * Hazelcast implementation of {@link ObjectStoreDAO}.
 * 
 * @author K. Benedyczak
 */
@Repository(GenericObjectHzStore.STORE_ID)
public class GenericObjectHzStore extends GenericBasicHzCRUD<GenericObjectBean> implements ObjectStoreDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public GenericObjectHzStore(GenericRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, GenericRDBMSStore.BEAN, rdbmsDAO);
	}

	@Override
	protected long createNoPropagateToRDBMS(GenericObjectBean obj) throws IllegalArgumentException
	{
		TransactionalMap<Long, GenericObjectBean> hMap = getMap();
		long key = index.incrementAndGet();
		while (hMap.containsKey(key))
			key = index.incrementAndGet();
		obj.setId(key);
		hMap.put(key, obj);
		return key;
	}
	
	@Override
	public Set<String> getNamesOfType(String type)
	{
		Set<String> ret = new HashSet<>();
		TransactionalMap<Long, GenericObjectBean> hMap = getMap();
		PredicateBuilder pBuilder = getPredicate(type, null);
		Collection<GenericObjectBean> values = hMap.values(pBuilder);
		for (GenericObjectBean v: values)
			ret.add(v.getName());
		return ret;
	}

	@Override
	public List<GenericObjectBean> getObjectsOfType(String type)
	{
		TransactionalMap<Long, GenericObjectBean> hMap = getMap();
		PredicateBuilder pBuilder = getPredicate(type, null);
		Collection<GenericObjectBean> values = hMap.values(pBuilder);
		return new ArrayList<>(values);
	}

	@Override
	public GenericObjectBean getObjectByNameType(String name, String type)
	{
		TransactionalMap<Long, GenericObjectBean> hMap = getMap();
		PredicateBuilder pBuilder = getPredicate(type, name);
		Collection<GenericObjectBean> values = hMap.values(pBuilder);
		if (values.isEmpty())
			return null;
		return values.iterator().next();
	}

	public GenericObjectBean getObjectByNameTypeNonNull(String name, String type)
	{
		GenericObjectBean ret = getObjectByNameType(name, type);
		if (ret == null)
			throw new IllegalArgumentException("Object with key [" + type + "//" + name +
					"] does not exist");
		return ret;
	}

	@Override
	public Set<String> getObjectTypes()
	{
		Set<String> ret = new HashSet<>();
		TransactionalMap<Long, GenericObjectBean> hMap = getMap();
		Collection<GenericObjectBean> values = hMap.values();
		for (GenericObjectBean v: values)
			ret.add(v.getType());
		return ret;
	}

	@Override
	public void removeObject(String name, String type)
	{
		GenericObjectBean toRemove = getObjectByNameTypeNonNull(name, type);
		deleteByKey(toRemove.getId());
	}

	@Override
	public void removeObjectsByType(String type)
	{
		List<GenericObjectBean> toRemove = getObjectsOfType(type);
		for (GenericObjectBean r: toRemove)
			deleteByKey(r.getId());
	}

	@Override
	public void updateObject(String name, String type, GenericObjectBean obj)
	{
		GenericObjectBean toUpdate = getObjectByNameTypeNonNull(name, type);
		toUpdate.setContents(obj.getContents());
		toUpdate.setName(obj.getName());
		toUpdate.setLastUpdate(obj.getLastUpdate());
		updateByKey(toUpdate.getId(), toUpdate);
	}
	
	@Override
	protected void preUpdateCheck(GenericObjectBean old, GenericObjectBean updated)
	{
		if (!old.getType().equals(updated.getType()))
			throw new IllegalArgumentException("Changing object type is illegal");
		if ((old.getSubType() != null && !old.getSubType().equals(updated.getSubType())) ||
				(old.getSubType() == null && updated.getSubType() != null))
			throw new IllegalArgumentException("Changing object subtype is illegal");
	}
	
	private PredicateBuilder getPredicate(String type, String value)
	{
		EntryObject e = new PredicateBuilder().getEntryObject();
		PredicateBuilder pBuilder = null;
		if (type != null)
			pBuilder = safeAdd(pBuilder, e.get("type").equal(type));
		if (value != null)
			pBuilder = safeAdd(pBuilder, e.get("name").equal(value));
		return pBuilder;
	}
	
	private PredicateBuilder safeAdd(PredicateBuilder existing, PredicateBuilder condition)
	{
		return existing == null ? condition : existing.and(condition);
	}
}
