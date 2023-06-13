/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Handles serialization of {@link IdentityType} metadata. The metadata
 * is common for all identity types.
 * @author K. Benedyczak
 */
@Component
class IdentityTypeJsonSerializer implements RDBMSObjectSerializer<IdentityType, BaseBean>
{
	@Autowired
	private ObjectMapper jsonMapper;
	
	@Override
	public IdentityType fromDB(BaseBean raw)
	{
		try
		{
			return IdentityTypeBaseMapper.map(jsonMapper.readValue(raw.getContents(), DBIdentityTypeBase.class),
					raw.getName());
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing identity type from DB", e);
		}
	}

	@Override
	public BaseBean toDB(IdentityType idType)
	{
		BaseBean toAdd = new BaseBean();
		toAdd.setName(idType.getName());
		try
		{
			toAdd.setContents(jsonMapper.writeValueAsBytes(IdentityTypeBaseMapper.map(idType)));

		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving identity type to DB", e);
		}

		return toAdd;
	}
}



