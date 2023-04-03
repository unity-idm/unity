/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Handles import/export of {@link CredentialDefinition}.
 * @author K. Benedyczak
 */
@Component
public class CredentialIE extends GenericObjectIEBase<CredentialDefinition>
{
	@Autowired
	public CredentialIE(CredentialDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 103, CredentialHandler.CREDENTIAL_OBJECT_TYPE);
	}
	
	@Override
	protected CredentialDefinition convert(ObjectNode src)
	{
		return CredentialDefinitionMapper.map(jsonMapper.convertValue(src, DBCredentialDefinition.class));
	}

	@Override
	protected ObjectNode convert(CredentialDefinition src)
	{
		return jsonMapper.convertValue(CredentialDefinitionMapper.map(src), ObjectNode.class);
	}
}



