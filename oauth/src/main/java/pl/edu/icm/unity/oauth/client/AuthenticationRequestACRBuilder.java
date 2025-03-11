/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.util.ArrayList;
import java.util.List;

import com.nimbusds.openid.connect.sdk.OIDCClaimsRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.ClaimRequirement;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSetRequest;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSetRequest.Entry;

import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.RequestACRsMode;

class AuthenticationRequestACRBuilder
{
	private Builder builder;
	
	public AuthenticationRequestACRBuilder(Builder builder)
	{
		this.builder = builder;
	}

	void addACR(CustomProviderProperties providerCfg,
			RequestedAuthenticationContextClassReference requestedAuthenticationContextClassReference)
	{
		addIDTokenClaimSetRequestIfNeeded(providerCfg, requestedAuthenticationContextClassReference);
		addRequestedVoluntaryACRsIfNeeded(requestedAuthenticationContextClassReference);
	}

	private void addRequestedVoluntaryACRsIfNeeded(
			RequestedAuthenticationContextClassReference acr)
	{
		if (!acr.essentialACRs()
				.isEmpty()
				&& !acr.voluntaryACRs()
						.isEmpty())
		{
			builder.acrValues(acr.voluntaryACRs()
					.stream()
					.map(acr_val -> new ACR(acr_val))
					.toList());
		}
	}

	private void addIDTokenClaimSetRequestIfNeeded(CustomProviderProperties providerCfg,
			RequestedAuthenticationContextClassReference requestedAuthenticationContextClassReference)
	{
		if (providerCfg.getRequestACRMode()
				.equals(RequestACRsMode.NONE))
		{
			return;
		}

		List<Entry> id = new ArrayList<>();
		if (providerCfg.getRequestACRMode()
				.equals(RequestACRsMode.FIXED))
		{
			id.add(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME)
					.withClaimRequirement(
							providerCfg.getBooleanValue(CustomProviderProperties.REQUESTED_ACRS_ARE_ESSENTIAL)
									? ClaimRequirement.ESSENTIAL
									: ClaimRequirement.VOLUNTARY)
					.withValues(providerCfg.getListOfValues(CustomProviderProperties.REQUESTED_ACRS)));
		} else if (providerCfg.getRequestACRMode()
				.equals(RequestACRsMode.FORWARD))
		{
			if (!requestedAuthenticationContextClassReference.essentialACRs()
					.isEmpty())
			{
				id.add(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withClaimRequirement(ClaimRequirement.ESSENTIAL)
						.withValues(requestedAuthenticationContextClassReference.essentialACRs()));
			} else if (!requestedAuthenticationContextClassReference.voluntaryACRs()
					.isEmpty())
			{
				id.add(new Entry(IDTokenClaimsSet.ACR_CLAIM_NAME).withClaimRequirement(ClaimRequirement.VOLUNTARY)
						.withValues(requestedAuthenticationContextClassReference.voluntaryACRs()));
			}
		}

		if (!id.isEmpty())
		{
			ClaimsSetRequest req = new ClaimsSetRequest(id);
			OIDCClaimsRequest oidcClaimsRequest = new OIDCClaimsRequest().withIDTokenClaimsRequest(req);
			builder.claims(oidcClaimsRequest);
		}
	}
}
