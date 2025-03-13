/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.ClaimRequirement;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSetRequest.Entry;

import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.RequestACRsMode;

@ExtendWith(MockitoExtension.class)
public class AuthenticationRequestACRBuilderTest
{

	@Test
	public void shouldForwardAllACRs() throws URISyntaxException
	{
		Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				Scope.parse("openid"), new ClientID("clientId"), new URI("uri"));
		CustomProviderProperties prop = mock(CustomProviderProperties.class);

		when(prop.getRequestACRMode()).thenReturn(RequestACRsMode.FORWARD);

		new AuthenticationRequestACRBuilder(builder).addACR(prop,
				new RequestedAuthenticationContextClassReference(List.of("es1"), List.of("voluntary1")));

		AuthenticationRequest authenticationRequest = builder.build();

		assertThat(authenticationRequest.getACRValues()).isEqualTo(List.of(new ACR("voluntary1")));

		assertThat(authenticationRequest.getOIDCClaims()
				.getIDTokenClaimsRequest()
				.get("acr")
				.toJSONObjectEntry()).isEqualTo(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withValues(List.of("es1"))
						.withClaimRequirement(ClaimRequirement.ESSENTIAL)
						.toJSONObjectEntry());

	}

	@Test
	public void shouldNotForwardACRsWhenNoneisSet() throws URISyntaxException
	{
		Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				Scope.parse("openid"), new ClientID("clientId"), new URI("uri"));
		CustomProviderProperties prop = mock(CustomProviderProperties.class);

		when(prop.getRequestACRMode()).thenReturn(RequestACRsMode.NONE);

		new AuthenticationRequestACRBuilder(builder).addACR(prop,
				new RequestedAuthenticationContextClassReference(List.of("es1"), List.of("voluntary1")));

		AuthenticationRequest authenticationRequest = builder.build();

		assertThat(authenticationRequest.getACRValues()).isNull();
		assertThat(authenticationRequest.getOIDCClaims()).isNull();
	}
	
	@Test
	public void shouldForwardOnlyEssentialACR() throws URISyntaxException
	{
		Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				Scope.parse("openid"), new ClientID("clientId"), new URI("uri"));
		CustomProviderProperties prop = mock(CustomProviderProperties.class);

		when(prop.getRequestACRMode()).thenReturn(RequestACRsMode.FORWARD);

		new AuthenticationRequestACRBuilder(builder).addACR(prop,
				new RequestedAuthenticationContextClassReference(List.of("es1"), List.of()));

		AuthenticationRequest authenticationRequest = builder.build();

		assertThat(authenticationRequest.getOIDCClaims()
				.getIDTokenClaimsRequest()
				.get("acr")
				.toJSONObjectEntry()).isEqualTo(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withValues(List.of("es1"))
						.withClaimRequirement(ClaimRequirement.ESSENTIAL)
						.toJSONObjectEntry());

	}

	@Test
	public void shouldForwardOnlyVoluntaryACR() throws URISyntaxException
	{
		Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				Scope.parse("openid"), new ClientID("clientId"), new URI("uri"));
		CustomProviderProperties prop = mock(CustomProviderProperties.class);

		when(prop.getRequestACRMode()).thenReturn(RequestACRsMode.FORWARD);

		new AuthenticationRequestACRBuilder(builder).addACR(prop,
				new RequestedAuthenticationContextClassReference(List.of(), List.of("voluntary1")));

		AuthenticationRequest authenticationRequest = builder.build();

		assertThat(authenticationRequest.getOIDCClaims()
				.getIDTokenClaimsRequest()
				.get("acr")
				.toJSONObjectEntry())
						.isEqualTo(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withValues(List.of("voluntary1"))
								.withClaimRequirement(ClaimRequirement.VOLUNTARY)
								.toJSONObjectEntry());
	}

	@Test
	public void shouldAddFixedEssentialACR() throws URISyntaxException
	{
		Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				Scope.parse("openid"), new ClientID("clientId"), new URI("uri"));
		CustomProviderProperties prop = mock(CustomProviderProperties.class);

		when(prop.getRequestACRMode()).thenReturn(RequestACRsMode.FIXED);
		when(prop.getBooleanValue(CustomProviderProperties.REQUESTED_ACRS_ARE_ESSENTIAL)).thenReturn(true);
		when(prop.getListOfValues(CustomProviderProperties.REQUESTED_ACRS)).thenReturn(List.of("acr1"));

		new AuthenticationRequestACRBuilder(builder).addACR(prop,
				new RequestedAuthenticationContextClassReference(List.of(), List.of()));
		AuthenticationRequest authenticationRequest = builder.build();

		assertThat(authenticationRequest.getOIDCClaims()
				.getIDTokenClaimsRequest()
				.get("acr")
				.toJSONObjectEntry()).isEqualTo(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withValues(List.of("acr1"))
						.withClaimRequirement(ClaimRequirement.ESSENTIAL)
						.toJSONObjectEntry());

	}

	@Test
	public void shouldAddFixedVoluntaryACR() throws URISyntaxException
	{
		Builder builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				Scope.parse("openid"), new ClientID("clientId"), new URI("uri"));
		CustomProviderProperties prop = mock(CustomProviderProperties.class);

		when(prop.getRequestACRMode()).thenReturn(RequestACRsMode.FIXED);
		when(prop.getBooleanValue(CustomProviderProperties.REQUESTED_ACRS_ARE_ESSENTIAL)).thenReturn(false);
		when(prop.getListOfValues(CustomProviderProperties.REQUESTED_ACRS)).thenReturn(List.of("acr1"));

		new AuthenticationRequestACRBuilder(builder).addACR(prop,
				new RequestedAuthenticationContextClassReference(List.of(), List.of()));

		AuthenticationRequest authenticationRequest = builder.build();

		assertThat(authenticationRequest.getOIDCClaims()
				.getIDTokenClaimsRequest()
				.get("acr")
				.toJSONObjectEntry()).isEqualTo(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withValues(List.of("acr1"))
						.withClaimRequirement(ClaimRequirement.VOLUNTARY)
						.toJSONObjectEntry());
	}
}
