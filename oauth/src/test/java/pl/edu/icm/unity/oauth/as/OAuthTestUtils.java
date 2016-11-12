/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.CREDENTIAL;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.ISSUER_URI;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.P;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.SCOPES;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.SCOPE_NAME;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.mockito.Mockito;

import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.CommonIdPProperties;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;

public class OAuthTestUtils
{
	public static OAuthASProperties getConfig()
	{
		Properties properties = new Properties();
		properties.setProperty(P + ISSUER_URI, "https://localhost:233/foo/token");
		properties.setProperty(P + CREDENTIAL, "MAIN");
		properties.setProperty(P + SCOPES + "1." + SCOPE_NAME, "s1");
		properties.setProperty(P + SCOPES + "2." + SCOPE_NAME, "s2");
		PKIManagement pkiManagement = new MockPKIMan();
		return new OAuthASProperties(properties, pkiManagement, 
				"https://localhost:233/foo");
	}
	
	public static OAuthAuthzContext createContext(ResponseType respType, GrantFlow grant, 
			long clientEntityId) throws Exception
	{
		return createContext(respType, grant, clientEntityId, 100, 0);
	}

	public static OAuthAuthzContext createOIDCContext(ResponseType respType, GrantFlow grant, 
			long clientEntityId, int accessTokenValidity, int maxExtValidity, String nonce) throws Exception
	{
		AuthenticationRequest request = new AuthenticationRequest.Builder(
					respType, 
					new Scope(OIDCScopeValue.OPENID), 
					new ClientID("clientC"), 
					new URI("https://return.host.com/foo")).
				state(new State("state123")).
				nonce(new Nonce(nonce)).
				build(); 
		X509Credential credential = new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), null, "pkcs12");
		OAuthASProperties mockedProps = Mockito.mock(OAuthASProperties.class);
		when(mockedProps.getBooleanValue(eq(CommonIdPProperties.SKIP_USERIMPORT))).thenReturn(Boolean.FALSE);
		
		OAuthAuthzContext ctx = new OAuthAuthzContext(
				request, mockedProps,
				accessTokenValidity, 
				maxExtValidity,
				200, 
				300, 
				"https://localhost:2443/oauth-as", 
				credential,
				false,
				TargetedPersistentIdentity.ID);
		ctx.setClientEntityId(clientEntityId);
		ctx.setClientUsername("clientC");
		ctx.setFlow(grant);
		ctx.setOpenIdMode(true);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		ctx.addScopeInfo(new ScopeInfo("sc1", "scope 1", Lists.newArrayList("email")));
		return ctx;
	}
	
	public static OAuthAuthzContext createContext(ResponseType respType, GrantFlow grant, 
			long clientEntityId, int accessTokenValidity, int maxExtValidity) throws Exception
	{
		AuthorizationRequest request = new AuthorizationRequest(null, respType, null,
				new ClientID("clientC"), new URI("https://return.host.com/foo"), 
				null, new State("state123"));
		X509Credential credential = new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), null, "pkcs12");
		OAuthASProperties mockedProps = Mockito.mock(OAuthASProperties.class);
		when(mockedProps.getBooleanValue(eq(CommonIdPProperties.SKIP_USERIMPORT))).thenReturn(Boolean.FALSE);
		
		OAuthAuthzContext ctx = new OAuthAuthzContext(
				request, mockedProps,
				accessTokenValidity, 
				maxExtValidity,
				200, 
				300, 
				"https://localhost:2443/oauth-as", 
				credential,
				false,
				TargetedPersistentIdentity.ID);
		ctx.setClientEntityId(clientEntityId);
		ctx.setClientUsername("clientC");
		ctx.setFlow(grant);
		ctx.setOpenIdMode(false);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		ctx.addScopeInfo(new ScopeInfo("sc1", "scope 1", Lists.newArrayList("email")));
		return ctx;
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(TokensManagement tokensMan, int accessTokenValidity, 
			int maxExtValidity) throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(new ResponseType(ResponseType.Value.TOKEN, 
				OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.CODE),
				GrantFlow.openidHybrid, 100, accessTokenValidity, maxExtValidity);
		
		return processor.prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, tokensMan);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(TokensManagement tokensMan) throws Exception
	{
		return initOAuthFlowHybrid(tokensMan, 100, 0);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowAccessCode(TokensManagement tokensMan, 
			OAuthAuthzContext ctx) throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		attributes.add(new StringAttribute("c", "/", AttributeVisibility.full, "PL"));
		IdentityParam identity = new IdentityParam("userName", "userA");

		return processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
	}

	public static Identity createOauthClient(IdentitiesManagement idsMan, AttributesManagement attrsMan,
			GroupsManagement groupsMan) throws Exception
	{
		Identity clientId = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "client1"), 
				"cr-pass", EntityState.valid, false);
		EntityParam e1 = new EntityParam(clientId);
		idsMan.setEntityCredential(e1, "credential1", new PasswordToken("clientPass").toJson());

		groupsMan.addGroup(new Group("/oauth-clients"));
		groupsMan.addMemberFromParent("/oauth-clients", e1);
		
		groupsMan.addGroup(new Group("/oauth-users"));
		groupsMan.addMemberFromParent("/oauth-users", e1);
		
		attrsMan.setAttribute(e1, new EnumAttribute(OAuthSystemAttributesProvider.ALLOWED_FLOWS, 
				"/oauth-clients", AttributeVisibility.local, 
				Lists.newArrayList(GrantFlow.authorizationCode.name(),
						GrantFlow.client.name())), 
				false);
		attrsMan.setAttribute(e1, new StringAttribute(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI, 
				"/oauth-clients", AttributeVisibility.local, "https://dummy-return.net"), false);
		
		attrsMan.setAttribute(e1, new StringAttribute(OAuthSystemAttributesProvider.CLIENT_NAME, 
				"/oauth-clients", AttributeVisibility.local, "clientName"), false);
		
		attrsMan.setAttribute(e1, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, "Regular User"), false);
		return clientId;
	}
	
}
