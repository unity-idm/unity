/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn.remote;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfileRepository;
import pl.edu.icm.unity.store.api.tx.Transactional;

@Component
class RemoteAuthnResultTranslatorImpl implements RemoteAuthnResultTranslator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RemoteAuthnResultTranslatorImpl.class);
	private final InputTranslationProfileRepository inputProfileRepo;
	private final IdentityResolver identityResolver;
	private final InputTranslationEngine trEngine;
	private final InputTranslationActionsRegistry actionsRegistry;
	private final EntityManagement idsMan;
	
	@Autowired
	RemoteAuthnResultTranslatorImpl(IdentityResolver identityResolver,	
			InputTranslationProfileRepository profileRepo,
			InputTranslationEngine trEngine,
			InputTranslationActionsRegistry actionsRegistry,
			@Qualifier("insecure") EntityManagement idsMan)
	{
		this.identityResolver = identityResolver;
		this.inputProfileRepo = profileRepo;
		this.trEngine = trEngine;
		this.actionsRegistry = actionsRegistry;
		this.idsMan = idsMan;
	}

	@Override
	@Transactional
	public RemoteAuthenticationResult getTranslatedResult(RemotelyAuthenticatedInput input, String profile, 
			boolean dryRun, Optional<IdentityTaV> identity, 
			String registrationForm, boolean allowAssociation) 
			throws RemoteAuthenticationException
	{
		TranslationProfile translationProfile;
		try
		{
			 translationProfile = inputProfileRepo.getProfile(profile);

		} catch (EngineException e)
		{
			log.error("Can not get translation profile " + profile , e);
			throw new ConfigurationException("Can not get translation profile " + profile , e);
		}
		
		if (translationProfile == null)
		{
			log.warn("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
			throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
		}
		
		
		return getTranslatedResult(input, translationProfile, dryRun, identity, registrationForm, allowAssociation);
	}
	
	@Override
	@Transactional
	public RemoteAuthenticationResult getTranslatedResult(RemotelyAuthenticatedInput input, TranslationProfile profile, 
			boolean dryRun, Optional<IdentityTaV> identity, 
			String registrationForm, boolean allowAssociation) 
			throws RemoteAuthenticationException
	{
		RemotelyAuthenticatedPrincipal remotePrincipal;
		try
		{
			remotePrincipal = translateRemoteInput(input, profile, dryRun, identity);
		} catch (EngineException e)
		{
			log.warn("The mapping of the remotely authenticated principal to a local representation failed", e);
			throw new RemoteAuthenticationException("The mapping of the remotely authenticated " +
					"principal to a local representation failed", e);
		}
		return dryRun ? assembleDryRunAuthenticationResult(remotePrincipal, registrationForm, allowAssociation) : 
			assembleAuthenticationResult(remotePrincipal, registrationForm, allowAssociation);
	}

	private RemoteAuthenticationResult assembleDryRunAuthenticationResult(RemotelyAuthenticatedPrincipal remotePrincipal,
			String registrationForm, boolean allowAssociation)
	{
		AuthenticatedEntity authenticatedEntity = null;
		if (remotePrincipal.getLocalMappedPrincipal() != null)
		{
			try
			{
				authenticatedEntity = resolveAuthenticatedEntity(remotePrincipal);
			} catch (RemoteAuthenticationException | EngineException e)
			{
				log.debug("Exception resolving remote principal", e);
			}
		} else
		{
			return handleUnknownUser(remotePrincipal, registrationForm, allowAssociation);
		}
		return RemoteAuthenticationResult.successfulPartial(remotePrincipal, authenticatedEntity);
	}
	
	/**
	 * Tries to resolve the primary identity from the previously created {@link RemotelyAuthenticatedPrincipal}
	 * (usually via {@link #processRemoteInput(RemotelyAuthenticatedInput)}) and returns a 
	 * final {@link AuthenticationResult} depending on the success of this action.
	 */
	@Override
	public RemoteAuthenticationResult assembleAuthenticationResult(RemotelyAuthenticatedPrincipal remoteContext, 
			String registrationForm, boolean allowAssociation) throws RemoteAuthenticationException
	{
		if (remoteContext.getIdentities().isEmpty())
			throw new RemoteAuthenticationException("The remotely authenticated principal " +
					"was not mapped to a local representation.");
		if (remoteContext.getLocalMappedPrincipal() == null)
			return handleUnknownUser(remoteContext, registrationForm, allowAssociation);
		try
		{
			AuthenticatedEntity authenticatedEntity = resolveAuthenticatedEntity(remoteContext);
			return RemoteAuthenticationResult.successful(remoteContext, authenticatedEntity);
		} catch (IllegalIdentityValueException ie)
		{
			return handleUnknownUser(remoteContext, registrationForm, allowAssociation);
		} catch (EngineException e)
		{
			throw new RemoteAuthenticationException("Problem occured when searching for the " +
					"mapped, remotely authenticated identity in the local user store", e);
		}
	}

	private AuthenticatedEntity resolveAuthenticatedEntity(RemotelyAuthenticatedPrincipal remoteContext)
			throws EngineException, RemoteAuthenticationException
	{
		EntityParam mappedEntity = remoteContext.getLocalMappedPrincipal();
		long resolved = mappedEntity.getEntityId() != null ? 
				mappedEntity.getEntityId() : 
				identityResolver.resolveIdentity(mappedEntity.getIdentity().getValue(), 
				new String[] {mappedEntity.getIdentity().getTypeId()}, 
				null, null);
		
		if (!identityResolver.isEntityEnabled(resolved))
			throw new RemoteAuthenticationException("The remotely authenticated principal "
					+ "was mapped to a disabled account");
		
		AuthenticatedEntity authenticatedEntity = new AuthenticatedEntity(resolved, 
				remoteContext.getMappingResult().getAuthenticatedWith(), null);
		authenticatedEntity.setRemoteIdP(remoteContext.getRemoteIdPName());
		return authenticatedEntity;
	}
	
	private RemoteAuthenticationResult handleUnknownUser(RemotelyAuthenticatedPrincipal remotePrincipal, String registrationForm, 
			boolean allowAssociation)
	{
		return RemoteAuthenticationResult.unknownRemotePrincipal(remotePrincipal, registrationForm, allowAssociation);
	}
	
	/**
	 * Invokes the configured translation profile on the remotely obtained authentication input. Then assembles  
	 * the {@link RemotelyAuthenticatedPrincipal} from the processed input containing the information about what 
	 * from the remote data is or can be meaningful in the local DB.
	 */
	@Override
	public final RemotelyAuthenticatedPrincipal translateRemoteInput(RemotelyAuthenticatedInput input, 
			TranslationProfile translationProfile, boolean dryRun, Optional<IdentityTaV> identity) throws EngineException
	{
		
		if (translationProfile == null)
		{
			log.warn("The translation profile can not be empty");
			throw new ConfigurationException("The translation profile can not be empty");
		}
		
		InputTranslationProfile profileInstance = new InputTranslationProfile(
				translationProfile, inputProfileRepo, actionsRegistry);
		
		MappingResult result = profileInstance.translate(input);
		log.info("Result of remote data mapping:\n{}", result);
		if (identity.isPresent())
		{
			IdentityTaV presetIdentity = identity.get();
			IdentityParam presetIdParam = new IdentityParam(presetIdentity.getTypeId(), 
					presetIdentity.getValue());
			log.info("Adding a preset identity as a required to results of mapping: {}", presetIdentity);
			result.addIdentity(new MappedIdentity(IdentityEffectMode.REQUIRE_MATCH, 
					presetIdParam, null));
		}
		setMappingToExistingEntity(result);
		if (!dryRun)
			trEngine.process(result);
		
		RemotelyAuthenticatedPrincipal ret = new RemotelyAuthenticatedPrincipal(input.getIdpName(), translationProfile.getName());
		ret.addAttributes(extractAttributes(result));
		ret.addIdentities(extractIdentities(result));
		ret.addGroups(extractGroups(result));
		ret.setLocalMappedPrincipal(result.getMappedAtExistingEntity());
		ret.setMappingResult(result);
		ret.setAuthnInput(input);
		ret.setSessionParticipants(input.getSessionParticipants());
		ret.setCreationTime(Instant.now());
		return ret;
	}
	
	private void setMappingToExistingEntity(MappingResult result) throws EngineException
	{
		Entity existing = null;
		for (MappedIdentity checked : result.getIdentities())
		{
			try
			{
				Entity found = idsMan.getEntity(new EntityParam(checked.getIdentity()));
				if (existing != null && !existing.getId().equals(found.getId()))
				{
					log.warn("Identity was mapped to two different entities: " + existing + " and "
							+ found);
					throw new ExecutionBreakException();
				}
				existing = found;
				result.addAuthenticatedWith(checked.getIdentity().getValue());
			} catch (IllegalArgumentException e)
			{
				log.trace("Identity " + checked + " not found in DB, details of exception follows", e);
			}
		}
		if (existing != null)
		{
			result.setMappedToExistingEntity(new EntityParam(existing.getId()));
		}
	}
	
	private List<IdentityTaV> extractIdentities(MappingResult input)
	{
		List<MappedIdentity> identities = input.getIdentities();
		List<IdentityTaV> ret = new ArrayList<>();
		if (identities == null)
			return ret;
		for (MappedIdentity ri: identities)
			ret.add(ri.getIdentity());
		return ret;
	}

	private Set<String> extractGroups(MappingResult input)
	{
		List<MappedGroup> groups = input.getGroups();
		Set<String> ret = new HashSet<>();
		if (groups == null)
			return ret;
		for (MappedGroup rg: groups)
			ret.add(rg.getGroup());
		return ret;
	}
	
	private static List<Attribute> extractAttributes(MappingResult input) throws EngineException
	{
		List<MappedAttribute> attributes = input.getAttributes();
		List<Attribute> ret = new ArrayList<>();
		for (MappedAttribute ra: attributes)
			ret.add(ra.getAttribute());
		return ret;
	}
}
