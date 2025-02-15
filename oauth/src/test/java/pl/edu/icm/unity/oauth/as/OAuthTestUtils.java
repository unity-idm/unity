/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.ACCESS_TOKEN_VALIDITY;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.CODE_TOKEN_VALIDITY;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.CREDENTIAL;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.IDENTITY_TYPE_FOR_SUBJECT;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.ID_TOKEN_VALIDITY;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.ISSUER_URI;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.P;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.SCOPES;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.SCOPE_NAME;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.springframework.context.ApplicationEventPublisher;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class OAuthTestUtils
{
	public static final String TOKEN_OWNING_CLIENT_CLIENT_ID = "clientC";
	public static final long TOKEN_OWNING_CLIENT_ENTITY_ID = 100;
	public static final String ISSUER = "https://localhost:233/foo/token";
	public static final String BASE_ADDR = "https://localhost:233/foo";
	
	public static final int DEFAULT_ACCESS_TOKEN_VALIDITY = 100;
	
	public static OAuthASProperties getConfig()
	{
		return getConfig(DEFAULT_ACCESS_TOKEN_VALIDITY, 0, false);
	}

	public static OAuthASProperties getOIDCConfig()
	{
		return getConfig(DEFAULT_ACCESS_TOKEN_VALIDITY, 0, true);
	}
	
	public static OAuthProcessor getOAuthProcessor(TokensManagement tokensMan)
	{
		return new OAuthProcessor(tokensMan, new OAuthAccessTokenRepository(tokensMan, 
				mock(SecuredTokensManagement.class)), mock(ApplicationEventPublisher.class), mock(MessageSource.class));
	}
	
	public static OAuthASProperties getConfig(int accessTokenValidity, int maxValidity, boolean oidc)
	{
		Properties properties = new Properties();
		properties.setProperty(P + ISSUER_URI, ISSUER);
		properties.setProperty(P + CREDENTIAL, "MAIN");
		properties.setProperty(P + CommonIdPProperties.SKIP_CONSENT, "false");
		properties.setProperty(P + ACCESS_TOKEN_VALIDITY, accessTokenValidity+"");
		if (maxValidity > 0)
			properties.setProperty(P + MAX_EXTEND_ACCESS_TOKEN_VALIDITY, maxValidity+"");
		properties.setProperty(P + CODE_TOKEN_VALIDITY, "200");
		properties.setProperty(P + ID_TOKEN_VALIDITY, "300");
		properties.setProperty(P + IDENTITY_TYPE_FOR_SUBJECT, TargetedPersistentIdentity.ID);
		properties.setProperty(P + SCOPES + "1." + SCOPE_NAME, "s1");
		properties.setProperty(P + SCOPES + "2." + SCOPE_NAME, "s2");
		if (oidc)
		{
			properties.setProperty(P + SCOPES + "3." + SCOPE_NAME, "openid");
		}
		properties.setProperty(P + SCOPES + "4." + SCOPE_NAME, "offline_access");

		PKIManagement pkiManagement = new MockPKIMan();
		return new OAuthASProperties(properties, pkiManagement, BASE_ADDR);
	}
	
	public static OAuthAuthzContext createOIDCContext(OAuthASProperties config, 
			ResponseType respType, GrantFlow grant, 
			long clientEntityId, String nonce) throws Exception
	{
		AuthenticationRequest request = new AuthenticationRequest.Builder(
					respType, 
					new Scope(OIDCScopeValue.OPENID), 
					new ClientID(TOKEN_OWNING_CLIENT_CLIENT_ID), 
					new URI("https://return.host.com/foo")).
				state(new State("state123")).
				nonce(new Nonce(nonce)).
				build(); 
		OAuthAuthzContext ctx = new OAuthAuthzContext(request, config);
		ctx.setClientEntityId(clientEntityId);
		ctx.setClientUsername(TOKEN_OWNING_CLIENT_CLIENT_ID);
		ctx.setFlow(grant);
		ctx.setOpenIdMode(true);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		ctx.addEffectiveScopeInfo(OAuthScope.builder().withName("sc1").withDescription("scope 1")
				.withAttributes(Lists.newArrayList("email")).withEnabled(true).build());
		return ctx;
	}

	public static OAuthAuthzContext createContext(OAuthASProperties config, 
			ResponseType respType, GrantFlow grant, 
			long clientEntityId) throws Exception
	{
		return createContext(config, respType, grant, clientEntityId, ClientType.CONFIDENTIAL);
	}
	
	public static OAuthAuthzContext createContext(OAuthASProperties config, 
			ResponseType respType, GrantFlow grant, 
			long clientEntityId, ClientType clientType) throws Exception
	{
		AuthenticationRequest request = new AuthenticationRequest(null, respType, new Scope("openid"),
				new ClientID(TOKEN_OWNING_CLIENT_CLIENT_ID), new URI("https://return.host.com/foo"), 
				null, new Nonce("nonce"));
		
		OAuthAuthzContext ctx = new OAuthAuthzContext(request, config);
		ctx.setClientEntityId(clientEntityId);
		ctx.setClientUsername(TOKEN_OWNING_CLIENT_CLIENT_ID);
		ctx.setClientType(clientType);
		ctx.setFlow(grant);
		ctx.setOpenIdMode(false);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		ctx.addEffectiveScopeInfo(OAuthScope.builder().withName("sc1").withDescription("scope 1")
				.withAttributes(Lists.newArrayList("email")).withEnabled(true).build());
		return ctx;
	}

	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(OAuthASProperties config, 
			OAuthProcessor processor, ClientType clientType) throws Exception
	{
		return initOAuthFlowHybrid(config, processor, clientType, TOKEN_OWNING_CLIENT_ENTITY_ID);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(OAuthASProperties config, 
			OAuthProcessor processor, ClientType clientType, long clientId) throws Exception
	{
		Collection<DynamicAttribute> attributes = new ArrayList<>();
		attributes.add(new DynamicAttribute(StringAttribute.of("email", "/", "example@example.com")));
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.TOKEN, 
				OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.CODE),
				GrantFlow.openidHybrid, clientId, clientType);
		
		return processor.prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, mock(OAuthIdpStatisticReporter.class));
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(OAuthASProperties config, 
			OAuthProcessor processor) throws Exception
	{
		return initOAuthFlowHybrid(config, processor, ClientType.CONFIDENTIAL, TOKEN_OWNING_CLIENT_ENTITY_ID);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(OAuthASProperties config, 
			OAuthProcessor processor, long clientId) throws Exception
	{
		return initOAuthFlowHybrid(config, processor, ClientType.CONFIDENTIAL, clientId);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowAccessCode(OAuthProcessor processor, 
			OAuthAuthzContext ctx) throws Exception
	{
	
		IdentityParam identity = new IdentityParam("userName", "userA");

		return initOAuthFlowAccessCode(processor, ctx, identity);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowAccessCode(OAuthProcessor processor, 
			OAuthAuthzContext ctx, IdentityParam identity) throws Exception
	{
		Collection<DynamicAttribute> attributes = new ArrayList<>();
		attributes.add(new DynamicAttribute(StringAttribute.of("email", "/", "example@example.com")));
		attributes.add(new DynamicAttribute(StringAttribute.of("c", "/", "PL")));
		
		return processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, mock(OAuthIdpStatisticReporter.class));
	}

	public static Identity createOauthClient(EntityManagement idsMan, AttributesManagement attrsMan,
			GroupsManagement groupsMan, EntityCredentialManagement eCredMan, String username) throws Exception
	{
		Identity clientId1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, username), 
				"cr-pass", EntityState.valid);
		EntityParam e1 = new EntityParam(clientId1);
		eCredMan.setEntityCredential(e1, "credential1", new PasswordToken("clientPass").toJson());

		if (!groupsMan.isPresent("/oauth-clients"))
			groupsMan.addGroup(new Group("/oauth-clients"));
		groupsMan.addMemberFromParent("/oauth-clients", e1);
		
		if (!groupsMan.isPresent("/oauth-users"))
			groupsMan.addGroup(new Group("/oauth-users"));
		groupsMan.addMemberFromParent("/oauth-users", e1);
		
		attrsMan.createAttribute(e1, EnumAttribute.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS, 
				"/oauth-clients", 
				Lists.newArrayList(GrantFlow.authorizationCode.name(),
						GrantFlow.client.name())));
		attrsMan.createAttribute(e1, StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI, 
				"/oauth-clients", "https://dummy-return.net"));
		
		attrsMan.createAttribute(e1, StringAttribute.of(OAuthSystemAttributesProvider.CLIENT_NAME, 
				"/oauth-clients", "clientName"));
		
		attrsMan.createAttribute(e1, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", "Regular User"));
		return clientId1;
	}
	
	public static ResolvedEndpoint getEndpoint()
	{
		return new ResolvedEndpoint(
				new Endpoint("name", null, null, new EndpointConfiguration(null, null, null, null, null), 0), null,
				null);
	}
	
	
}
