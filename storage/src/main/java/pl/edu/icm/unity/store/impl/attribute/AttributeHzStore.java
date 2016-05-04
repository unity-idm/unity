/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.StoredAttribute;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;
import pl.edu.icm.unity.types.basic.AttributeExt;


/**
 * Hazelcast implementation of attribute store.
 * 
 * @author K. Benedyczak
 */
@Repository(AttributeHzStore.STORE_ID)
public class AttributeHzStore extends GenericBasicHzCRUD<StoredAttribute> implements AttributeDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public AttributeHzStore(AttributeRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, AttributeRDBMSStore.BEAN, rdbmsDAO);
	}

	@Override
	public void updateAttribute(StoredAttribute a)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAttribute(String attribute, long entityId, String group)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAttributesInGroup(long entityId, String group)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<AttributeExt<?>> getAttributes(String attribute, long entityId, String group)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
