/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.console.tokens;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BearerJWTAccessToken;

/**
 * Show Oauth token details
 * 
 * @author P.Piernik
 *
 */
class OAuthTokenViewer extends VerticalLayout
{
	private MessageSource msg;
	private FormLayout main;
	private TextArea token;
	private TextArea jwtClaimsSet;
	private Span jwtInfo;
	private TextArea idToken;
	private Span audience;
	private Span redirectUri;
	private Span maxTokenValidity;
	private Span requestedScopes;
	private Span effectiveScopes;
	private FormLayout userInfoComponent;

	OAuthTokenViewer(MessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		token = new TextArea();
		token.setSizeFull();
		token.setReadOnly(true);
		main.addFormItem(token, msg.getMessage("OAuthTokenViewer.token"));

		jwtClaimsSet = new TextArea();
		jwtClaimsSet.setSizeFull();
		jwtClaimsSet.setReadOnly(true);
		main.addFormItem(jwtClaimsSet, msg.getMessage("OAuthTokenViewer.jwtClaims"));

		jwtInfo = new Span();
		main.addFormItem(jwtInfo, msg.getMessage("OAuthTokenViewer.jwtInfo"));
		
		idToken = new TextArea();
		idToken.setSizeFull();
		idToken.setReadOnly(true);
		main.addFormItem(idToken, msg.getMessage("OAuthTokenViewer.idtoken"));

		audience = new Span();
		main.addFormItem(audience, msg.getMessage("OAuthTokenViewer.audience"));

		redirectUri = new Span();
		main.addFormItem(redirectUri, msg.getMessage("OAuthTokenViewer.redirectUri"));

		maxTokenValidity = new Span();
		main.addFormItem(maxTokenValidity, msg.getMessage("OAuthTokenViewer.maxTokenValidity"));

		requestedScopes = new Span();
		main.addFormItem(requestedScopes, msg.getMessage("OAuthTokenViewer.requestedScopes"));

		effectiveScopes = new Span();
		main.addFormItem(effectiveScopes, msg.getMessage("OAuthTokenViewer.effectiveScopes"));

		
		userInfoComponent = new FormLayout();
		userInfoComponent.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addFormItem(userInfoComponent, msg.getMessage("OAuthTokenViewer.userInfo"));

		add(main);
		setPadding(true);
		setSpacing(false);
	}

	private void setIdToken(String value)
	{
		idToken.setReadOnly(false);
		idToken.setValue(value);
		idToken.setReadOnly(true);
	}

	public void setInput(Optional<OAuthTokenBean> tokenBean)
	{
		userInfoComponent.removeAll();
		if (tokenBean.isEmpty())
		{
			setIdToken("");
			token.setValue("");
			audience.setText("");
			redirectUri.setText("");
			maxTokenValidity.setText("");
			requestedScopes.setText("");
			effectiveScopes.setText("");
			setVisible(false);
			return;
		}

		setVisible(true);
		Token rawToken = tokenBean.get().getToken();
		OAuthToken oauthToken = tokenBean.get().getOAuthToken();

		token.setValue(tokenBean.get().getTokenValue());
		if (oauthToken.getOpenidInfo() != null)
		{
			idToken.getParent().get().setVisible(true);
			setIdToken(oauthToken.getOpenidInfo());
		} else
		{
			idToken.getParent().get().setVisible(false);
		}


		audience.setText(String.join(",", oauthToken.getAudience()));
		redirectUri.setText(oauthToken.getRedirectUri());
		Date maxValidity = new Date(
				rawToken.getCreated().getTime() + oauthToken.getMaxExtendedValidity() * 1000);
		maxTokenValidity.setText(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(maxValidity));
		requestedScopes.setText(String.join(", ", oauthToken.getRequestedScope()));
		effectiveScopes.add(new EffectiveScopeComponent(oauthToken, msg));
			
		setTokenCoreInfo(tokenBean.get());
		
		try
		{
			UserInfo userInfoClaimSet = UserInfo.parse(oauthToken.getUserInfo());
			JWTClaimsSet jwtClaimSet = userInfoClaimSet.toJWTClaimsSet();

			for (String name : jwtClaimSet.getClaims().keySet())
			{
				Span infoL = new Span(jwtClaimSet.getClaim(name).toString());
				userInfoComponent.addFormItem(infoL, name + ":");

			}
		} catch (ParseException e)
		{
			// ok, userinfo will be empty
		}
	}

	private void setTokenCoreInfo(OAuthTokenBean oAuthTokenBean)
	{
		Optional<SignedJWT> jwt = oAuthTokenBean.getJWT();
		Optional<JWTClaimsSet> claims = BearerJWTAccessToken.tryParseJWTClaimSet(jwt);
		if (!oAuthTokenBean.isRefreshToken() && claims.isPresent())
		{
			jwtClaimsSet.getParent().get().setVisible(true);
			jwtInfo.getParent().get().setVisible(true);
			JsonNode tree = JsonUtil.parse(claims.get().toString());
			jwtClaimsSet.setValue(JsonUtil.serializeHumanReadable(tree));
			jwtInfo.setText(jwt.get().getHeader().toString());
		} else
		{
			jwtClaimsSet.getParent().get().setVisible(false);
			jwtInfo.getParent().get().setVisible(false);
		}
	}

}
