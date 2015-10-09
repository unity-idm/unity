/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;

import com.google.common.collect.Lists;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * RESTful implementation of the OIDC Discovery endpoint. Free access.
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path("/.well-known")
public class DiscoveryResource extends BaseOAuthResource
{
	private OAuthASProperties config;
	private OAuthEndpointsCoordinator coordinator;
	
	public DiscoveryResource(OAuthASProperties config, OAuthEndpointsCoordinator coordinator)
	{
		this.config = config;
		this.coordinator = coordinator;
	}

	@Path("/openid-configuration")
	@GET
	public Response getMetadata()
	{
		String issuerUri = config.getValue(OAuthASProperties.ISSUER_URI);
		List<SubjectType> supportedSubjects = Lists.newArrayList(SubjectType.PUBLIC);
		String baseUri = config.getBaseAddress();
		OIDCProviderMetadata meta;
		try
		{
			URI jwkUri = new URI(baseUri + OAuthTokenEndpoint.JWK_PATH);
			URI tokenEndpointUri = new URI(baseUri + OAuthTokenEndpoint.TOKEN_PATH);
			URI userInfoEndpointUri = new URI(baseUri + OAuthTokenEndpoint.USER_INFO_PATH);
			URI authzEndpointUri = new URI(coordinator.getAuthzEndpoint(issuerUri));
			meta = new OIDCProviderMetadata(new Issuer(issuerUri), supportedSubjects, jwkUri);
			meta.setAuthorizationEndpointURI(authzEndpointUri);
			meta.setTokenEndpointURI(tokenEndpointUri);
			meta.setUserInfoEndpointURI(userInfoEndpointUri);
		} catch (URISyntaxException e)
		{
			throw new InternalException("Can't encode URI", e);
		}
		
		Set<String> scopeKeys = config.getStructuredListKeys(OAuthASProperties.SCOPES);
		Set<String> scopes = new HashSet<>();
		for (String scopeKey: scopeKeys)
		{
			String scope = config.getValue(scopeKey+OAuthASProperties.SCOPE_NAME);
			scopes.add(scope);
		}
		meta.setScopes(new Scope(scopes.toArray(new String[scopes.size()])));
		
		ResponseType rt1 = new ResponseType(ResponseType.Value.CODE);
		ResponseType rt2 = new ResponseType(ResponseType.Value.TOKEN);
		ResponseType rt3 = new ResponseType(OIDCResponseTypeValue.ID_TOKEN);
		ResponseType rt4 = new ResponseType(ResponseType.Value.CODE, OIDCResponseTypeValue.ID_TOKEN);
		ResponseType rt5 = new ResponseType(ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN);
		ResponseType rt6 = new ResponseType(ResponseType.Value.TOKEN, ResponseType.Value.CODE);
		ResponseType rt7 = new ResponseType(ResponseType.Value.TOKEN, ResponseType.Value.CODE, 
				OIDCResponseTypeValue.ID_TOKEN);
		List<ResponseType> l = new ArrayList<ResponseType>();
		Collections.addAll(l, rt1, rt2, rt3, rt4, rt5, rt6, rt7);
		meta.setResponseTypes(l);
		
		meta.setResponseModes(Lists.newArrayList(ResponseMode.QUERY, ResponseMode.FRAGMENT));
		meta.setGrantTypes(Lists.newArrayList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT));
		meta.setIDTokenJWSAlgs(Lists.newArrayList(JWSAlgorithm.RS256, JWSAlgorithm.ES256));
		
		return toResponse(Response.ok(meta.toJSONObject().toJSONString()));
	}
}
