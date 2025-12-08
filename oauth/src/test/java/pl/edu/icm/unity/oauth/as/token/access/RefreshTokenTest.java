/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.ACRRequest;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.webauthz.ClaimsInTokenAttribute;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * An integration test of token refresh flow
 * @author P.Piernik
 *
 */
public class RefreshTokenTest extends TokenTestBase
{
	private RefreshToken initRefresh(Set<String> scope, ClientAuthentication ca) throws Exception
	{
		return init(scope, ca, null, null).getTokens().getRefreshToken();	
	}
	

	@Test
	public void shouldRefreshToken() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar"), ca);

		AccessTokenResponse parsedResp = getRefreshedAccessToken(refreshToken, ca, "foo", "bar");
		BearerAccessToken bearerToken = (BearerAccessToken) parsedResp.getTokens().getAccessToken();

		assertThat(bearerToken.getLifetime()).isEqualTo(3600l);
		assertThat(bearerToken.getScope()).isEqualTo(new Scope("foo", "bar"));
		assertThat(bearerToken.getType()).isEqualTo(AccessTokenType.BEARER);
	}

	@Test
	public void shouldNotRefreshToken() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.NEVER);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar"), ca);
		assertThat(refreshToken).isNull();
	}

	@Test
	public void shouldNotIssueRefreshTokenBasedOnOfflinePolicyWithoutOfflineScope() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar"), ca);
		assertThat(refreshToken).isNull();
	}

	@Test
	public void shouldIssueRefreshTokenBasedOnOfflinePolicyWithOfflineScope() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar", OIDCScopeValue.OFFLINE_ACCESS.getValue()),
				ca);
		assertThat(refreshToken).isNotNull();
	}

	@Test
	public void shouldAssumeOriginalScopesWhenNoScopesAreRequestedUponRefresh() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar"), ca);

		AccessTokenResponse parsedResp = getRefreshedAccessToken(refreshToken, ca);
		BearerAccessToken bearerToken = (BearerAccessToken) parsedResp.getTokens().getAccessToken();
		
		assertThat(bearerToken.getLifetime()).isEqualTo(3600l);
		assertThat(bearerToken.getScope()).isEqualTo(new Scope("foo", "bar"));
		assertThat(bearerToken.getType()).isEqualTo(AccessTokenType.BEARER);
	}
	
	@Test
	public void refreshedTokenCanBeUsedToObtainUserInfo() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		
		RefreshToken refreshToken =init(List.of(
		new RequestedOAuthScope("foo", ActiveOAuthScopeDefinition.builder()
				.withName("foo")
				.withDescription("foo")
				.withAttributes(List.of("email"))
				.withPattern(true)
				.build(), true), 
		new RequestedOAuthScope("bar", ActiveOAuthScopeDefinition.builder()
				.withName("bar")
				.withDescription("bar")
				.withAttributes(List.of("c"))
				.withPattern(true)
				.build(), true)), ca, null, null).getTokens().getRefreshToken();
		
		
		JWTClaimsSet claimSet = refreshAndGetUserInfo(refreshToken, ca, "foo", "bar");

		assertThat(claimSet.getClaim("c")).isEqualTo("PL");
		assertThat(claimSet.getClaim("email")).isEqualTo("example@example.com");
		assertThat(claimSet.getClaim("sub")).isEqualTo("userA");
	}

	@Test
	public void shouldRefreshTokenWithIdToken() throws Exception
	{
		super.setupOIDC(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("openid"), ca);

		TokenRequest refreshRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca,
				new RefreshTokenGrant(refreshToken), new Scope("openid"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse refreshResp = wrapped.send();
		AccessTokenResponse refreshParsedResp = AccessTokenResponse.parse(refreshResp);
		assertThat(refreshParsedResp.getTokens().getAccessToken()).isNotNull();
		assertThat(refreshParsedResp.getCustomParameters().get("id_token")).isNotNull();
	
	}

	@Test
	public void shouldDenyToRefreshTokenWithIncorrectScope() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar"), ca);

		// check wrong scope
		TokenRequest refreshRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca,
				new RefreshTokenGrant(refreshToken), new Scope("xx"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode()).isEqualTo(HTTPResponse.SC_BAD_REQUEST);
	}

	@Test
	public void shouldDenyToRefreshTokenByAnotherClient() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Set.of("foo", "bar"), ca);

		TokenRequest refreshRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca2,
				new RefreshTokenGrant(refreshToken), new Scope("foo"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode()).isEqualTo(HTTPResponse.SC_BAD_REQUEST);

	}
	
	@Test
	public void shouldRefreshUserInfoAfterRefreshToken() throws Exception
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken =init(List.of(
				new RequestedOAuthScope("foo", ActiveOAuthScopeDefinition.builder()
						.withName("foo")
						.withDescription("foo")
						.withAttributes(List.of("email"))
						.withPattern(true)
						.build(), true), 
				new RequestedOAuthScope("bar", ActiveOAuthScopeDefinition.builder()
						.withName("bar")
						.withDescription("bar")
						.withAttributes(List.of("c"))
						.withPattern(true)
						.build(), true)), ca, null, null).getTokens().getRefreshToken();

		JWTClaimsSet claimSet = refreshAndGetUserInfo(refreshToken, ca, "foo", "bar");	
		
		assertThat(claimSet.getClaim("c")).isEqualTo("PL");
		
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		attrsMan.setAttribute(new EntityParam(identity),
				StringAttribute.of("c", "/oauth-users", "new"));
		
		claimSet = refreshAndGetUserInfo(refreshToken, ca, "foo", "bar");
		assertThat(claimSet.getClaim("c")).isEqualTo("new");	
		
	}
	
	@Test
	public void shouldPreserveAcrAndAuthnTimeAfterTokenRefresh() throws Exception
	{
		super.setupOIDC(RefreshTokenIssuePolicy.ALWAYS);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));
		IdentityParam identity = initUser("userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getOIDCConfig(),
				new ResponseType(ResponseType.Value.CODE), GrantFlow.authorizationCode, clientId1.getEntityId());
		ctx.setRequestedScopes(Set.of("openid"));
		ctx.addEffectiveScopeInfo(new RequestedOAuthScope("openid", ActiveOAuthScopeDefinition.builder().withName("openid").build(), false));
		ctx.setOpenIdMode(true);
		ctx.setRequestedAcr(new ACRRequest(List.of(new ACR("acrReq")), null));
		ctx.setClaimsInTokenAttribute(Optional.of(ClaimsInTokenAttribute.builder().withValues(Set.of(ClaimsInTokenAttribute.Value.token)).build()));
		InvocationContext simpltyContext = new InvocationContext(null, new AuthenticationRealm(), Collections.emptyList());
		LoginSession session = new LoginSession();
		session.setFirstFactorRemoteIdPAuthnContext(new RemoteAuthnMetadata(Protocol.OIDC, "idp", List.of("acr1")));
		simpltyContext.setLoginSession(session);
		InvocationContext.setCurrent(simpltyContext);
		Instant authn_time = Instant.now();
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx, identity);
		OAuthTestUtils.getOAuthProcessor(tokensMan).prepareAuthzResponseAndRecordInternalState(
				List.of(), identity, ctx, mock(OAuthIdpStatisticReporter.class), authn_time , null);
		TokenRequest request = new TokenRequest.Builder(new URI(getOauthUrl("/oauth/token")), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(), new URI("https://return.host.com/foo")))
						.build();
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse resp2 = wrapped.send();
		AccessTokenResponse parsedInitialResp = AccessTokenResponse.parse(resp2);
		
		AccessTokenResponse refreshParsedResp = getRefreshedAccessToken(parsedInitialResp.getTokens().getRefreshToken(), ca, "openid");
		
		SignedJWT signedJWT = SignedJWT.parse(refreshParsedResp.getTokens().getAccessToken().getValue());
		assertThat(signedJWT.getJWTClaimsSet().getClaim("acr")).isEqualTo("acr1");
		assertThat(signedJWT.getJWTClaimsSet().getClaim("auth_time")).isEqualTo(authn_time.getEpochSecond());	
	}

	private AccessTokenResponse getRefreshedAccessToken(RefreshToken token, 
			ClientAuthentication ca, String... scopes) throws Exception
	{
		TokenRequest refreshRequest = new TokenRequest(
				new URI(getOauthUrl("/oauth/token")), ca,
				new RefreshTokenGrant(token), new Scope(scopes));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse refreshResp = wrapped.send();
		return AccessTokenResponse.parse(refreshResp);
	}
	
	private JWTClaimsSet refreshAndGetUserInfo(RefreshToken token, 
			ClientAuthentication ca, String... scopes) throws Exception
	{
		AccessTokenResponse refreshParsedResp = getRefreshedAccessToken(token, ca, scopes);
		assertThat(refreshParsedResp.getTokens().getAccessToken()).isNotNull();
		return getUserInfo(refreshParsedResp.getTokens().getAccessToken());
	}

	private JWTClaimsSet getUserInfo(AccessToken accessToken) throws Exception
	{
		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI(getUserInfoUrl()),
				(BearerAccessToken) accessToken);
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new HttpRequestConfigurer().secureRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse uiHttpResponse = wrapped2.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		UserInfo ui = uiResponseS.getUserInfo();
		JWTClaimsSet claimSet = ui.toJWTClaimsSet();
		return claimSet;
	}

	private String getUserInfoUrl()
	{
		return getOauthUrl("/oauth/userinfo");
	}
}