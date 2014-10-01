/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.SPSettings;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.ext.JpegImageAttributeHandler;
import pl.edu.icm.unity.webui.common.provider.ExposedAttributesComponent;
import pl.edu.icm.unity.webui.common.provider.IdPButtonsBar;
import pl.edu.icm.unity.webui.common.provider.IdPButtonsBar.Action;
import pl.edu.icm.unity.webui.common.provider.IdentitySelectorComponent;
import pl.edu.icm.unity.webui.common.provider.SPInfoComponent;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * UI of the authorization endpoint, responsible for getting consent after resource owner login.
 * @author K. Benedyczak
 */
@Component("OAuthAuthzUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class OAuthAuthzUI extends UnityUIBase 
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthAuthzUI.class);
	private UnityMessageSource msg;
	private EndpointDescription endpointDescription;
	private IdPEngine idpEngine;
	private AttributeHandlerRegistry handlersRegistry;
	private PreferencesManagement preferencesMan;
	private AuthenticationProcessor authnProcessor;
	
	private IdentitySelectorComponent idSelector;
	private ExposedAttributesComponent attrsPresenter;
	private OAuthResponseHandler oauthResponseHandler;
	private CheckBox rememberCB;
	
	@Autowired
	public OAuthAuthzUI(UnityMessageSource msg, 
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan,
			AuthenticationProcessor authnProcessor, IdPEngine idpEngine)
	{
		super(msg);
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.idpEngine = idpEngine;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration)
	{
		this.endpointDescription = description;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		OAuthAuthzContext ctx = OAuthResponseHandler.getContext();
		oauthResponseHandler = new OAuthResponseHandler();
		
		VerticalLayout vmain = new VerticalLayout();
		TopHeaderLight header = new TopHeaderLight(endpointDescription.getId(), msg);
		vmain.addComponent(header);

		
		VerticalLayout contents = new VerticalLayout();
		contents.setSizeUndefined();
		contents.setMargin(true);
		contents.setSpacing(true);
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.TOP_CENTER);
		
		try
		{
			createInfoPart(ctx, contents);

			createExposedDataPart(ctx, contents);

			createButtonsPart(contents);

			setContent(vmain);

			loadPreferences(ctx);
		} catch (EopException e)
		{
			//OK
		}

	}

	private void createInfoPart(OAuthAuthzContext oauthCtx, VerticalLayout contents)
	{
		String oauthRequester = oauthCtx.getClientName();
		if (oauthRequester == null)
			oauthRequester = oauthCtx.getRequest().getClientID().getValue();
		String returnAddress = oauthCtx.getReturnURI().toASCIIString();

		Resource clientLogo = null;
		Attribute<BufferedImage> logoAttr = oauthCtx.getClientLogo();
		if (oauthCtx.getClientLogo()  != null)
			clientLogo = new JpegImageAttributeHandler.SimpleImageSource(
					logoAttr.getValues().get(0), 
					logoAttr.getAttributeSyntax(), "jpg").getResource();
		
		Label info1 = new Label(msg.getMessage("OAuthAuthzUI.info1"));
		info1.setStyleName(Reindeer.LABEL_H1);
		
		SPInfoComponent spInfo = new SPInfoComponent(msg, clientLogo, oauthRequester, returnAddress);
		
		Label spc1 = new Label("<br>", ContentMode.HTML);
		Label info2 = new Label(msg.getMessage("OAuthAuthzUI.info2"));
		
		contents.addComponents(info1, spInfo, spc1, info2);
	}

	private void createExposedDataPart(OAuthAuthzContext ctx, VerticalLayout contents) throws EopException
	{
		Panel exposedInfoPanel = new Panel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			//TODO present requested scopes properly
			Label scopes = new Label("Scopes: " + ctx.getRequest().getScope().toString());
			contents.addComponent(scopes);
			
			
			TranslationResult translationResult = getUserInfo(ctx);
			attrsPresenter = new ExposedAttributesComponent(msg, handlersRegistry, 
					translationResult.getAttributes(), true);
			contents.addComponent(attrsPresenter);
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState());
			oauthResponseHandler.returnOauthResponse(oauthResponse, true);
			return;
		}
		
		rememberCB = new CheckBox(msg.getMessage("OAuthAuthzUI.rememberSettings"));
		contents.addComponent(rememberCB);
	}

	private void createButtonsPart(VerticalLayout contents)
	{
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, new IdPButtonsBar.ActionListener()
		{
			@Override
			public void buttonClicked(Action action)
			{
				try
				{
					if (Action.ACCEPT == action)
						confirm();
					else if (Action.DENY == action)
						decline();
				} catch (EopException e) 
				{
					//OK
				}
			}
		});
		
		contents.addComponent(buttons);
		contents.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
	}

	
	private TranslationResult getUserInfo(OAuthAuthzContext ctx) 
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		String flow = ctx.getRequest().getResponseType().impliesCodeFlow() ? 
				GrantFlow.authorizationCode.toString() : GrantFlow.implicit.toString();
		return idpEngine.obtainUserInformation(new EntityParam(ae.getEntityId()), 
				ctx.getUsersGroup(), 
				ctx.getTranslationProfile(), 
				ctx.getRequest().getClientID().getValue(),
				"OAuth2", 
				flow,
				true);
	}
	
	private void loadPreferences(OAuthAuthzContext ctx) throws EopException
	{
		try
		{
			OAuthPreferences preferences = OAuthPreferences.getPreferences(preferencesMan);
			SPSettings settings = preferences.getSPSettings(ctx.getRequest().getClientID().getValue());
			updateUIFromPreferences(settings);
		} catch (EopException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState());
			oauthResponseHandler.returnOauthResponse(oauthResponse, true);
			return;
		}
	}
	
	private void updateUIFromPreferences(SPSettings settings) throws EngineException, EopException
	{
		if (settings == null)
			return;

		if (settings.isDoNotAsk())
		{
			if (settings.isDefaultAccept())
				confirm();
			else
				decline();
		}
	}
	
	/**
	 * Applies UI selected values to the given preferences object
	 * @param preferences
	 * @param ctx
	 * @param defaultAccept
	 * @throws EngineException
	 */
	private void updatePreferencesFromUI(OAuthPreferences preferences, OAuthAuthzContext ctx, boolean defaultAccept) 
			throws EngineException
	{
		if (!rememberCB.getValue())
			return;
		String reqIssuer = ctx.getRequest().getClientID().getValue();
		SPSettings settings = preferences.getSPSettings(reqIssuer);
		settings.setDefaultAccept(defaultAccept);
		settings.setDoNotAsk(true);
		preferences.setSPSettings(reqIssuer, settings);
	}
	
	private void storePreferences(boolean defaultAccept)
	{
		try
		{
			OAuthAuthzContext ctx = OAuthResponseHandler.getContext();
			OAuthPreferences preferences = OAuthPreferences.getPreferences(preferencesMan);
			updatePreferencesFromUI(preferences, ctx, defaultAccept);
			OAuthPreferences.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}
	
	protected void decline() throws EopException
	{
		OAuthAuthzContext ctx = OAuthResponseHandler.getContext();
		storePreferences(false);
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState());
		oauthResponseHandler.returnOauthResponse(oauthResponse, false);
	}
	
	protected void confirm() throws EopException
	{
		storePreferences(true);
		OAuthAuthzContext ctx = OAuthResponseHandler.getContext();
		try
		{
			Collection<Attribute<?>> attributes = attrsPresenter.getUserFilteredAttributes();
			//TODO proper token creation, proper response.
			AuthorizationSuccessResponse oauthResponse = null;
			ResponseType respType = ctx.getRequest().getResponseType(); 
			if (respType.impliesCodeFlow())
			{
				AuthorizationCode authzCode = new AuthorizationCode();
				oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), authzCode, 
						ctx.getRequest().getState());
			} else if (respType.impliesImplicitFlow())
			{
//				AccessToken accessToken = new AccessTo
//				oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), accessToken, 
//						ctx.getRequest().getState());
			}
			oauthResponseHandler.returnOauthResponse(oauthResponse, false);
		} catch (Exception e)
		{
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState());
			oauthResponseHandler.returnOauthResponse(oauthResponse, false);
		}
	}
}
