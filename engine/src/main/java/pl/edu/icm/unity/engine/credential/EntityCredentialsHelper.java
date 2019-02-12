/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
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
	private CredentialRepository credentialRepository;
	@Autowired
	private LocalCredentialsRegistry localCredReg;
	@Autowired
	private CredentialReqRepository credentialReqRepository;
	@Autowired
	private AttributesHelper attributesHelper;
	
	
	public CredentialInfo getCredentialInfo(long entityId) throws EngineException
	{
		Map<String, AttributeExt> attributes = attributesHelper.getAllAttributesAsMapOneGroup(entityId, "/");
		String credentialRequirementId = getCredentialReqFromAttribute(attributes);
		CredentialRequirementsHolder credReq = getCredentialRequirements(credentialRequirementId);
		return getCredentialInfoNoQuery(entityId, attributes, credReq, credentialRequirementId);
	}

	public String getCredentialReqFromAttribute(Map<String, AttributeExt> attributes)
	{
		Attribute credReqA = attributes.get(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		if (credReqA == null)
			throw new InternalException("No credential requirement set for an entity"); 
		return (String)credReqA.getValues().get(0);
	}

	public CredentialInfo getCredentialInfoNoQuery(long entityId, Map<String, AttributeExt> attributes, 
			CredentialRequirementsHolder credReq, String credentialRequirementId)
	{
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
		CredentialRequirements requirements = credentialReqRepository.get(requirementName);
		Collection<CredentialDefinition> credDefs = credentialRepository.getCredentialDefinitions();
		return new CredentialRequirementsHolder(localCredReg, requirements, credDefs);
	}
	
	public void setEntityCredentialRequirements(long entityId, String credReqId) 
			throws EngineException
	{
		
		credentialReqRepository.assertExist(credReqId);
		setEntityCredentialRequirementsNoCheck(entityId, credReqId);
	}
	
	public void setEntityCredentialRequirementsNoCheck(long entityId, String credReqId) 
	{
		Attribute credReq = StringAttribute.of(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS,
				"/", credReqId);
		attributesHelper.createOrUpdateAttribute(credReq, entityId);
	}

	
	/**
	 * Sets a credential which was previously prepared (i.e. hashed etc). Absolutely no checking is performed.
	 * @param entityId
	 * @param newCred
	 * @param credentialId
	 * @param sqlMap
	 * @throws EngineException
	 */
	public void setPreviouslyPreparedEntityCredential(long entityId, String newCred, String credentialId)
	{
		String credentialAttributeName = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialId;
		Attribute newCredentialA = StringAttribute.of(credentialAttributeName, 
				"/", Collections.singletonList(newCred));
		attributesHelper.createOrUpdateAttribute(newCredentialA, entityId);
	}
	
	/**
	 * Prepares and sets credential
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param currentRawCredential
	 * @throws EngineException
	 */
	public void setEntityCredential(long entityId, String credentialId, String rawCredential) throws EngineException
	{
		String cred = prepareEntityCredential(entityId, credentialId, rawCredential, true);
		setPreviouslyPreparedEntityCredential(entityId, cred, credentialId);
	}
	
	/**
	 * Prepares and sets credential without verify it
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param currentRawCredential
	 * @throws EngineException
	 */
	public void setEntityCredentialInternalWithoutVerify(long entityId, String credentialId, 
			String rawCredential)  throws EngineException
	{
		String cred = prepareEntityCredential(entityId, credentialId, rawCredential, false);
		setPreviouslyPreparedEntityCredential(entityId, cred, credentialId);
	}
	
	
	/**
	 * Prepares entity's credential (hashes, checks etc). This is internal method which 
	 * doesn't perform any authorization nor argument initialization checking.
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param sqlMap
	 * @throws EngineException
	 */
	private String prepareEntityCredential(long entityId, String credentialId, 
			String rawCredential, boolean verify) throws EngineException
	{
		Map<String, AttributeExt> attributes = attributesHelper.getAllAttributesAsMapOneGroup(entityId, "/");
		
		Attribute credReqA = attributes.get(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		String credentialRequirements = (String)credReqA.getValues().get(0);
		CredentialRequirementsHolder credReqs = getCredentialRequirements(credentialRequirements);
		LocalCredentialVerificator handler = credReqs.getCredentialHandler(credentialId);
		if (handler == null)
			throw new IllegalCredentialException("The credential id is not among the " +
					"entity's credential requirements: " + credentialId);

		String credentialAttributeName = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialId;
		Attribute currentCredentialA = attributes.get(credentialAttributeName);
		String currentCredential = currentCredentialA != null ? 
				(String)currentCredentialA.getValues().get(0) : null;
				
		return handler.prepareCredential(rawCredential, currentCredential, verify);
	}
}



