/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
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
	public GenericObjectBean toBlob(CredentialDefinition value)
	{
		byte[] contents = JsonUtil.serialize2Bytes(value.toJson());
		return new GenericObjectBean(value.getName(), contents, supportedType);
	}

	@Override
	public CredentialDefinition fromBlob(GenericObjectBean blob)
	{
		return new CredentialDefinition(JsonUtil.parse(blob.getContents()));
	}
}
