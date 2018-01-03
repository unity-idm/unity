/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.util.configuration.PropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * IdP engine is responsible for performing common IdP-related functionality. It resolves the information
 * about the user being queried, applies translation profile on the data and exposes it to the endpoint 
 * requiring the data.
 * 
 * @author K. Benedyczak
 */
class IdPEngineImplBase implements IdPEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdPEngineImplBase.class);

	private AttributesManagement attributesMan;
	private EntityManagement identitiesMan;
	private UserImportSerivce userImportService;
	private OutputProfileExecutor outputProfileExecutor;
	
	IdPEngineImplBase(AttributesManagement attributesMan, 
			EntityManagement identitiesMan,
			UserImportSerivce userImportService,
			OutputProfileExecutor outputProfileExecutor)
	{
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.userImportService = userImportService;
		this.outputProfileExecutor = outputProfileExecutor;
	}

	@Override
	public TranslationResult obtainUserInformationWithEnrichingImport(EntityParam entity,
			String group, String profile, String requester, String protocol,
			String protocolSubType, boolean allowIdentityCreate,
			PropertiesHelper importsConfig) throws EngineException
	{
		Entity fullEntity = identitiesMan.getEntity(entity, requester, allowIdentityCreate, group);
		Map<String, String> firstIdentitiesByType = new HashMap<>();
		fullEntity.getIdentities().forEach(id -> {
			if (!firstIdentitiesByType.containsKey(id.getTypeId()))
				firstIdentitiesByType.put(id.getTypeId(), id.getValue());
		});
		List<UserImportSpec> userImports = CommonIdPProperties.getUserImports(
				importsConfig, firstIdentitiesByType);
		List<ImportResult> importResult = userImportService.importToExistingUser(
				userImports, fullEntity.getIdentities().get(0));
		
		return obtainUserInformationPostImport(entity, fullEntity, group, profile, 
				requester, protocol, protocolSubType, 
				assembleImportStatus(importResult));
	}
	
	@Override
	public TranslationResult obtainUserInformationWithEarlyImport(IdentityTaV identity, String group, String profile,
			String requester, String protocol, String protocolSubType, boolean allowIdentityCreate,
			PropertiesHelper config)
			throws EngineException
	{
		List<UserImportSpec> userImports = CommonIdPProperties.getUserImportsLegacy(
				config, identity.getValue(), identity.getTypeId());
		List<ImportResult> importResult = userImportService.importUser(userImports);
		EntityParam entity = new EntityParam(identity);
		Entity fullEntity = identitiesMan.getEntity(entity, requester, allowIdentityCreate, group);
		
		return obtainUserInformationPostImport(entity, fullEntity, group, profile, 
				requester, protocol, protocolSubType, 
				assembleImportStatus(importResult));
	}
	
	private TranslationResult obtainUserInformationPostImport(EntityParam entity, Entity fullEntity,
			String group, String profile,
			String requester, String protocol, String protocolSubType,
			Map<String, Status> importStatus) throws EngineException
	{
		Collection<String> allGroups = identitiesMan.getGroups(entity).keySet();
		Collection<AttributeExt> allAttributes = attributesMan.getAttributes(
				entity, group, null);
		if (log.isTraceEnabled())
			log.trace("Attributes to be returned (before postprocessing): " + 
					allAttributes + "\nGroups: " + allGroups + "\nIdentities: " + 
					fullEntity.getIdentities());
		TranslationInput input = new TranslationInput(allAttributes, fullEntity, group, allGroups, 
				requester, protocol, protocolSubType, importStatus);
		return outputProfileExecutor.execute(profile, input);
	}

	private Map<String, Status> assembleImportStatus(List<ImportResult> results)
	{
		return results.stream().collect(
				Collectors.toMap(r -> r.importerKey, 
						r -> r.authenticationResult.getStatus()));
	}

	/**
	 * Returns an {@link IdentityParam} out of valid identities which is either equal to the provided selected 
	 * identity or the first one. This method properly compares the identity values.
	 * @param userInfo
	 * @param validIdentities
	 * @param selectedIdentity
	 * @return
	 * @throws EngineException
	 * @throws SAMLRequesterException
	 */
	@Override
	public IdentityParam getIdentity(List<IdentityParam> validIdentities, String selectedIdentity) 
			throws EngineException, SAMLRequesterException
	{
		if (validIdentities.size() > 0)
		{
			for (IdentityParam id: validIdentities)
			{
				if (id instanceof Identity)
				{
					if (((Identity)id).getComparableValue().equals(selectedIdentity))
					{
						return id;
					}
				} else
				{
					if (id.getValue().equals(selectedIdentity))
						return id;
				}
			}
		}
		return validIdentities.get(0);
	}
}
