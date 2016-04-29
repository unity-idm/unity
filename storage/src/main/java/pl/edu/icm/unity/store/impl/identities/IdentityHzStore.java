/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.store.impl.entities.EntityHzStore;
import pl.edu.icm.unity.types.basic.Identity;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;


/**
 * Hazelcast impl of {@link IdentityDAO}
 * @author K. Benedyczak
 */
@Repository(IdentityHzStore.STORE_ID)
public class IdentityHzStore extends GenericNamedHzCRUD<Identity> implements IdentityDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public IdentityHzStore(IdentityRDBMSStore rdbmsStore, EntityHzStore entityDAO)
	{
		super(STORE_ID, NAME, IdentityRDBMSStore.BEAN, rdbmsStore);
		entityDAO.addRemovalHandler(this::cascadeEntityRemoval);
	}

	@Override
	public List<Identity> getByEntity(long entityId)
	{
		TransactionalMap<Long, Identity> hMap = getMap();
		List<Identity> ret = new ArrayList<>();
		EntryObject e = new PredicateBuilder().getEntryObject();
		@SuppressWarnings("unchecked")
		Predicate<Long, Identity> predicate = e.get("entityId").equal(entityId);
		ret.addAll(hMap.values(predicate));
		return ret;
	}
	
	private void cascadeEntityRemoval(long id, String name)
	{
		List<Identity> byEntity = getByEntity(id);
		for (Identity childId: byEntity)
			delete(childId.getName());
	}
	
	@Override
	protected void preUpdateCheck(Identity old, Identity updated)
	{
		if (!old.getTypeId().equals(updated.getTypeId()))
			throw new IllegalArgumentException("Can not change identity type from " + 
					old.getTypeId() + " to " + updated.getTypeId());
		if (!old.getComparableValue().equals(updated.getComparableValue()))
			throw new IllegalArgumentException("Can not change identity value from " + 
					old.getComparableValue() + " to " + updated.getComparableValue());
		
	}
}
