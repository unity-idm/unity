/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
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

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.Prompt;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.image.SimpleImageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.idpcommon.ExposedAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar.Action;
import pl.edu.icm.unity.webui.idpcommon.IdentitySelectorComponent;
import pl.edu.icm.unity.webui.idpcommon.SPInfoComponent;
import pl.edu.icm.unity.webui.idpcommon.URIPresentationHelper;
/**
 * Consent screen after resource owner login and obtaining set of effective attributes.
 * 
 * @author K. Benedyczak
 */
class OAuthConsentScreen extends CustomComponent 
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthConsentScreen.class);
	private MessageSource msg;
	
	private final AttributeHandlerRegistry handlersRegistry;
	private final PreferencesManagement preferencesMan;
	private final StandardWebLogoutHandler authnProcessor;
	private final OAuthResponseHandler oauthResponseHandler;
	private final IdentityTypeSupport idTypeSupport;
	private final AttributeTypeSupport aTypeSupport; 
	
	private final IdentityParam identity;
	private final Collection<DynamicAttribute> attributes;

	private final Runnable declineHandler;
	private final BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler;
	
	private IdentitySelectorComponent idSelector;
	private ExposedAttributesComponent attrsPresenter;
	private CheckBox rememberCB;
	
	OAuthConsentScreen(MessageSource msg, 
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan,
			StandardWebLogoutHandler authnProcessor, 
			IdentityTypeSupport idTypeSupport, 
			AttributeTypeSupport aTypeSupport,
			IdentityParam identity,
			Collection<DynamicAttribute> attributes,
			Runnable declineHandler,
			BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler,
			OAuthResponseHandler oAuthResponseHandler)
	{
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.identity = identity;
		this.attributes = attributes;
		this.idTypeSupport = idTypeSupport;
		this.aTypeSupport = aTypeSupport;
		this.declineHandler = declineHandler;
		this.acceptHandler = acceptHandler;
		this.oauthResponseHandler = oAuthResponseHandler;
		initUI();
	}

	private void initUI()
	{
		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		
		VerticalLayout vmain = new VerticalLayout();
		vmain.setMargin(false);
		vmain.setSpacing(false);
		
		VerticalLayout contents = new VerticalLayout();
		contents.addStyleName("u-consentMainColumn");
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.MIDDLE_CENTER);
		
		String oauthRequester = ctx.getClientName();
		if (oauthRequester == null)
			oauthRequester = ctx.getRequest().getClientID().getValue();
		
		createInfoPart(ctx, oauthRequester, contents);

		createExposedDataPart(ctx, oauthRequester, contents, attributes, identity);

		createButtonsPart(contents);

		setCompositionRoot(vmain);

		loadPreferences(ctx);
	}

	private void createInfoPart(OAuthAuthzContext oauthCtx, String oauthRequester, VerticalLayout contents)
	{
		String returnAddress = oauthCtx.getReturnURI().toASCIIString();

		Resource clientLogo = null;
		Attribute logoAttr = oauthCtx.getClientLogo();
		if (logoAttr != null && ImageAttributeSyntax.ID.equals(logoAttr.getValueSyntax()))
		{
			ImageAttributeSyntax syntax = (ImageAttributeSyntax) aTypeSupport.getSyntax(logoAttr);
			UnityImage image = syntax.convertFromString(logoAttr.getValues().get(0));
			clientLogo = new SimpleImageSource(image).getResource();
		}
		SPInfoComponent spInfo = new SPInfoComponent(msg, clientLogo, oauthRequester, returnAddress);
		
		contents.addComponents(spInfo);
	}

	private void createExposedDataPart(OAuthAuthzContext ctx, String oauthRequester, VerticalLayout contents,
			Collection<DynamicAttribute> attributes, IdentityParam identity)
	{
		SafePanel exposedInfoPanel = new SafePanel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);

		for (OAuthScope si : ctx.getEffectiveRequestedScopes())
		{
			
			String label = Strings.isNullOrEmpty(si.description) ? si.name : si.description;
			Label scope = new Label100("\u25CF " + label);
			eiLayout.addComponents(scope);
		}
		eiLayout.addComponent(getSpacer());
		
		if (!ctx.getAdditionalAudience().isEmpty())
		{
			eiLayout.addComponent(new AudienceInfoComponent(msg, ctx.getAdditionalAudience(), oauthRequester));
			eiLayout.addComponent(getSpacer());			
		}
		
		createIdentityPart(identity, eiLayout);
		attrsPresenter = new ExposedAttributesComponent(msg, idTypeSupport, handlersRegistry, attributes,
				Optional.of(identity));
		eiLayout.addComponent(attrsPresenter);

		rememberCB = new CheckBox(msg.getMessage("OAuthAuthzUI.rememberSettings"));
		contents.addComponent(rememberCB);
		rememberCB.setVisible(!(ctx.getClientType() == ClientType.PUBLIC) && !ctx.getPrompts().contains(Prompt.CONSENT));
	}
	
	private Label getSpacer()
	{
		Label spacer = HtmlTag.br();
		spacer.addStyleName(Styles.vLabelSmall.toString());
		return spacer;
	}
	
	private void createIdentityPart(IdentityParam validIdentity, VerticalLayout contents)
	{
		idSelector = new IdentitySelectorComponent(msg, idTypeSupport, Lists.newArrayList(validIdentity));
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
		contents.setComponentAlignment(buttons, Alignment.BOTTOM_RIGHT);
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
			oauthResponseHandler.returnOauthResponseNotThrowingAndReportStatistic(oauthResponse, true, ctx, Status.FAILED);
		}
	}
	
	private void updateUIFromPreferences(OAuthClientSettings settings, OAuthAuthzContext ctx) throws EngineException
	{
		if (settings == null)
			return;
		
		String selId = settings.getSelectedIdentity();
		idSelector.setSelected(selId);
		
		if (settings.isDoNotAsk() && ctx.getClientType() != ClientType.PUBLIC
				&& settings.getEffectiveRequestedScopes()
						.containsAll(Arrays.asList(ctx.getEffectiveRequestedScopesList()))
				&& settings.getAudience().containsAll(ctx.getAdditionalAudience()) && !ctx.getPrompts().contains(Prompt.CONSENT))
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
		settings.setEffectiveRequestedScopes(new HashSet<>(Arrays.asList((ctx.getEffectiveRequestedScopesList()))));
		String identityValue = idSelector.getSelectedIdentityForPreferences();
		if (identityValue != null)
			settings.setSelectedIdentity(identityValue);
		settings.setAudience(ctx.getAdditionalAudience().stream().collect(Collectors.toSet()));
		preferences.setSPSettings(reqIssuer, settings);
		
	}
	
	private void storePreferences(boolean defaultAccept)
	{
		try
		{
			OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
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
	
	
	public static class AudienceInfoComponent extends CustomComponent
	{
		private final MessageSource msg;
		
		public AudienceInfoComponent(MessageSource msg, List<String> audience, String clientName)
		{
			this.msg = msg;
			init(audience, clientName);
		}

		private void init(List<String> audience, String clientName)
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.addComponent(new Label(msg.getMessage("AudienceInfoComponent.infoHeader", clientName)));

			for (String si : audience)
			{
				String label = URIPresentationHelper.getHumanReadableDomain(si);
				Label aud = new Label100("\u25CF " + label);
				main.addComponent(aud);
			}
			setCompositionRoot(main);
		}
	}
}
