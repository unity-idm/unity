/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.action.CreateAttributeActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * IdP engine is responsible for performing common IdP-related functionality. It resolves the information
 * about the user being queried, applies translation profile on the data and exposes it to the endpoint 
 * requiring the data.
 * 
 * @author K. Benedyczak
 */
@Component
public class IdPEngineImpl implements IdPEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdPEngineImpl.class);

	private AttributesManagement attributesMan;
	private EntityManagement identitiesMan;
	private OutputTranslationEngine translationEngine;
	private TranslationProfileManagement profileManagement;
	private UserImportSerivce userImportService;
	private OutputTranslationActionsRegistry actionsRegistry;
	
	private OutputTranslationProfile defaultProfile;
	
	@Autowired
	public IdPEngineImpl(AttributesManagement attributesMan, EntityManagement identitiesMan,
			@Qualifier("insecure") OutputTranslationEngine translationEngine,
			TranslationProfileManagement profileManagement,
			UserImportSerivce userImportService,
			OutputTranslationActionsRegistry actionsRegistry)
	{
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.translationEngine = translationEngine;
		this.profileManagement = profileManagement;
		this.userImportService = userImportService;
		this.actionsRegistry = actionsRegistry;

		this.defaultProfile = createDefaultOutputProfile();
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
	@Override
	public TranslationResult obtainUserInformation(EntityParam entity, String group, String profile,
			String requester, String protocol, String protocolSubType, boolean allowIdentityCreate,
			boolean triggerImport) 
			throws EngineException
	{
		IdentityTaV identityTaV = entity.getIdentity();
		if (identityTaV != null && triggerImport)
			userImportService.importUser(identityTaV.getValue(), identityTaV.getTypeId());
		Collection<String> allGroups = identitiesMan.getGroups(entity).keySet();
		Collection<AttributeExt> allAttributes = attributesMan.getAttributes(
				entity, group, null);
		Entity fullEntity = identitiesMan.getEntity(entity, requester, allowIdentityCreate, group);
		if (log.isTraceEnabled())
			log.trace("Attributes to be returned (before postprocessing): " + 
					allAttributes + "\nGroups: " + allGroups + "\nIdentities: " + 
					fullEntity.getIdentities());

		OutputTranslationProfile profileInstance;
		if (profile != null)
		{
			TranslationProfile translationProfile = profileManagement.listOutputProfiles().get(profile);
			if (translationProfile == null)
				throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
			profileInstance = new OutputTranslationProfile(translationProfile, actionsRegistry);
		} else
		{
			profileInstance = defaultProfile;
		}
		TranslationInput input = new TranslationInput(allAttributes, fullEntity, group, allGroups, 
				requester, protocol, protocolSubType);
		TranslationResult result = profileInstance.translate(input);
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
	
	private OutputTranslationProfile createDefaultOutputProfile()
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateAttributeActionFactory.NAME, 
				new String[] {"memberOf", "groups"});
		rules.add(new TranslationRule("true", action1));
		TranslationProfile profile = new TranslationProfile("DEFAULT OUTPUT PROFILE", "", ProfileType.OUTPUT,
				rules);
		return new OutputTranslationProfile(profile, actionsRegistry);
	}
}
