/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.action.CreateAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.FilterAttributeActionFactory;
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
public class IdPEngineImplBase implements IdPEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdPEngineImplBase.class);

	private AttributesManagement attributesMan;
	private EntityManagement identitiesMan;
	private OutputTranslationEngine translationEngine;
	private OutputTranslationProfileRepository outputProfileRepo;
	private UserImportSerivce userImportService;
	private OutputTranslationActionsRegistry actionsRegistry;
	private UnityMessageSource msg;
	private AttributeValueConverter attrValueConverter; 
	
	private OutputTranslationProfile defaultProfile;

	
	public IdPEngineImplBase(AttributesManagement attributesMan, 
			EntityManagement identitiesMan,
			OutputTranslationProfileRepository outputProfileRepo,
			OutputTranslationEngine translationEngine,
			UserImportSerivce userImportService,
			OutputTranslationActionsRegistry actionsRegistry,
			AttributeValueConverter attrValueConverter,
			UnityMessageSource msg)
	{
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.translationEngine = translationEngine;
		this.outputProfileRepo = outputProfileRepo;
		this.userImportService = userImportService;
		this.actionsRegistry = actionsRegistry;
		this.attrValueConverter = attrValueConverter;
		this.msg = msg;

		this.defaultProfile = createDefaultOutputProfile();
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
		userImportService.importToExistingUser(userImports, fullEntity.getIdentities().get(0));
		
		return obtainUserInformationPostImport(entity, fullEntity, group, profile, 
				requester, protocol, protocolSubType);
	}
	
	@Override
	public TranslationResult obtainUserInformationWithEarlyImport(IdentityTaV identity, String group, String profile,
			String requester, String protocol, String protocolSubType, boolean allowIdentityCreate,
			PropertiesHelper config)
			throws EngineException
	{
		List<UserImportSpec> userImports = CommonIdPProperties.getUserImportsLegacy(
				config, identity.getValue(), identity.getTypeId());
		userImportService.importUser(userImports);
		EntityParam entity = new EntityParam(identity);
		Entity fullEntity = identitiesMan.getEntity(entity, requester, allowIdentityCreate, group);
		
		return obtainUserInformationPostImport(entity, fullEntity, group, profile, 
				requester, protocol, protocolSubType);
	}
	
	private TranslationResult obtainUserInformationPostImport(EntityParam entity, Entity fullEntity,
			String group, String profile,
			String requester, String protocol, String protocolSubType) throws EngineException
	{
		Collection<String> allGroups = identitiesMan.getGroups(entity).keySet();
		Collection<AttributeExt> allAttributes = attributesMan.getAttributes(
				entity, group, null);
		if (log.isTraceEnabled())
			log.trace("Attributes to be returned (before postprocessing): " + 
					allAttributes + "\nGroups: " + allGroups + "\nIdentities: " + 
					fullEntity.getIdentities());

		OutputTranslationProfile profileInstance;
		if (profile != null)
		{
			TranslationProfile translationProfile = outputProfileRepo.listAllProfiles().get(profile);
			if (translationProfile == null)
				throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
			profileInstance = new OutputTranslationProfile(translationProfile, outputProfileRepo, 
					actionsRegistry, attrValueConverter);
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
				new String[] {"memberOf", "groups", "false", 
						msg.getMessage("DefaultOutputTranslationProfile.attr.memberOf"), 
						msg.getMessage("DefaultOutputTranslationProfile.attr.memberOfDesc")});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(FilterAttributeActionFactory.NAME,
				"sys:.*");
		rules.add(new TranslationRule("true", action2));
		TranslationProfile profile = new TranslationProfile("DEFAULT OUTPUT PROFILE", "", ProfileType.OUTPUT,
				rules);
		return new OutputTranslationProfile(profile, outputProfileRepo, actionsRegistry, attrValueConverter);
	}
}
