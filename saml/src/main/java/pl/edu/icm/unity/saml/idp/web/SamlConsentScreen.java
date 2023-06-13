/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import com.google.common.base.Strings;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.consent_utils.ExposedSelectableAttributesComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.IdPButtonsBar;
import io.imunity.vaadin.endpoint.common.consent_utils.IdentitySelectorComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.SPInfoComponent;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.webui.authn.WebLogoutHandler;
import pl.edu.icm.unity.webui.idpcommon.SelectableAttributesComponent;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint.SAML_ENTRY_SERVLET_PATH;

/**
 * Consent screen of the SAML web IdP. Fairly simple: shows who asks, what is going to be sent,
 * and optionally allows for some customization.
 */
@CssImport("./styles/consent-screen.css")
class SamlConsentScreen extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlConsentScreen.class);
	protected final MessageSource msg;
	protected final AttributeHandlerRegistry handlersRegistry;
	protected final IdentityTypeSupport identityTypeSupport;
	protected final PreferencesManagement preferencesMan;
	protected final WebLogoutHandler authnProcessor;
	protected final AttributeTypeSupport aTypeSupport;
	protected final VaadinLogoImageLoader imageAccessService;

	protected final List<IdentityParam> validIdentities;
	protected final Collection<DynamicAttribute> attributes;
	protected final Map<String, AttributeType> attributeTypes;
	
	protected final Runnable declineHandler;
	protected final ConfirmationConsumer acceptHandler;
	
	protected IdentitySelectorComponent idSelector;
	protected SelectableAttributesComponent attrsPresenter;
	protected SamlResponseHandler samlResponseHandler;
	protected Checkbox rememberCB;

	public SamlConsentScreen(MessageSource msg, VaadinLogoImageLoader imageAccessService,
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan,
			WebLogoutHandler authnProcessor,
			IdentityTypeSupport identityTypeSupport, 
			AttributeTypeSupport aTypeSupport,
			List<IdentityParam> validIdentities,
			Collection<DynamicAttribute> attributes,
			Map<String, AttributeType> attributeTypes,
			Runnable declineHandler,
			ConfirmationConsumer acceptHandler)
	{
		this.msg = msg;
		this.imageAccessService = imageAccessService;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.identityTypeSupport = identityTypeSupport;
		this.aTypeSupport = aTypeSupport;
		this.validIdentities = validIdentities;
		this.attributes = attributes;
		this.attributeTypes = attributeTypes;
		this.declineHandler = declineHandler;
		this.acceptHandler = acceptHandler;
		initUI();
	}

	protected void initUI()
	{
		SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext();
		
		VerticalLayout contents = new VerticalLayout();
		contents.addClassName("u-consentMainColumn");

		createInfoPart(samlCtx, contents);

		Component exposedInfoPanel = createExposedDataPart(samlCtx);
		contents.add(exposedInfoPanel);
		createRememberMeCheckbox(contents);
		
		createButtonsPart(samlCtx, contents);

		add(contents);
		setAlignItems(Alignment.CENTER);

		loadPreferences(samlCtx);
	}

	private void createInfoPart(SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		AuthnRequestType request = samlCtx.getRequest();

		String samlRequester = request.getIssuer().getStringValue();
		String returnAddress = samlCtx.getSamlConfiguration().getReturnAddressForRequester(request);
		String displayedName = samlCtx.getSamlConfiguration().getDisplayedNameForRequester(request.getIssuer(), msg);
		Image logo = samlCtx.getSamlConfiguration().getLogoForRequesterOrNull(request.getIssuer(), msg,
				imageAccessService).orElse(null);

		SPInfoComponent spInfo = new SPInfoComponent(msg, logo,
				Strings.isNullOrEmpty(displayedName) ? samlRequester : displayedName,
				returnAddress);

		contents.add(spInfo);
	}

	protected Component createExposedDataPart(SAMLAuthnContext samlCtx)
	{
		Div exposedInfoPanel = new Div();
		exposedInfoPanel.setClassName("u-consent-screen");

		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setWidthFull();
		exposedInfoPanel.add(eiLayout);
		idSelector = new IdentitySelectorComponent(msg, identityTypeSupport, validIdentities);

		Label info1 = new Label(msg.getMessage("SamlIdPWebUI.allowForSignInInfo"));
		Label info2 = new Label(msg.getMessage("SamlIdPWebUI.allowForReadingUserProfile"));
		eiLayout.add(info1, info2);
		
		if (validIdentities.size() > 1)
			eiLayout.add(idSelector);
		
		eiLayout.add(new HtmlComponent("br"));
		
		boolean userCanEditConsent = samlCtx.getSamlConfiguration().userCanEditConsent;
		Optional<IdentityParam> selectedIdentity = Optional.ofNullable(validIdentities.size() == 1 ? validIdentities.get(0) : null); 
		attrsPresenter = userCanEditConsent ? 
				new ExposedSelectableAttributesComponent(msg, identityTypeSupport, handlersRegistry,
						attributeTypes, aTypeSupport, attributes, selectedIdentity) :
				new ROExposedAttributesComponent(msg, identityTypeSupport, attributes, handlersRegistry, 
						selectedIdentity);
		eiLayout.add((Component)attrsPresenter);
		return exposedInfoPanel;
	}

	protected void createRememberMeCheckbox(VerticalLayout layout)
	{
		rememberCB = new Checkbox(msg.getMessage("SamlIdPWebUI.rememberSettings"));
		rememberCB.addClassName("u-consent-screen-checkbox");
		layout.add(rememberCB);
	}
	
	private void createButtonsPart(final SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, SAML_ENTRY_SERVLET_PATH, action ->
		{
			if (IdPButtonsBar.Action.ACCEPT == action)
				confirm(samlCtx);
			else if (IdPButtonsBar.Action.DENY == action)
				decline();
		});
		
		contents.add(buttons);
		contents.setAlignItems(Alignment.CENTER);
		buttons.setClassName("u-consent-screen-buttons");
	}
	
	
	protected void loadPreferences(SAMLAuthnContext samlCtx)
	{
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			SPSettings settings = preferences.getSPSettings(samlCtx.getRequest().getIssuer());
			updateUIFromPreferences(settings, samlCtx);
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleExceptionNotThrowing(e, true);
			return;
		}
	}
	
	protected void updateUIFromPreferences(SPSettings settings, SAMLAuthnContext samlCtx) throws EngineException
	{
		if (settings == null)
			return;
		Map<String, Attribute> attribtues = settings.getHiddenAttribtues();
		attrsPresenter.setInitialState(attribtues);
		String selId = settings.getSelectedIdentity();
		idSelector.setSelected(selId);

		if (settings.isDoNotAsk())
		{
			if (settings.isDefaultAccept())
				confirm(samlCtx);
			else
				decline();
		}
	}
	
	/**
	 * Applies UI selected values to the given preferences object
	 */
	protected void updatePreferencesFromUI(SamlPreferences preferences, SAMLAuthnContext samlCtx, boolean defaultAccept) 
			throws EngineException
	{
		if (!rememberCB.getValue())
			return;
		NameIDType reqIssuer = samlCtx.getRequest().getIssuer();
		SPSettings settings = preferences.getSPSettings(reqIssuer);
		settings.setDefaultAccept(defaultAccept);
		settings.setDoNotAsk(true);
		settings.setHiddenAttribtues(attrsPresenter.getHiddenAttributes());

		String identityValue = idSelector.getSelectedIdentityForPreferences();
		if (identityValue != null)
			settings.setSelectedIdentity(identityValue);
		settings.setTimestamp(Instant.now());
		preferences.setSPSettings(reqIssuer, settings);
	}
	
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext();
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
			SamlPreferences.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}

	protected void decline()
	{
		storePreferences(false);
		declineHandler.run();
	}
	
	protected void confirm(SAMLAuthnContext samlCtx)
	{
		storePreferences(true);
		acceptHandler.onAccepted(idSelector.getSelectedIdentity(), 
				getExposedAttributes());
	}
	
	protected Collection<Attribute> getExposedAttributes()
	{
		return attrsPresenter.getUserFilteredAttributes();
	}
	
	public interface ConfirmationConsumer
	{
		void onAccepted(IdentityParam selectedIdentity, Collection<Attribute> attributes);
	}
}
