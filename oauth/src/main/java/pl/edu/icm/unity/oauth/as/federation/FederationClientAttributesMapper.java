/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

public class FederationClientAttributesMapper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, FederationClientAttributesMapper.class);
	public static List<Attribute> toOAuthAttributes(OIDCClientMetadata meta, String oauthGroup)
	{
		return toOAuthAttributes(meta, oauthGroup, null);
	}

	public static List<Attribute> toOAuthAttributes(OIDCClientMetadata meta, String oauthGroup, String clientId)
	{
		List<Attribute> attrs = new ArrayList<>();

		List<String> redirectUris = meta.getRedirectionURIs() != null
				? meta.getRedirectionURIs().stream().map(Object::toString).toList()
				: List.of();
		if (!redirectUris.isEmpty())
			attrs.add(StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI, oauthGroup, redirectUris));

		if (meta.getScope() != null && !meta.getScope().isEmpty())
			attrs.add(StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_SCOPES, oauthGroup,
					meta.getScope().stream().map(Object::toString).toList()));

		List<String> flows = mapGrantTypes(meta);
		if (!flows.isEmpty())
			attrs.add(EnumAttribute.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS, oauthGroup, flows));

		String clientType = ApplicationType.NATIVE.equals(meta.getApplicationType())
				? ClientType.PUBLIC.toString()
				: ClientType.CONFIDENTIAL.toString();
		attrs.add(EnumAttribute.of(OAuthSystemAttributesProvider.CLIENT_TYPE, oauthGroup, clientType));

		String displayName = toDisplayName(meta, clientId);
		if (displayName != null)
			attrs.add(StringAttribute.of(OAuthSystemAttributesProvider.CLIENT_NAME, oauthGroup, displayName));

		boolean isPublic = ClientType.PUBLIC.toString().equals(clientType);
		if (!isPublic)
		{
			ClientAuthenticationMethod metaAuthnMethod = meta.getTokenEndpointAuthMethod();
			if (metaAuthnMethod != null && !ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(metaAuthnMethod))
				log.warn(
						"Federation client {} declares token_endpoint_auth_method={}, overriding to private_key_jwt as required by OpenID Federation",
						clientId, metaAuthnMethod);
			attrs.add(EnumAttribute.of(OAuthSystemAttributesProvider.CLIENT_AUTHN_METHOD, oauthGroup,
					ClientAuthnMethod.private_key_jwt.toString()));
		}
		
		return attrs;
	}

	public static String toDisplayName(OIDCClientMetadata meta, String clientId)
	{
		String name = meta.getName() != null ? meta.getName() : meta.getOrganizationName();
		String baseName = name != null ? name : clientId;
		return baseName != null ? "[Federated] " + baseName : null;
	}

	private static List<String> mapGrantTypes(OIDCClientMetadata meta)
	{
		if (meta.getGrantTypes() == null)
			return List.of(GrantFlow.authorizationCode.toString());

		List<String> flows = new ArrayList<>();
		for (var gt : meta.getGrantTypes())
		{
			switch (gt.getValue())
			{
				case "authorization_code" -> flows.add(GrantFlow.authorizationCode.toString());
				case "implicit" -> flows.add(GrantFlow.implicit.toString());
				case "client_credentials" -> flows.add(GrantFlow.client.toString());
				default -> { }
			}
		}
		return flows.isEmpty() ? List.of(GrantFlow.authorizationCode.toString()) : flows;
	}
}
