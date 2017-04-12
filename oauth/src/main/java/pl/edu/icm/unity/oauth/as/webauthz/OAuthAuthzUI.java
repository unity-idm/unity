/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.awt.image.BufferedImage;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.ext.JpegImageAttributeHandler;
import pl.edu.icm.unity.webui.common.provider.ExposedAttributesComponent;
import pl.edu.icm.unity.webui.common.provider.IdPButtonsBar;
import pl.edu.icm.unity.webui.common.provider.IdPButtonsBar.Action;
import pl.edu.icm.unity.webui.common.provider.IdentitySelectorComponent;
import pl.edu.icm.unity.webui.common.provider.SPInfoComponent;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * UI of the authorization endpoint, responsible for getting consent after resource owner login.
 * @author K. Benedyczak
 */
@Component("OAuthAuthzUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class OAuthAuthzUI extends UnityEndpointUIBase 
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthAuthzUI.class);
	private UnityMessageSource msg;
	private TokensManagement tokensMan;
	
	private OAuthIdPEngine idpEngine;
	private AttributeHandlerRegistry handlersRegistry;
	private AttributesManagement attrMan;
	private PreferencesManagement preferencesMan;
	private WebAuthenticationProcessor authnProcessor;
	private IdentityTypesRegistry identityTypesRegistry;
	
	private IdentitySelectorComponent idSelector;
	private ExposedAttributesComponent attrsPresenter;
	private OAuthResponseHandler oauthResponseHandler;
	private CheckBox rememberCB;
	private OAuthProcessor oauthProcessor;
	private SessionManagement sessionMan; 
	
	@Autowired
	public OAuthAuthzUI(UnityMessageSource msg, TokensManagement tokensMan,
			AttributeHandlerRegistry handlersRegistry, AttributesManagement attrMan,
			PreferencesManagement preferencesMan,
			WebAuthenticationProcessor authnProcessor, IdPEngine idpEngine,
			IdentityTypesRegistry identityTypesRegistry,
			EnquiresDialogLauncher enquiryDialogLauncher, SessionManagement sessionMan)
	{
		super(msg, enquiryDialogLauncher);
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.attrMan = attrMan;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.sessionMan = sessionMan;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.tokensMan = tokensMan;
		this.identityTypesRegistry = identityTypesRegistry;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		oauthResponseHandler = new OAuthResponseHandler(sessionMan);
		oauthProcessor = new OAuthProcessor();
		
		VerticalLayout vmain = new VerticalLayout();
		TopHeaderLight header = new TopHeaderLight(endpointDescription.getDisplayedName().getValue(msg), msg);
		vmain.addComponent(header);

		
		VerticalLayout contents = new VerticalLayout();
		contents.addStyleName(Styles.maxWidthColumn.toString());
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
		info1.addStyleName(Styles.vLabelH1.toString());
		
		SPInfoComponent spInfo = new SPInfoComponent(msg, clientLogo, oauthRequester, returnAddress);
		
		Label spc1 = HtmlTag.br();
		Label info2 = new Label(msg.getMessage("OAuthAuthzUI.info2"));
		
		contents.addComponents(info1, spInfo, spc1, info2);
	}

	private void createExposedDataPart(OAuthAuthzContext ctx, VerticalLayout contents) throws EopException
	{
		SafePanel exposedInfoPanel = new SafePanel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			for (ScopeInfo si: ctx.getEffectiveRequestedScopes())
			{
				Label scope = new Label(si.getName());
				Label scopeDesc = new Label(si.getDescription());
				scopeDesc.addStyleName(Styles.vLabelSmall.toString());
				eiLayout.addComponents(scope, scopeDesc);
			}
			Label spacer = HtmlTag.br();
			spacer.addStyleName(Styles.vLabelSmall.toString());
			eiLayout.addComponent(spacer);
			
			TranslationResult translationResult = idpEngine.getUserInfo(ctx);
			
			createIdentityPart(translationResult, eiLayout, ctx.getConfig().getSubjectIdentityType());
			
			attrsPresenter = new ExposedAttributesComponent(msg, handlersRegistry, attrMan,
					oauthProcessor.filterAttributes(translationResult, ctx.getRequestedAttrs()));
			eiLayout.addComponent(attrsPresenter);
		} catch (OAuthErrorResponseException e)
		{
			oauthResponseHandler.returnOauthResponse(e.getOauthResponse(), e.isInvalidateSession());
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user 
			//if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponse(oauthResponse, true);
		}
		
		rememberCB = new CheckBox(msg.getMessage("OAuthAuthzUI.rememberSettings"));
		contents.addComponent(rememberCB);
	}
	
	private void createIdentityPart(TranslationResult translationResult, VerticalLayout contents,
			String subjectIdentityType) 
			throws EngineException
	{
		IdentityParam validIdentity = idpEngine.getIdentity(translationResult, subjectIdentityType);
		idSelector = new IdentitySelectorComponent(msg, identityTypesRegistry, 
				Lists.newArrayList(validIdentity));
		contents.addComponent(idSelector);
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

	private void loadPreferences(OAuthAuthzContext ctx) throws EopException
	{
		try
		{
			OAuthPreferences preferences = OAuthPreferences.getPreferences(preferencesMan);
			OAuthClientSettings settings = preferences.getSPSettings(ctx.getRequest().getClientID().getValue());
			updateUIFromPreferences(settings);
		} catch (EopException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponse(oauthResponse, true);
		}
	}
	
	private void updateUIFromPreferences(OAuthClientSettings settings) throws EngineException, EopException
	{
		if (settings == null)
			return;
		
		String selId = settings.getSelectedIdentity();
		idSelector.setSelected(selId);
		
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
		OAuthClientSettings settings = preferences.getSPSettings(reqIssuer);
		settings.setDefaultAccept(defaultAccept);
		settings.setDoNotAsk(true);
		String identityValue = idSelector.getSelectedIdentityForPreferences();
		if (identityValue != null)
			settings.setSelectedIdentity(identityValue);
		preferences.setSPSettings(reqIssuer, settings);
	}
	
	private void storePreferences(boolean defaultAccept)
	{
		try
		{
			OAuthAuthzContext ctx = OAuthContextUtils.getContext();
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
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		storePreferences(false);
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(),
				ctx.getRequest().impliedResponseMode());
		oauthResponseHandler.returnOauthResponse(oauthResponse, false);
	}
	
	protected void confirm() throws EopException
	{
		storePreferences(true);
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		try
		{
			Collection<DynamicAttribute> attributes = attrsPresenter.getUserFilteredAttributes();
			IdentityParam identity = idSelector.getSelectedIdentity();
			
			AuthorizationSuccessResponse oauthResponse = oauthProcessor.
					prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, tokensMan);
			
			
			oauthResponseHandler.returnOauthResponse(oauthResponse, false);
		} catch (EopException eop)
		{
			throw eop;
		} catch (Exception e)
		{
			log.error("Error during OAuth processing", e);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponse(oauthResponse, false);
		}
	}
}
