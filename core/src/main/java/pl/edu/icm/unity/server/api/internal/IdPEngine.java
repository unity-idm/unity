/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.server.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * IdP engine is responsible for performing common IdP-related functionality. It resolves the information
 * about the user being queried, applies translation profile on the data and exposes it to the endpoint 
 * requiring the data.
 * 
 * @author K. Benedyczak
 */
@Component
public class IdPEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdPEngine.class);
	private AttributesManagement attributesMan;
	private IdentitiesManagement identitiesMan;
	private OutputTranslationEngine translationEngine;
	private TranslationProfileManagement profileManagement;
	private UserImportSerivce userImportService;
	
	@Autowired
	public IdPEngine(AttributesManagement attributesMan, IdentitiesManagement identitiesMan,
			OutputTranslationEngine translationEngine,
			@Qualifier("insecure") TranslationProfileManagement profileManagement,
			UserImportSerivce userImportService)
	{
		super();
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.translationEngine = translationEngine;
		this.profileManagement = profileManagement;
		this.userImportService = userImportService;
	}



	/**
	 * Obtains a complete and translated information about entity, authorized to be published.
	 * @param entity entity for which the query is performed
	 * @param group the group from which attributes shall be resolved
	 * @param profile output translation profile to be consulted. Can be null -> then default profile is used. 
	 * @param requester identity of requester
	 * @param protocol identifier of access protocol
	 * @param protocolSubType sub identifier of protocol (e.g. binding)
	 * @param allowIdentityCreate whether a dynamic id can be established
	 * @return obtained data
	 * @throws EngineException
	 */
	public TranslationResult obtainUserInformation(EntityParam entity, String group, String profile,
			String requester, String protocol, String protocolSubType, boolean allowIdentityCreate,
			boolean triggerImport) 
			throws EngineException
	{
		IdentityTaV identityTaV = entity.getIdentity();
		if (identityTaV != null && triggerImport)
			userImportService.importUser(identityTaV.getValue(), identityTaV.getTypeId());
		Collection<String> allGroups = identitiesMan.getGroups(entity).keySet();
		Collection<AttributeExt<?>> allAttributes = attributesMan.getAttributes(
				entity, group, null);
		Entity fullEntity = identitiesMan.getEntity(entity, requester, allowIdentityCreate, group);
		if (log.isTraceEnabled())
			log.trace("Attributes to be returned (before postprocessing): " + 
					allAttributes + "\nGroups: " + allGroups + "\nIdentities: " + 
					Arrays.toString(fullEntity.getIdentities()));

		OutputTranslationProfile translationProfile = profile == null ? 
				profileManagement.getDefaultOutputProfile() :  
				profileManagement.listOutputProfiles().get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
		TranslationInput input = new TranslationInput(allAttributes, fullEntity, group, allGroups, 
				requester, protocol, protocolSubType);
		TranslationResult result = translationProfile.translate(input);
		translationEngine.process(input, result);
		return result;
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
	public static IdentityParam getIdentity(List<IdentityParam> validIdentities, String selectedIdentity) 
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
