/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;

/**
 * Minor helper for remote verificators to properly call {@link RemoteAuthnResultTranslator}
 */
public abstract class AbstractRemoteVerificator extends AbstractVerificator
{
	private RemoteAuthnResultTranslator translator;
	
	public AbstractRemoteVerificator(String name, String description, String exchangeId, 
			RemoteAuthnResultTranslator translator)
	{
		super(name, description, exchangeId);
		this.translator = translator;
	}
	
	protected RemoteAuthenticationResult getResultForNonInteractiveAuthn(RemotelyAuthenticatedInput input, 
			TranslationProfile profile) throws RemoteAuthenticationException
	{
		return getResult(input, profile, false, null, false);
	}

	protected RemoteAuthenticationResult getResult(RemotelyAuthenticatedInput input, TranslationProfile profile,
			boolean sandboxMode, 
			String registrationForm, boolean allowAssociation) throws RemoteAuthenticationException
	{
		return translator.getTranslatedResult(input, profile, 
				sandboxMode, Optional.empty(), registrationForm, allowAssociation, getAuthenticationMethod());
	}
	
	public static TranslationProfile getTranslationProfile(UnityPropertiesHelper props, String globalProfileNameKey,
			String embeddedProfileKey) throws ConfigurationException
	{
		if (props.isSet(embeddedProfileKey))
		{
			return TranslationProfileGenerator.getProfileFromString(props.getValue(embeddedProfileKey));
		} else if (props.getValue(globalProfileNameKey) != null)
		{
			return TranslationProfileGenerator
					.generateIncludeInputProfile(props.getValue(globalProfileNameKey));
		} else
		{
			throw new ConfigurationException("Translation profile is not set");
		}
	}
	
	protected AuthenticationResult addGenericMessageIfError(RemoteAuthenticationResult result, 
			ResolvableError errorMessage)
	{
		if (result.getStatus() == Status.deny && result.asRemote().getErrorResult().error == null)
			return RemoteAuthenticationResult.failed(result.asRemote().getErrorResult().remotePrincipal, errorMessage);
		return result;
	}
		
	public List<IdPInfo> getIdPs()
	{
		return Collections.emptyList();
	}
}
