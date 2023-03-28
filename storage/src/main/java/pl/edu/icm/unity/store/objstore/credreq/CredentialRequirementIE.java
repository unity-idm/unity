/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.credreq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase2;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Handles import/export of {@link CredentialRequirements}.
 * @author K. Benedyczak
 */
@Component
public class CredentialRequirementIE extends GenericObjectIEBase2<CredentialRequirements>
{
	@Autowired
	public CredentialRequirementIE(CredentialRequirementDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 104, 
				CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE);
	}
	
	@Override
	protected CredentialRequirements convert(ObjectNode src)
	{
		return CredentialRequirementsMapper.map(jsonMapper.convertValue(src, DBCredentialRequirements.class));
	}

	@Override
	protected ObjectNode convert(CredentialRequirements src)
	{
		return jsonMapper.convertValue(CredentialRequirementsMapper.map(src), ObjectNode.class);
	}
}



