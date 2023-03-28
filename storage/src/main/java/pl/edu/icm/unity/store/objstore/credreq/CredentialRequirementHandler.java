/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.credreq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Handler for {@link CredentialRequirements}
 * @author K. Benedyczak
 */
@Component
public class CredentialRequirementHandler extends DefaultEntityHandler<CredentialRequirements>
{
	public static final String CREDENTIAL_REQ_OBJECT_TYPE = "credentialRequirement";
	
	@Autowired
	public CredentialRequirementHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, CREDENTIAL_REQ_OBJECT_TYPE, CredentialRequirements.class);
	}

	@Override
	public GenericObjectBean toBlob(CredentialRequirements value)
	{
		try
		{
			return new GenericObjectBean(value.getName(), jsonMapper.writeValueAsBytes(CredentialRequirementsMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize credential requirements to JSON", e);
		}
	}

	@Override
	public CredentialRequirements fromBlob(GenericObjectBean blob)
	{
		try
		{
			return CredentialRequirementsMapper.map(jsonMapper.readValue(blob.getContents(), DBCredentialRequirements.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize credential requirements from JSON", e);
		}
	}
}
