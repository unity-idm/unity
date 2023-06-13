/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link CredentialDefinition}
 * 
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
	public GenericObjectBean toBlob(CredentialDefinition value)
	{
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(CredentialDefinitionMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize credential definition to JSON", e);
		}
	}

	@Override
	public CredentialDefinition fromBlob(GenericObjectBean blob)
	{
		try
		{
			return CredentialDefinitionMapper
					.map(jsonMapper.readValue(blob.getContents(), DBCredentialDefinition.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize credential definition from JSON", e);
		}
	}
}
