/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static pl.edu.icm.unity.oauth.as.OAuthASProperties.CREDENTIAL;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.ISSUER_URI;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.P;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.SCOPES;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.SCOPE_NAME;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;

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
	
	public static OAuthAuthzContext createContext(ResponseType respType, GrantFlow grant) throws Exception
	{
		AuthorizationRequest request = new AuthorizationRequest(null, respType, 
				new ClientID("clientC"), new URI("https://return.host.com/foo"), 
				null, new State("state123"));
		X509Credential credential = new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), null, "pkcs12");
		OAuthAuthzContext ctx = new OAuthAuthzContext(
				request, 
				100, 
				200, 
				300, 
				"https://localhost:2443/oauth-as", 
				credential);
		ctx.setClientEntityId(100);
		ctx.setClientName("clientC");
		ctx.setFlow(grant);
		ctx.setOpenIdMode(true);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		ctx.addScopeInfo(new ScopeInfo("sc1", "scope 1", Lists.newArrayList("email")));
		return ctx;
	}
	
	
	public static AuthorizationSuccessResponse initOAuthFlowHybrid(TokensManagement tokensMan) throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		IdentityParam identity = new IdentityParam("username", "userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(new ResponseType(ResponseType.Value.TOKEN, 
				OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.CODE),
				GrantFlow.openidHybrid);
		
		return processor.prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, tokensMan);
	}
	
	public static AuthorizationSuccessResponse initOAuthFlowAccessCode(TokensManagement tokensMan) throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		IdentityParam identity = new IdentityParam("username", "userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode);

		return processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
	}

}
