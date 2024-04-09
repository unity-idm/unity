/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;

@Component
class OAuthRemoteAuthenticationInputAssembler
{
	private static final String ISSUER = "iss";
	private static final String ACR_CLAIM = "acr";

	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthRemoteAuthenticationInputAssembler.class);

	private final OAuthDiscoveryMetadataCache metadataManager;

	OAuthRemoteAuthenticationInputAssembler(OAuthDiscoveryMetadataCache metadataManager)
	{
		this.metadataManager = metadataManager;
	}

	RemotelyAuthenticatedInput convertInput(CustomProviderProperties provCfg, OAuthContext context,
			AttributeFetchResult attributes, boolean openIdConnectMode)
	{
		String tokenEndpoint = provCfg.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
		String discoveryEndpoint = provCfg.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		if (tokenEndpoint == null && discoveryEndpoint != null)
		{
			try
			{
				OIDCProviderMetadata providerMeta = metadataManager.getMetadata(provCfg.generateMetadataRequest());
				tokenEndpoint = providerMeta.getTokenEndpointURI()
						.toString();
			} catch (Exception e)
			{
				log.warn("Can't obtain OIDC metadata", e);
			}
		}
		if (tokenEndpoint == null)
			tokenEndpoint = "unknown";

		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(tokenEndpoint);
		for (Map.Entry<String, List<String>> attr : attributes.getAttributes()
				.entrySet())
		{
			input.addAttribute(new RemoteAttribute(attr.getKey(), attr.getValue()
					.toArray()));
			if (attr.getKey()
					.equals("sub")
					&& !attr.getValue()
							.isEmpty())
				input.addIdentity(new RemoteIdentity(attr.getValue()
						.get(0), "sub"));
		}
		input.setRawAttributes(attributes.getRawAttributes());
		input.setRemoteAuthnMetadata(getAuthnMeta(attributes, openIdConnectMode));
		return input;
	}

	private RemoteAuthnMetadata getAuthnMeta(AttributeFetchResult attributes, boolean openIdConnectMode)
	{
		return new RemoteAuthnMetadata(openIdConnectMode ? Protocol.OIDC : Protocol.OTHER,
				openIdConnectMode ? attributes.getAttributes()
						.get(ISSUER)
						.get(0) : RemoteAuthnMetadata.UNDEFINED_IDP,
				getAcr(attributes));
	}

	private List<String> getAcr(AttributeFetchResult attributes)
	{
		return attributes.getAttributes()
				.get(ACR_CLAIM) != null ? List.of(
						attributes.getAttributes()
								.get(ACR_CLAIM)
								.get(0))
						: List.of();
	}
}
