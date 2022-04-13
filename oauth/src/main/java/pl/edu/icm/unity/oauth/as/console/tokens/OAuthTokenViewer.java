/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
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
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BearerJWTAccessToken;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

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
	private Label jwtInfo;
	private TextArea idToken;
	private Label audience;
	private Label redirectUri;
	private Label maxTokenValidity;
	private Label requestedScopes;
	private CompactFormLayout userInfoComponent;

	OAuthTokenViewer(MessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		main = FormLayoutWithFixedCaptionWidth.withShortCaptions();

		token = new TextArea();
		token.setCaption(msg.getMessage("OAuthTokenViewer.token"));
		token.setSizeFull();
		token.setReadOnly(true);

		jwtClaimsSet = new TextArea();
		jwtClaimsSet.setCaption(msg.getMessage("OAuthTokenViewer.jwtClaims"));
		jwtClaimsSet.setSizeFull();
		jwtClaimsSet.setReadOnly(true);
		jwtInfo = new Label();
		jwtInfo.setCaption(msg.getMessage("OAuthTokenViewer.jwtInfo"));
		
		
		idToken = new TextArea();
		idToken.setCaption(msg.getMessage("OAuthTokenViewer.idtoken"));
		idToken.setSizeFull();
		idToken.setReadOnly(true);
		audience = new Label();
		audience.setCaption(msg.getMessage("OAuthTokenViewer.audience"));
		redirectUri = new Label();
		redirectUri.setCaption(msg.getMessage("OAuthTokenViewer.redirectUri"));
		maxTokenValidity = new Label();
		maxTokenValidity.setCaption(msg.getMessage("OAuthTokenViewer.maxTokenValidity"));
		requestedScopes = new Label();
		requestedScopes.setCaption(msg.getMessage("OAuthTokenViewer.requestedScopes"));

		userInfoComponent = new CompactFormLayout();
		userInfoComponent.setCaption(msg.getMessage("OAuthTokenViewer.userInfo"));
		userInfoComponent.setMargin(false);
		userInfoComponent.setSpacing(false);

		main.addComponents(token, jwtClaimsSet, jwtInfo, redirectUri, maxTokenValidity, audience, 
				requestedScopes, idToken, userInfoComponent);
		addComponent(main);
		setMargin(false);
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
		userInfoComponent.removeAllComponents();
		if (!tokenBean.isPresent())
		{
			setIdToken("");
			token.setValue("");
			audience.setValue("");
			redirectUri.setValue("");
			maxTokenValidity.setValue("");
			requestedScopes.setValue("");
			setVisible(false);
			return;
		}

		setVisible(true);
		Token rawToken = tokenBean.get().getToken();
		OAuthToken oauthToken = tokenBean.get().getOAuthToken();

		token.setValue(tokenBean.get().getTokenValue());
		token.setRows(tokenBean.get().getTokenValue().length() > 150 ? 5 : 1);
		if (oauthToken.getOpenidInfo() != null)
		{
			idToken.setVisible(true);
			setIdToken(oauthToken.getOpenidInfo());
		} else
		{
			idToken.setVisible(false);
		}


		audience.setValue(String.join(",", oauthToken.getAudience()));
		redirectUri.setValue(oauthToken.getRedirectUri());
		Date maxValidity = new Date(
				rawToken.getCreated().getTime() + oauthToken.getMaxExtendedValidity() * 1000);
		maxTokenValidity.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(maxValidity));
		requestedScopes.setValue(String.join(", ", oauthToken.getRequestedScope()));

		setTokenCoreInfo(tokenBean.get());
		
		try
		{
			UserInfo userInfoClaimSet = UserInfo.parse(oauthToken.getUserInfo());
			JWTClaimsSet jwtClaimSet = userInfoClaimSet.toJWTClaimsSet();

			for (String name : jwtClaimSet.getClaims().keySet())
			{
				Label infoL = new Label();
				infoL.setCaption(name + ":");
				infoL.setValue(jwtClaimSet.getClaim(name).toString());
				userInfoComponent.addComponent(infoL);

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
			jwtClaimsSet.setVisible(true);
			jwtInfo.setVisible(true);
			JsonNode tree = JsonUtil.parse(claims.get().toString());
			jwtClaimsSet.setValue(JsonUtil.serializeHumanReadable(tree));
			jwtInfo.setValue(jwt.get().getHeader().toString());
		} else
		{
			jwtClaimsSet.setVisible(false);
			jwtInfo.setVisible(false);
		}
	}

}
