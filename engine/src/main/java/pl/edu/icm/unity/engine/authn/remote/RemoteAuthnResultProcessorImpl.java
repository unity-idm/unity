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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfileRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implements {@link RemoteAuthnResultProcessor} 
 * @author K. Benedyczak
 */
@Component
public class RemoteAuthnResultProcessorImpl implements RemoteAuthnResultProcessor
{
	private InputTranslationProfileRepository inputProfileRepo;
	private IdentityResolver identityResolver;
	private InputTranslationEngine trEngine;
	private InputTranslationActionsRegistry actionsRegistry;
	
	
	@Autowired
	public RemoteAuthnResultProcessorImpl(IdentityResolver identityResolver,	
			InputTranslationProfileRepository profileRepo,
			InputTranslationEngine trEngine,
			InputTranslationActionsRegistry actionsRegistry)
	{
		this.identityResolver = identityResolver;
		this.inputProfileRepo = profileRepo;
		this.trEngine = trEngine;
		this.actionsRegistry = actionsRegistry;
	}

	/**
	 * This method is calling {@link #processRemoteInput(RemotelyAuthenticatedInput)} and then
	 * {@link #assembleAuthenticationResult(RemotelyAuthenticatedContext)}.
	 * Usually it is the only one that is used, when {@link RemotelyAuthenticatedInput} 
	 * is obtained in an implementation specific way.
	 * 
	 * @param input
	 * @return
	 * @throws EngineException 
	 */
	@Override
	@Transactional
	public AuthenticationResult getResult(RemotelyAuthenticatedInput input, String profile, 
			boolean dryRun, Optional<IdentityTaV> identity) 
			throws AuthenticationException
	{
		RemotelyAuthenticatedContext context;
		try
		{
			context = processRemoteInput(input, profile, dryRun, identity);
		} catch (EngineException e)
		{
			throw new AuthenticationException("The mapping of the remotely authenticated " +
					"principal to a local representation failed", e);
		}
		return dryRun ? new AuthenticationResult(Status.success, context, null) : 
			assembleAuthenticationResult(context);
	}
	
	/**
	 * Tries to resolve the primary identity from the previously created {@link RemotelyAuthenticatedContext}
	 * (usually via {@link #processRemoteInput(RemotelyAuthenticatedInput)}) and returns a 
	 * final {@link AuthenticationResult} depending on the success of this action.
	 * 
	 * @param remoteContext
	 * @return
	 * @throws EngineException 
	 */
	@Override
	public AuthenticationResult assembleAuthenticationResult(RemotelyAuthenticatedContext remoteContext) 
			throws AuthenticationException
	{
		if (remoteContext.getIdentities().isEmpty())
			throw new AuthenticationException("The remotely authenticated principal " +
					"was not mapped to a local representation.");
		if (remoteContext.getLocalMappedPrincipal() == null)
			handleUnknownUser(remoteContext);
		try
		{
			EntityParam mappedEntity = remoteContext.getLocalMappedPrincipal();
			long resolved = mappedEntity.getEntityId() != null ? 
					mappedEntity.getEntityId() : 
					identityResolver.resolveIdentity(mappedEntity.getIdentity().getValue(), 
					new String[] {mappedEntity.getIdentity().getTypeId()}, 
					null, null);
			
			if (!identityResolver.isEntityEnabled(resolved))
				throw new AuthenticationException("The remotely authenticated principal "
						+ "was mapped to a disabled account");
			
			AuthenticatedEntity authenticatedEntity = new AuthenticatedEntity(resolved, 
					remoteContext.getMappingResult().getAuthenticatedWith(), null);
			authenticatedEntity.setRemoteIdP(remoteContext.getRemoteIdPName());
			return new AuthenticationResult(Status.success, remoteContext, authenticatedEntity);
		} catch (IllegalIdentityValueException ie)
		{
			handleUnknownUser(remoteContext);
			return null; //dummy - above line always throws exception
		} catch (EngineException e)
		{
			throw new AuthenticationException("Problem occured when searching for the " +
					"mapped, remotely authenticated identity in the local user store", e);
		}
	}
	
	private void handleUnknownUser(RemotelyAuthenticatedContext remoteContext) throws AuthenticationException
	{
		AuthenticationResult r = new AuthenticationResult(Status.unknownRemotePrincipal, 
				remoteContext, null);
		throw new AuthenticationException(r, "The mapped identity is not present in the local " +
				"user store.");
	}
	
	/**
	 * Invokes the configured translation profile on the remotely obtained authentication input. Then assembles  
	 * the {@link RemotelyAuthenticatedContext} from the processed input containing the information about what 
	 * from the remote data is or can be meaningful in the local DB.
	 * 
	 * @param input
	 * @return
	 * @throws EngineException
	 */
	@Override
	public final RemotelyAuthenticatedContext processRemoteInput(RemotelyAuthenticatedInput input, 
			String profile, boolean dryRun, Optional<IdentityTaV> identity) throws EngineException
	{
		TranslationProfile translationProfile = inputProfileRepo.getProfile(profile);
		InputTranslationProfile profileInstance = new InputTranslationProfile(
				translationProfile, inputProfileRepo, actionsRegistry);
		if (translationProfile == null)
			throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
		MappingResult result = profileInstance.translate(input);
		
		if (identity.isPresent())
		{
			IdentityTaV presetIdentity = identity.get();
			IdentityParam presetIdParam = new IdentityParam(presetIdentity.getTypeId(), 
					presetIdentity.getValue());
			result.addIdentity(new MappedIdentity(IdentityEffectMode.REQUIRE_MATCH, 
					presetIdParam, null));
		}
		if (!dryRun)
			trEngine.process(result);
		
		RemotelyAuthenticatedContext ret = new RemotelyAuthenticatedContext(input.getIdpName(), profile);
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
