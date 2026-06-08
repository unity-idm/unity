/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.logging.log4j.Logger;

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
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.emi.security.authn.x509.X509Credential;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;

@Path("/.well-known")
public class OAuthASFederationEntityStatementResource extends BaseOAuthResource
{
	static final String ENTITY_STATEMENT_MEDIA_TYPE = "application/entity-statement+jwt";

	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			OAuthASFederationEntityStatementResource.class);

	private final OAuthASProperties config;
	private final OAuthEndpointsCoordinator coordinator;
	private final OAuthScopesService scopeService;
	private final PKIManagement pkiManagement;

	public OAuthASFederationEntityStatementResource(OAuthASProperties config, OAuthEndpointsCoordinator coordinator,
			OAuthScopesService scopeService, PKIManagement pkiManagement)
	{
		this.config = config;
		this.coordinator = coordinator;
		this.scopeService = scopeService;
		this.pkiManagement = pkiManagement;
	}

	@Path("/openid-federation")
	@GET
	@Produces(ENTITY_STATEMENT_MEDIA_TYPE)
	public Response getEntityStatement()
	{
		if (!config.getBooleanValue(OAuthASProperties.FEDERATION_MEMBERSHIP_ENABLED))
			return Response.status(Response.Status.NOT_FOUND).build();

		String credentialName = config.getValue(OAuthASProperties.FEDERATION_CREDENTIAL);
		String issuerUri = config.getValue(OAuthASProperties.ISSUER_URI);
		String superiorEntityId = config.getValue(OAuthASProperties.FEDERATION_SUPERIOR_ENTITY_ID);
		long validity = config.getIntValue(OAuthASProperties.FEDERATION_METADATA_VALIDITY);

		try
		{
			X509Credential federationCredential = pkiManagement.getCredential(credentialName);
			OIDCProviderMetadata providerMetadata = buildProviderMetadata(issuerUri);
			String entityStatement = OAuthASFederationEntityStatementGenerator
					.generate(issuerUri, federationCredential, superiorEntityId, validity, providerMetadata)
					.getSignedStatement()
					.serialize();
			return Response.ok(entityStatement).build();
		} catch (EngineException e)
		{
			log.error("Failed to load federation credential {}", credentialName, e);
			return Response.serverError().build();
		} catch (Exception e)
		{
			log.error("Failed to generate federation entity statement", e);
			return Response.serverError().build();
		}
	}

	private OIDCProviderMetadata buildProviderMetadata(String issuerUri) throws URISyntaxException
	{
		String baseUri = config.getBaseAddress();
		URI jwkUri = new URI(baseUri + OAuthTokenEndpoint.JWK_PATH);
		URI tokenEndpointUri = new URI(baseUri + OAuthTokenEndpoint.TOKEN_PATH);
		URI userInfoEndpointUri = new URI(baseUri + OAuthTokenEndpoint.USER_INFO_PATH);
		URI authzEndpointUri = new URI(coordinator.getAuthzEndpoint(issuerUri));

		OIDCProviderMetadata meta = new OIDCProviderMetadata(new Issuer(issuerUri),
				List.of(SubjectType.PUBLIC), jwkUri);
		meta.setAuthorizationEndpointURI(authzEndpointUri);
		meta.setTokenEndpointURI(tokenEndpointUri);
		meta.setUserInfoEndpointURI(userInfoEndpointUri);
		meta.setIntrospectionEndpointURI(new URI(baseUri + OAuthTokenEndpoint.TOKEN_INTROSPECTION_PATH));
		meta.setRevocationEndpointURI(new URI(baseUri + OAuthTokenEndpoint.TOKEN_REVOCATION_PATH));

		meta.setCodeChallengeMethods(Lists.newArrayList(CodeChallengeMethod.PLAIN, CodeChallengeMethod.S256));

		List<String> scopes = scopeService.getActiveScopeNames(config);
		meta.setScopes(new Scope(scopes.toArray(new String[0])));

		meta.setResponseTypes(Lists.newArrayList(
				new ResponseType(ResponseType.Value.CODE),
				new ResponseType(ResponseType.Value.TOKEN),
				new ResponseType(OIDCResponseTypeValue.ID_TOKEN),
				new ResponseType(ResponseType.Value.CODE, OIDCResponseTypeValue.ID_TOKEN),
				new ResponseType(ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN),
				new ResponseType(ResponseType.Value.TOKEN, ResponseType.Value.CODE),
				new ResponseType(ResponseType.Value.TOKEN, ResponseType.Value.CODE,
						OIDCResponseTypeValue.ID_TOKEN)));

		meta.setResponseModes(Lists.newArrayList(ResponseMode.QUERY, ResponseMode.FRAGMENT));
		meta.setGrantTypes(Lists.newArrayList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT));
		meta.setIDTokenJWSAlgs(Lists.newArrayList(JWSAlgorithm.RS256, JWSAlgorithm.ES256));

		meta.setClientRegistrationTypes(List.of(ClientRegistrationType.AUTOMATIC));

		return meta;
	}
}
