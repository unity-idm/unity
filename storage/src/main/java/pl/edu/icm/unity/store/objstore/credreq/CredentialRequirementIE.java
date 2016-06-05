/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.credreq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Handles import/export of {@link CredentialRequirements}.
 * @author K. Benedyczak
 */
@Component
public class CredentialRequirementIE extends GenericObjectIEBase<CredentialRequirements>
{
	@Autowired
	public CredentialRequirementIE(CredentialRequirementDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, CredentialRequirements.class, 104, 
				CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE);
	}
}



