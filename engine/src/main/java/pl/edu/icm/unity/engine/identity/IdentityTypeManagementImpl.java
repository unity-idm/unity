/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implementation of identity types management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
@Primary
public class IdentityTypeManagementImpl implements IdentityTypesManagement
{
	private IdentityTypeDAO dbIdentities;
	private AttributeTypeDAO dbAttributes;
	private InternalAuthorizationManager authz;
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired
	public IdentityTypeManagementImpl(IdentityTypeDAO dbIdentities, AttributeTypeDAO dbAttributes, 
			InternalAuthorizationManager authz, IdentityTypesRegistry idTypesRegistry)
	{
		this.dbIdentities = dbIdentities;
		this.dbAttributes = dbAttributes;
		this.authz = authz;
		this.idTypesRegistry = idTypesRegistry;
	}

	@Override
	@Transactional
	public Collection<IdentityType> getIdentityTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return dbIdentities.getAll();
	}

	@Transactional
	@Override
	public void updateIdentityType(IdentityType toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toUpdate.getIdentityTypeProvider());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		if (toUpdate.getMinInstances() < 0)
			throw new IllegalAttributeTypeException("Minimum number of instances "
					+ "can not be negative");
		if (toUpdate.getMinVerifiedInstances() > toUpdate.getMinInstances())
			throw new IllegalAttributeTypeException("Minimum number of verified instances "
					+ "can not be larger then the regular minimum of instances");
		if (toUpdate.getMinInstances() > toUpdate.getMaxInstances())
			throw new IllegalAttributeTypeException("Minimum number of instances "
					+ "can not be larger then the maximum");


		Map<String, AttributeType> atsMap = dbAttributes.getAllAsMap();
		Map<String, String> extractedAts = toUpdate.getExtractedAttributes();
		Set<AttributeType> supportedForExtraction = idTypeDef.getAttributesSupportedForExtraction();
		Map<String, AttributeType> supportedForExtractionMap = new HashMap<>();
		for (AttributeType at: supportedForExtraction)
			supportedForExtractionMap.put(at.getName(), at);

		for (Map.Entry<String, String> extracted: extractedAts.entrySet())
		{
			AttributeType type = atsMap.get(extracted.getValue());
			if (type == null)
				throw new IllegalAttributeTypeException("Can not extract attribute " + 
						extracted.getKey() + " as " + extracted.getValue() + 
						" because the latter is not defined in the system");
			AttributeType supportedType = supportedForExtractionMap.get(extracted.getKey());
			if (supportedType == null)
				throw new IllegalAttributeTypeException("Can not extract attribute " + 
						extracted.getKey() + " as " + extracted.getValue() + 
						" because the former is not supported by the identity provider");
		}
		dbIdentities.update(toUpdate);
	}
}
