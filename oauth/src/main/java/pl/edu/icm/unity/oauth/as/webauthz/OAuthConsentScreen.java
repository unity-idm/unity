/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.ext.JpegImageAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.idpcommon.ExposedAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar.Action;
import pl.edu.icm.unity.webui.idpcommon.IdentitySelectorComponent;
import pl.edu.icm.unity.webui.idpcommon.SPInfoComponent;

/**
 * Consent screen after resource owner login and obtaining set of effective attributes.
 * 
 * @author K. Benedyczak
 */
class OAuthConsentScreen extends CustomComponent 
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthConsentScreen.class);
	private UnityMessageSource msg;
	
	private AttributeHandlerRegistry handlersRegistry;
	private PreferencesManagement preferencesMan;
	private StandardWebAuthenticationProcessor authnProcessor;
	private SessionManagement sessionMan;
	private OAuthResponseHandler oauthResponseHandler;
	private IdentityTypeSupport idTypeSupport;
	private AttributeTypeSupport aTypeSupport; 
	
	private IdentitySelectorComponent idSelector;
	private ExposedAttributesComponent attrsPresenter;
	private CheckBox rememberCB;
	private IdentityParam identity;
	private Collection<DynamicAttribute> attributes;
	
	private Runnable declineHandler;
	private BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler; 
	
	OAuthConsentScreen(UnityMessageSource msg, 
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan,
			StandardWebAuthenticationProcessor authnProcessor, 
			IdentityTypeSupport idTypeSupport, 
			AttributeTypeSupport aTypeSupport,
			SessionManagement sessionMan,
			IdentityParam identity,
			Collection<DynamicAttribute> attributes,
			Runnable declineHandler,
			BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler)
	{
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.sessionMan = sessionMan;
		this.identity = identity;
		this.attributes = attributes;
		this.idTypeSupport = idTypeSupport;
		this.aTypeSupport = aTypeSupport;
		this.declineHandler = declineHandler;
		this.acceptHandler = acceptHandler;
		initUI();
	}

	private void initUI()
	{
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		oauthResponseHandler = new OAuthResponseHandler(sessionMan);
		
		VerticalLayout vmain = new VerticalLayout();
		vmain.setMargin(false);
		vmain.setSpacing(false);
		
		VerticalLayout contents = new VerticalLayout();
		contents.addStyleName(Styles.maxWidthColumn.toString());
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.TOP_CENTER);
		
		
		createInfoPart(ctx, contents);

		createExposedDataPart(ctx, contents, attributes, identity);

		createButtonsPart(contents);

		setCompositionRoot(vmain);

		loadPreferences(ctx);
	}

	private void createInfoPart(OAuthAuthzContext oauthCtx, VerticalLayout contents)
	{
		String oauthRequester = oauthCtx.getClientName();
		if (oauthRequester == null)
			oauthRequester = oauthCtx.getRequest().getClientID().getValue();
		String returnAddress = oauthCtx.getReturnURI().toASCIIString();

		Resource clientLogo = null;
		Attribute logoAttr = oauthCtx.getClientLogo();
		if (logoAttr != null && JpegImageAttributeSyntax.ID.equals(logoAttr.getValueSyntax()))
		{
			JpegImageAttributeSyntax syntax = (JpegImageAttributeSyntax) aTypeSupport.getSyntax(logoAttr);
			BufferedImage image = syntax.convertFromString(logoAttr.getValues().get(0));
			clientLogo = new JpegImageAttributeHandler.SimpleImageSource(
					image, 
					syntax, "jpg").getResource();
		}
		Label info1 = new Label100(msg.getMessage("OAuthAuthzUI.info1"));
		info1.addStyleName(Styles.vLabelH1.toString());
		
		SPInfoComponent spInfo = new SPInfoComponent(msg, clientLogo, oauthRequester, returnAddress);
		
		Label spc1 = HtmlTag.br();
		Label info2 = new Label100(msg.getMessage("OAuthAuthzUI.info2"));
		
		contents.addComponents(info1, spInfo, spc1, info2);
	}

	private void createExposedDataPart(OAuthAuthzContext ctx, VerticalLayout contents,
			Collection<DynamicAttribute> attributes, IdentityParam identity)
	{
		SafePanel exposedInfoPanel = new SafePanel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);

		for (ScopeInfo si: ctx.getEffectiveRequestedScopes())
		{
			Label scope = new Label100(si.getName());
			Label scopeDesc = new Label100(si.getDescription());
			scopeDesc.addStyleName(Styles.vLabelSmall.toString());
			eiLayout.addComponents(scope, scopeDesc);
		}
		Label spacer = HtmlTag.br();
		spacer.addStyleName(Styles.vLabelSmall.toString());
		eiLayout.addComponent(spacer);

		createIdentityPart(identity, eiLayout);
		attrsPresenter = new ExposedAttributesComponent(msg, handlersRegistry, attributes);
		eiLayout.addComponent(attrsPresenter);
		
		rememberCB = new CheckBox(msg.getMessage("OAuthAuthzUI.rememberSettings"));
		contents.addComponent(rememberCB);
		
		if (ctx.getClientType() == ClientType.PUBLIC)
			rememberCB.setVisible(false);
	}
	
	private void createIdentityPart(IdentityParam validIdentity, VerticalLayout contents)
	{
		idSelector = new IdentitySelectorComponent(msg, idTypeSupport, Lists.newArrayList(validIdentity));
		contents.addComponent(idSelector);
	}
	
	private void createButtonsPart(VerticalLayout contents)
	{
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, action ->
		{
			if (Action.ACCEPT == action)
				confirm();
			else if (Action.DENY == action)
				decline();
		});
		contents.addComponent(buttons);
		contents.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
	}

	private void loadPreferences(OAuthAuthzContext ctx)
	{
		try
		{
			OAuthPreferences preferences = OAuthPreferences.getPreferences(preferencesMan);
			OAuthClientSettings settings = preferences.getSPSettings(ctx.getRequest().getClientID().getValue());
			updateUIFromPreferences(settings, ctx);
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, true);
		}
	}
	
	private void updateUIFromPreferences(OAuthClientSettings settings, OAuthAuthzContext ctx) throws EngineException
	{
		if (settings == null)
			return;
		
		String selId = settings.getSelectedIdentity();
		idSelector.setSelected(selId);
		
		if (settings.isDoNotAsk() && ctx.getClientType() != ClientType.PUBLIC)
		{
			setCompositionRoot(new VerticalLayout());
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
	
	private void decline()
	{
		storePreferences(false);
		declineHandler.run();
	}
	
	private void confirm()
	{
		storePreferences(true);
		Collection<DynamicAttribute> attributes = attrsPresenter.getUserFilteredAttributes();
		IdentityParam identity = idSelector.getSelectedIdentity();
		acceptHandler.accept(identity, attributes);
	}
}
