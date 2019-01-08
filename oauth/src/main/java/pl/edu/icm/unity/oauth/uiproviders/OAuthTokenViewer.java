/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.webui.common.CompactFormLayout;

/**
 * Show Oauth token details
 * @author P.Piernik
 *
 */
public class OAuthTokenViewer extends VerticalLayout
{
	
	private UnityMessageSource msg;
	
	private FormLayout main;
	
	private Label value;
	private TextArea idToken;
	private Label audience;
	private Label redirectUri;
	private Label maxTokenValidity;
	private Label requestedScopes;
	private CompactFormLayout userInfoComponent;
	
	
	public OAuthTokenViewer(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		main = new CompactFormLayout();
		value = new Label();
		value.setCaption(msg.getMessage("OAuthTokenViewer.value"));
				
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

		main.addComponents(value, redirectUri, maxTokenValidity, audience, requestedScopes, idToken,
				userInfoComponent);
		main.setSizeFull();
		addComponent(main);
		setSizeFull();
		setMargin(false);
		setSpacing(false);
	}

	private void setIdToken(String value)
	{
		idToken.setReadOnly(false);
		idToken.setValue(value);
		idToken.setReadOnly(true);
	}

	public void setInput(OAuthToken token, Token raw)
	{
		userInfoComponent.removeAllComponents();
		if (token == null)
		{
			setIdToken("");
			value.setValue("");
			audience.setValue("");
			redirectUri.setValue("");
			maxTokenValidity.setValue("");
			requestedScopes.setValue("");
			
			return;
		}

		if (token.getOpenidInfo() != null)
		{
			idToken.setVisible(true);
			setIdToken(token.getOpenidInfo());

		} else
		{
			idToken.setVisible(false);
		}
		
		value.setValue(raw.getValue());
		audience.setValue(token.getAudience());
		redirectUri.setValue(token.getRedirectUri());
		Date maxValidity = new Date(
				raw.getCreated().getTime() + token.getMaxExtendedValidity() * 1000);
		maxTokenValidity.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
				.format(maxValidity));
		requestedScopes.setValue(String.join(", ", token.getRequestedScope()));

		try
		{
			UserInfo userInfoClaimSet = UserInfo.parse(token.getUserInfo());
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

}


