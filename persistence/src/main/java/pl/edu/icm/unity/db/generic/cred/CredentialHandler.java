/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.cred;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Handler for {@link CredentialDefinition}
 * @author K. Benedyczak
 */
@Component
public class CredentialHandler extends DefaultEntityHandler<CredentialDefinition>
{
	public static final String CREDENTIAL_OBJECT_TYPE = "credential";
	
	@Autowired
	public CredentialHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, CREDENTIAL_OBJECT_TYPE, CredentialDefinition.class);
	}

	@Override
	public GenericObjectBean toBlob(CredentialDefinition value, SqlSession sql)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(value);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize credential to JSON", e);
		}
	}

	@Override
	public CredentialDefinition fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			return jsonMapper.readValue(blob.getContents(), CredentialDefinition.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize credential from JSON", e);
		}
	}
}
