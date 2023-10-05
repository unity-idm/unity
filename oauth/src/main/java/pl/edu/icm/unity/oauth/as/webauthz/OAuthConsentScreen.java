/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.consent_utils.ExposedAttributesComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.IdPButtonsBar;
import io.imunity.vaadin.endpoint.common.consent_utils.IdentitySelectorComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.SPInfoComponent;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.Prompt;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.webui.idpcommon.URIPresentationHelper;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;

import static pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint.OAUTH_ROUTING_SERVLET_PATH;

/**
 * Consent screen after resource owner login and obtaining set of effective attributes.
 */
class OAuthConsentScreen extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthConsentScreen.class);
	private final MessageSource msg;
	
	private final AttributeHandlerRegistry handlersRegistry;
	private final PreferencesManagement preferencesMan;
	private final VaddinWebLogoutHandler authnProcessor;
	private final OAuthResponseHandler oauthResponseHandler;
	private final IdentityTypeSupport idTypeSupport;
	private final AttributeTypeSupport aTypeSupport; 
	
	private final IdentityParam identity;
	private final Collection<DynamicAttribute> attributes;

	private final Runnable declineHandler;
	private final BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler;
	
	private IdentitySelectorComponent idSelector;
	private ExposedAttributesComponent attrsPresenter;
	private Checkbox rememberCB;
	
	OAuthConsentScreen(MessageSource msg, 
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan,
			VaddinWebLogoutHandler authnProcessor,
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
		setMargin(false);
		setSpacing(false);
		setAlignItems(Alignment.CENTER);
		
		VerticalLayout contents = new VerticalLayout();
		contents.addClassName("u-consentMainColumn");
		contents.setAlignItems(Alignment.CENTER);
		add(contents);

		String oauthRequester = ctx.getClientName();
		if (oauthRequester == null)
			oauthRequester = ctx.getRequest().getClientID().getValue();
		
		createInfoPart(ctx, oauthRequester, contents);

		createExposedDataPart(ctx, oauthRequester, contents, attributes, identity);

		createButtonsPart(contents);

		loadPreferences(ctx);
	}

	private void createInfoPart(OAuthAuthzContext oauthCtx, String oauthRequester, VerticalLayout contents)
	{
		String returnAddress = oauthCtx.getReturnURI().toASCIIString();

		Image clientLogo = null;
		Attribute logoAttr = oauthCtx.getClientLogo();
		if (logoAttr != null && ImageAttributeSyntax.ID.equals(logoAttr.getValueSyntax()))
		{
			ImageAttributeSyntax syntax = (ImageAttributeSyntax) aTypeSupport.getSyntax(logoAttr);
			UnityImage image = syntax.convertFromString(logoAttr.getValues().get(0));
			clientLogo = new Image(new StreamResource(UUID.randomUUID() + "." + image.getType().toExt(), () -> new ByteArrayInputStream(image.getImage())), "");
		}
		SPInfoComponent spInfo = new SPInfoComponent(msg, clientLogo, oauthRequester, returnAddress);
		
		contents.add(spInfo);
	}

	private void createExposedDataPart(OAuthAuthzContext ctx, String oauthRequester, VerticalLayout contents,
			Collection<DynamicAttribute> attributes, IdentityParam identity)
	{
		Div exposedInfoPanel = new Div();
		exposedInfoPanel.setClassName("u-consent-screen");
		contents.add(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setWidthFull();
		exposedInfoPanel.add(eiLayout);

		for (OAuthScope si : ctx.getEffectiveRequestedScopes())
		{
			String label = Strings.isNullOrEmpty(si.description) ? si.name : si.description;
			Span scope = new Span("● " + label);
			eiLayout.add(scope);
		}
		eiLayout.add(new HtmlComponent("br"));

		if (!ctx.getAdditionalAudience().isEmpty())
		{
			eiLayout.add(new AudienceInfoComponent(msg, ctx.getAdditionalAudience(), oauthRequester));
			eiLayout.add(new HtmlComponent("br"));
		}
		
		createIdentityPart(identity);
		attrsPresenter = new ExposedAttributesComponent(msg, idTypeSupport, handlersRegistry, attributes,
				Optional.of(identity));
		eiLayout.add(attrsPresenter);

		rememberCB = new Checkbox(msg.getMessage("OAuthAuthzUI.rememberSettings"));
		contents.add(rememberCB);
		rememberCB.addClassName("u-consent-screen-checkbox");
		rememberCB.setVisible(!(ctx.getClientType() == ClientType.PUBLIC) && !ctx.getPrompts().contains(Prompt.CONSENT));
	}
	
	private void createIdentityPart(IdentityParam validIdentity)
	{
		idSelector = new IdentitySelectorComponent(msg, idTypeSupport, Lists.newArrayList(validIdentity));
	}
	
	private void createButtonsPart(VerticalLayout contents)
	{
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, OAUTH_ROUTING_SERVLET_PATH, action ->
		{
			if (IdPButtonsBar.Action.ACCEPT == action)
				confirm();
			else if (IdPButtonsBar.Action.DENY == action)
				decline();
		});
		contents.add(buttons);
		buttons.setClassName("u-consent-screen-buttons");
		buttons.setAlignItems(Alignment.CENTER);
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
	
	private void updateUIFromPreferences(OAuthClientSettings settings, OAuthAuthzContext ctx)
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
		settings.setAudience(new HashSet<>(ctx.getAdditionalAudience()));
		settings.setTimestamp(Instant.now());
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
	
	
	public static class AudienceInfoComponent extends VerticalLayout
	{
		private final MessageSource msg;
		
		public AudienceInfoComponent(MessageSource msg, List<String> audience, String clientName)
		{
			this.msg = msg;
			init(audience, clientName);
		}

		private void init(List<String> audience, String clientName)
		{
			setMargin(false);
			setPadding(false);
			add(new Span(msg.getMessage("AudienceInfoComponent.infoHeader", clientName)));

			for (String si : audience)
			{
				String label = URIPresentationHelper.getHumanReadableDomain(si);
				Span aud = new Span("● " + label);
				add(aud);
			}
		}
	}
}
