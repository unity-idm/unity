/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.types.basic.IdentityType;

import com.hazelcast.core.HazelcastInstance;

/**
 * Hazelcast store of {@link IdentityType}s.
 * 
 * @author K. Benedyczak
 */
@Repository(IdentityTypeHzStore.STORE_ID)
public class IdentityTypeHzStore extends GenericNamedHzCRUD<IdentityType> implements IdentityTypeDAO
{
	public static final String STORE_ID = DAO_ID + "hz";
	private static final String NAME = "identity type";

	@Autowired
	public IdentityTypeHzStore(IdentityTypeRDBMSStore rdbmsStore, HazelcastInstance hzInstance)
	{
		super(STORE_ID, NAME, IdentityTypeRDBMSStore.BEAN, rdbmsStore, hzInstance);
	}

	
	@Override
	public long create(IdentityType idType) throws IllegalArgumentException
	{
		if (idType.getDescription() == null)
			idType.setDescription(idType.getIdentityTypeProvider().getDefaultDescription());
		return super.create(idType);
	}
}
