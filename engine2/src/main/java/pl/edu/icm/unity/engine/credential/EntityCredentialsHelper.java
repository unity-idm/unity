/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Low level helper operations on entity's credential and credential requirements.
 * @author K. Benedyczak
 */
@Component
public class EntityCredentialsHelper
{
	@Autowired
	private CredentialDB credentialDB;
	@Autowired
	private LocalCredentialsRegistry localCredReg;
	@Autowired
	private CredentialRequirementDB credentialRequirementDB;
	@Autowired
	private AttributesHelper attributesHelper;
	
	
	public CredentialInfo getCredentialInfo(long entityId) throws EngineException
	{
		Map<String, AttributeExt> attributes = attributesHelper.getAllAttributesAsMapOneGroup(entityId, "/");
		
		Attribute credReqA = attributes.get(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		if (credReqA == null)
			throw new InternalException("No credential requirement set for an entity"); 
		String credentialRequirementId = (String)credReqA.getValues().get(0);
		
		CredentialRequirementsHolder credReq = getCredentialRequirements(
				credentialRequirementId);
		Set<String> required = credReq.getCredentialRequirements().getRequiredCredentials();
		Map<String, CredentialPublicInformation> credentialsState = new HashMap<>();
		for (String cd: required)
		{
			LocalCredentialVerificator handler = credReq.getCredentialHandler(cd);
			Attribute currentCredA = attributes.get(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+cd);
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			
			credentialsState.put(cd, handler.checkCredentialState(currentCred));
		}
		
		return new CredentialInfo(credentialRequirementId, credentialsState);
	}
	

	
	public CredentialRequirementsHolder getCredentialRequirements(String requirementName) 
			throws EngineException
	{
		CredentialRequirements requirements = credentialRequirementDB.get(requirementName);
		List<CredentialDefinition> credDefs = credentialDB.getAll();
		return new CredentialRequirementsHolder(localCredReg, requirements, credDefs);
	}
	

	public void setEntityCredentialRequirements(long entityId, String credReqId) 
			throws EngineException
	{
		if (!credentialRequirementDB.exists(credReqId))
			throw new IllegalArgumentException("There is no required credential set with id " + credReqId);
		setEntityCredentialRequirementsNoCheck(entityId, credReqId);
	}
	
	public void setEntityCredentialRequirementsNoCheck(long entityId, String credReqId) 
	{
		StringAttribute credReq = new StringAttribute(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS,
				"/", credReqId);
		attributesHelper.createOrUpdateAttribute(credReq, entityId);
	}
}
