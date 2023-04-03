/*
N * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeRDBMSStore;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * Identity JSON and DB serialization
 * @author K. Benedyczak
 */
@Component
class IdentityJsonSerializer implements RDBMSObjectSerializer<StoredIdentity, IdentityBean>
{
	private final IdentityTypeRDBMSStore idTypeDAO;
	
	IdentityJsonSerializer(IdentityTypeRDBMSStore idTypeDAO)
	{
		this.idTypeDAO = idTypeDAO;
	}

	@Override
	public IdentityBean toDB(StoredIdentity sobject)
	{
		Identity object = sobject.getIdentity();
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(object.getEntityId());
		idB.setName(sobject.getName());
		long typeKey = idTypeDAO.getKeyForName(object.getTypeId());
		idB.setTypeId(typeKey);
		try
		{
			idB.setContents(Constants.MAPPER.writeValueAsBytes(IdentityBaseMapper.map(object)));
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving identity to DB", e);
		}
		return idB;
	}

	@Override
	public StoredIdentity fromDB(IdentityBean bean)
	{
		try
		{
			return new StoredIdentity(
					IdentityBaseMapper.map(Constants.MAPPER.readValue(bean.getContents(), DBIdentityBase.class), bean.getTypeName(),bean.getEntityId()));
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing identity from DB", e);
		}
	}
}
