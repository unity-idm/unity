/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Defines comunication of a OAuth verificator and retrieval.
 * @author K. Benedyczak
 */
public interface OAuthExchange extends CredentialExchange
{
	public static final String ID = "OAuth2 exchange";
	
	OAuthClientProperties getSettings();
	
	OAuthContext createRequest(String providerKey, Optional<ExpectedIdentity> expectedIdentity, 
			AuthenticationStepContext authnStepContext, 
			LoginMachineDetails initialLoginMachine, 
			String ultimateReturnURL,
			AuthenticationTriggeringContext authnTriggeringContext) 
			throws URISyntaxException, SerializeException, ParseException, IOException;
}
