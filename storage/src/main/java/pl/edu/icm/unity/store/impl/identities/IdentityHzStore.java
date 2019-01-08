/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;

import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.store.impl.entities.EntityHzStore;
import pl.edu.icm.unity.store.impl.membership.MembershipHzStore;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * Hazelcast impl of {@link IdentityDAO}
 * @author K. Benedyczak
 */
@Repository(IdentityHzStore.STORE_ID)
public class IdentityHzStore extends GenericNamedHzCRUD<StoredIdentity> implements IdentityDAO
{
	public static final String STORE_ID = DAO_ID + "hz";
	private MembershipHzStore membershipStore;

	@Autowired
	public IdentityHzStore(IdentityRDBMSStore rdbmsStore, EntityHzStore entityDAO, MembershipHzStore membershipStore)
	{
		super(STORE_ID, NAME, IdentityRDBMSStore.BEAN, rdbmsStore);
		this.membershipStore = membershipStore;
		entityDAO.addRemovalHandler(this::cascadeEntityRemoval);
	}

	@Override
	public List<StoredIdentity> getByEntityFull(long entityId)
	{
		TransactionalMap<Long, StoredIdentity> hMap = getMap();
		List<StoredIdentity> ret = new ArrayList<>();
		EntryObject e = new PredicateBuilder().getEntryObject();
		@SuppressWarnings("unchecked")
		Predicate<Long, StoredIdentity> predicate = e.get("entityId").equal(entityId);
		ret.addAll(hMap.values(predicate));
		return ret;
	}
	
	@Override
	public List<Identity> getByEntity(long entityId)
	{
		return getByEntityFull(entityId).stream().map(si -> si.getIdentity()).collect(Collectors.toList());
	}
	
	private void cascadeEntityRemoval(long id, String name)
	{
		List<StoredIdentity> byEntity = getByEntityFull(id);
		for (StoredIdentity childId: byEntity)
			delete(childId.getName());
	}
	
	@Override
	protected void preUpdateCheck(StoredIdentity sold, StoredIdentity supdated)
	{
		Identity old = sold.getIdentity();
		Identity updated = supdated.getIdentity();
		if (!old.getTypeId().equals(updated.getTypeId()))
			throw new IllegalArgumentException("Can not change identity type from " + 
					old.getTypeId() + " to " + updated.getTypeId());
		if (!old.getComparableValue().equals(updated.getComparableValue()))
			throw new IllegalArgumentException("Can not change identity value from " + 
					old.getComparableValue() + " to " + updated.getComparableValue());
		
	}

	@Override
	public List<StoredIdentity> getByGroup(String group)
	{
		Set<Long> members = membershipStore.getMembers(group).stream()
				.map(member -> member.getEntityId())
				.collect(Collectors.toSet());
		return getAll().stream()
				.filter(si -> members.contains(si.getEntityId()))
				.collect(Collectors.toList());
	}
}
