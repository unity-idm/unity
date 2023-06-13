/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;

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
	private OAuthScopesService scopeService;
	
	public DiscoveryResource(OAuthASProperties config, OAuthEndpointsCoordinator coordinator,
			OAuthScopesService scopeService)
	{
		this.config = config;
		this.coordinator = coordinator;
		this.scopeService = scopeService;
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
			meta.setIntrospectionEndpointURI(new URI(baseUri + OAuthTokenEndpoint.TOKEN_INTROSPECTION_PATH));
			meta.setRevocationEndpointURI(new URI(baseUri + OAuthTokenEndpoint.TOKEN_REVOCATION_PATH));
		} catch (URISyntaxException e)
		{
			throw new InternalException("Can't encode URI", e);
		}

		
		
		meta.setCodeChallengeMethods(Lists.newArrayList(CodeChallengeMethod.PLAIN, CodeChallengeMethod.S256));
		
		List<String> scopes = scopeService.getActiveScopeNames(config);	
		meta.setScopes(new Scope(scopes.toArray(new String[scopes.size()])));
		
		ResponseType rt1 = new ResponseType(ResponseType.Value.CODE);
		ResponseType rt2 = new ResponseType(ResponseType.Value.TOKEN);
		ResponseType rt3 = new ResponseType(OIDCResponseTypeValue.ID_TOKEN);
		ResponseType rt4 = new ResponseType(ResponseType.Value.CODE, OIDCResponseTypeValue.ID_TOKEN);
		ResponseType rt5 = new ResponseType(ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN);
		ResponseType rt6 = new ResponseType(ResponseType.Value.TOKEN, ResponseType.Value.CODE);
		ResponseType rt7 = new ResponseType(ResponseType.Value.TOKEN, ResponseType.Value.CODE, 
				OIDCResponseTypeValue.ID_TOKEN);
		meta.setResponseTypes(Lists.newArrayList(rt1, rt2, rt3, rt4, rt5, rt6, rt7));
		
		meta.setResponseModes(Lists.newArrayList(ResponseMode.QUERY, ResponseMode.FRAGMENT));
		meta.setGrantTypes(Lists.newArrayList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT));
		meta.setIDTokenJWSAlgs(Lists.newArrayList(JWSAlgorithm.RS256, JWSAlgorithm.ES256));
		
		return toResponse(Response.ok(meta.toJSONObject().toJSONString()));
	}
}
