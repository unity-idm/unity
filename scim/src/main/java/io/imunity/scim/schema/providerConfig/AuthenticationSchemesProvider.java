/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.providerConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.schema.providerConfig.SCIMProviderConfigResource.AuthenticationSchema;
import io.imunity.scim.schema.providerConfig.SCIMProviderConfigResource.AuthenticationSchema.Type;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;

@Component
class AuthenticationSchemesProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, AuthenticationSchemesProvider.class);

	
	private static final AuthenticationSchema HTTP_BASIC_SCHEMA = AuthenticationSchema.builder().withName("HTTP Basic")
			.withType(Type.httpbasic).withDescription("Authentication scheme using the HTTP Basic Standard").build();

	private static final AuthenticationSchema BEARER_TOKEN_SCHEMA = AuthenticationSchema.builder()
			.withName("OAuth Bearer Token").withType(Type.oauthbearertoken)
			.withDescription("Authentication scheme using the OAuth Bearer Token Standard").build();

	private static final Map<String, AuthenticationSchema> AuthenticationSchemaTypeMap = Map.of(
			BearerTokenVerificator.NAME, BEARER_TOKEN_SCHEMA, PasswordVerificator.NAME, HTTP_BASIC_SCHEMA,
			"composite-password", HTTP_BASIC_SCHEMA, "ldap", HTTP_BASIC_SCHEMA, "pam", HTTP_BASIC_SCHEMA

	);

	private final AuthenticatorManagement authenticatorManagement;
	private final AuthenticationFlowManagement authenticationFlowManagement;

	@Autowired
	public AuthenticationSchemesProvider(AuthenticatorManagement authenticatorManagement,
			AuthenticationFlowManagement authenticationFlowManagement)
	{

		this.authenticatorManagement = authenticatorManagement;
		this.authenticationFlowManagement = authenticationFlowManagement;
	}

	Set<AuthenticationSchema> getAuthenticationSchemes(List<String> authnOptions)
	{
		Set<AuthenticationSchema> ret = new HashSet<>();
		Map<String, AuthenticatorInfo> authenticators = new HashMap<>();
		try
		{
			authenticators.putAll(authenticatorManagement.getAuthenticators(JAXRSAuthentication.NAME).stream()
					.collect(Collectors.toMap(a -> a.getId(), a -> a)));
		} catch (EngineException e)
		{
			log.error("Can not get authenticators", e);
		}

		Map<String, AuthenticationFlowDefinition> flows = new HashMap<>();
		try
		{
			flows.putAll(authenticationFlowManagement.getAuthenticationFlows().stream()
					.collect(Collectors.toMap(a -> a.getName(), a -> a)));

		} catch (EngineException e)
		{
			log.error("Can not get authentication flows", e);
		}

		for (String authnOption : authnOptions)
		{
			AuthenticatorInfo authenticatorInfo = authenticators.get(authnOption);
			if (authenticatorInfo != null)
			{
				mapAuthenticator(authenticatorInfo).ifPresent(ret::add);
			} else if (flows.get(authnOption) != null)
			{
				AuthenticationFlowDefinition flow = flows.get(authnOption);
				for (String authnenticator : flow.getFirstFactorAuthenticators())
				{

					mapAuthenticator(authenticators.get(authnenticator)).ifPresent(ret::add);
				}
			}

		}
		return ret;
	}

	Optional<AuthenticationSchema> mapAuthenticator(AuthenticatorInfo authenticator)
	{
		return Optional.ofNullable(
				AuthenticationSchemaTypeMap.get(authenticator.getTypeDescription().getVerificationMethod()));
	}

}
