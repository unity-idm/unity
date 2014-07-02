/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.translation.MappedAttribute;
import pl.edu.icm.unity.server.authn.remote.translation.MappedIdentity;
import pl.edu.icm.unity.server.authn.remote.translation.MappingResult;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityTaV;

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
	private TranslationEngine trEngine;
	
	public RemoteVerificatorUtil(IdentityResolver identityResolver,	TranslationProfileManagement profileManagement,
			TranslationEngine trEngine)
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
	public AuthenticationResult getResult(RemotelyAuthenticatedInput input, String profile) 
			throws AuthenticationException
	{
		RemotelyAuthenticatedContext context;
		try
		{
			context = processRemoteInput(input, profile);
		} catch (EngineException e)
		{
			throw new AuthenticationException("The mapping of the remtely authenticated " +
					"principal to a local representation failed", e);
		}
		return assembleAuthenticationResult(context);
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
		IdentityTaV remoteIdentityMapped = remoteContext.getPrimaryIdentity();
		if (remoteIdentityMapped == null)
			throw new AuthenticationException("The remotely authenticated principal " +
					"was not mapped to a local representation.");
		
		try
		{
			long resolved = identityResolver.resolveIdentity(remoteIdentityMapped.getValue(), 
					new String[] {remoteIdentityMapped.getTypeId()});
			AuthenticatedEntity authenticatedEntity = new AuthenticatedEntity(resolved, 
					remoteIdentityMapped.getValue(), false);
			return new AuthenticationResult(Status.success, remoteContext, authenticatedEntity);
		} catch (IllegalIdentityValueException ie)
		{
			AuthenticationResult r = new AuthenticationResult(Status.unknownRemotePrincipal, 
					remoteContext, null);
			throw new AuthenticationException(r, "The mapped identity is not present in the local " +
					"user store.");
		} catch (EngineException e)
		{
			throw new AuthenticationException("Problem occured when searching for the " +
					"mapped, remotely authenticated identity in the local user store", e);
		}
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
			String profile)	throws EngineException
	{
		TranslationProfile translationProfile = profileManagement.listProfiles().get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
		MappingResult result = translationProfile.translate(input);

		trEngine.process(result);
		
		RemotelyAuthenticatedContext ret = new RemotelyAuthenticatedContext(input.getIdpName());
		ret.addAttributes(extractAttributes(result));
		ret.addIdentities(extractIdentities(result));
		ret.addGroups(result.getGroups());
		ret.setPrimaryIdentity(extractPrimaryIdentity(result));
		return ret;
	}
	
	private IdentityTaV extractPrimaryIdentity(MappingResult input)
	{
		return input.getIdentities().get(0).getIdentity();
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
	
	public static List<Attribute<?>> extractAttributes(MappingResult input) throws EngineException
	{
		List<MappedAttribute> attributes = input.getAttributes();
		List<Attribute<?>> ret = new ArrayList<>();
		for (MappedAttribute ra: attributes)
			ret.add(ra.getAttribute());
		return ret;
	}
}
