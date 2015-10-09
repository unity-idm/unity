/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.MappedAttribute;
import pl.edu.icm.unity.server.translation.in.MappedGroup;
import pl.edu.icm.unity.server.translation.in.MappedIdentity;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Processes {@link RemotelyAuthenticatedInput} by applying a translation profile to it and 
 * returns {@link RemotelyAuthenticatedContext} or {@link AuthenticationResult} depending whether 
 * caller wants to have a possibility to postprocess the translation profile output or not.
 * 
 * @author K. Benedyczak
 */
public class RemoteVerificatorUtil
{
	private TranslationProfileManagement profileManagement;
	private IdentityResolver identityResolver;
	private InputTranslationEngine trEngine;
	
	public RemoteVerificatorUtil(IdentityResolver identityResolver,	TranslationProfileManagement profileManagement,
			InputTranslationEngine trEngine)
	{
		this.identityResolver = identityResolver;
		this.profileManagement = profileManagement;
		this.trEngine = trEngine;
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
	public AuthenticationResult getResult(RemotelyAuthenticatedInput input, String profile, boolean dryRun) 
			throws AuthenticationException
	{
		RemotelyAuthenticatedContext context;
		try
		{
			context = processRemoteInput(input, profile, dryRun);
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
			AuthenticatedEntity authenticatedEntity = new AuthenticatedEntity(resolved, 
					remoteContext.getMappingResult().getAuthenticatedWith(), false);
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
	public final RemotelyAuthenticatedContext processRemoteInput(RemotelyAuthenticatedInput input, 
			String profile, boolean dryRun) throws EngineException
	{
		InputTranslationProfile translationProfile = profileManagement.listInputProfiles().get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
		MappingResult result = translationProfile.translate(input);

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
	
	public static List<Attribute<?>> extractAttributes(MappingResult input) throws EngineException
	{
		List<MappedAttribute> attributes = input.getAttributes();
		List<Attribute<?>> ret = new ArrayList<>();
		for (MappedAttribute ra: attributes)
			ret.add(ra.getAttribute());
		return ret;
	}
}
