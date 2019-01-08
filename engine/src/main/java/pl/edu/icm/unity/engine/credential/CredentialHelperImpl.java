/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Default implementation of the credential helper. Immutable.
 * @author K. Benedyczak
 */
@Component
public class CredentialHelperImpl implements CredentialHelper
{
	private AttributeDAO attributeDAO;
	private AttributesHelper attributeHelper;
	private CredentialRepository credentialRepo;
	
	@Autowired
	public CredentialHelperImpl(AttributeDAO attributeDAO, AttributesHelper attributeHelper,
			CredentialRepository credentialRepo)
	{
		this.attributeDAO = attributeDAO;
		this.attributeHelper = attributeHelper;
		this.credentialRepo = credentialRepo;
	}

	@Override
	@Transactional
	public void updateCredential(long entityId, String credentialName, String value) throws EngineException 
	{
		updateCredentialInternal(entityId, credentialName, value);
	}
	
	@Override
	@Transactional
	public void setCredential(long entityId, String credentialName, String value, 
			LocalCredentialVerificator handler) throws EngineException 
	{
		String credentialAttributeName = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialName;
		List<AttributeExt> attributes = attributeDAO.getEntityAttributes(entityId, 
				credentialAttributeName, "/");
		Attribute currentCredentialA = attributes.isEmpty() ? null : attributes.get(0);
		String currentCredential = currentCredentialA != null ? 
				(String)currentCredentialA.getValues().get(0) : null;
		String newValue = handler.prepareCredential(value, currentCredential, true);
		updateCredentialInternal(entityId, credentialName, newValue);
	}
	

	private void updateCredentialInternal(long entityId, String credentialName, String value) 
			throws EngineException 
	{
		String credentialAttributeName = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialName;
		Attribute newCredentialA = StringAttribute.of(credentialAttributeName, 
				"/", Collections.singletonList(value));
		attributeHelper.addSystemAttribute(entityId, newCredentialA, true);
	}
	
	@Override
	@Transactional
	public boolean isCredentialSet(EntityParam entity, String credentialId)
			throws EngineException
	{	
		String credentialAttributeName = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialId;
		List<AttributeExt> entityAttributes = attributeDAO.getEntityAttributes(entity.getEntityId(), credentialAttributeName, "/");
		return !entityAttributes.isEmpty();
	}
	
	@Override
	@Transactional
	public Map<String, CredentialDefinition> getCredentialDefinitions() throws EngineException
	{
		return credentialRepo.getCredentialDefinitions().stream().collect(Collectors
				.toMap(CredentialDefinition::getName, Function.identity()));
	}
}
