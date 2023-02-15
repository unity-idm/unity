/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import com.google.common.base.Strings;
import com.vaadin.server.Resource;
import com.vaadin.ui.*;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.idpcommon.*;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar.Action;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consent screen of the SAML web IdP. Fairly simple: shows who asks, what is going to be sent,
 * and optionally allows for some customization.
 *  
 * @author K. Benedyczak
 */
class SamlConsentScreen extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlConsentScreen.class);
	protected final MessageSource msg;
	protected final AttributeHandlerRegistryV8 handlersRegistry;
	protected final IdentityTypeSupport identityTypeSupport;
	protected final PreferencesManagement preferencesMan;
	protected final StandardWebLogoutHandler authnProcessor;
	protected final AttributeTypeSupport aTypeSupport;
	protected final ImageAccessService imageAccessService;

	protected final List<IdentityParam> validIdentities;
	protected final Collection<DynamicAttribute> attributes;
	protected final Map<String, AttributeType> attributeTypes;
	
	protected final Runnable declineHandler;
	protected final ConfirmationConsumer acceptHandler;
	
	protected IdentitySelectorComponent idSelector;
	protected SelectableAttributesComponent attrsPresenter;
	protected SamlResponseHandler samlResponseHandler;
	protected CheckBox rememberCB;

	public SamlConsentScreen(MessageSource msg, ImageAccessService imageAccessService,  
			AttributeHandlerRegistryV8 handlersRegistry,
			PreferencesManagement preferencesMan,
			StandardWebLogoutHandler authnProcessor, 
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
		
		VerticalLayout vmain = new VerticalLayout();
		vmain.setMargin(false);
		vmain.setSpacing(false);
		
		VerticalLayout contents = new VerticalLayout();
		contents.addStyleName("u-consentMainColumn");
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.TOP_CENTER);

		createInfoPart(samlCtx, contents);

		Component exposedInfoPanel = createExposedDataPart(samlCtx);
		contents.addComponent(exposedInfoPanel);
		createRememberMeCheckbox(contents);
		
		createButtonsPart(samlCtx, contents);

		setCompositionRoot(vmain);

		loadPreferences(samlCtx);
	}

	private void createInfoPart(SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		AuthnRequestType request = samlCtx.getRequest();

		String samlRequester = request.getIssuer().getStringValue();
		String returnAddress = samlCtx.getSamlConfiguration().getReturnAddressForRequester(request);
		String displayedName = samlCtx.getSamlConfiguration().getDisplayedNameForRequester(request.getIssuer(), msg);
		Resource logo = samlCtx.getSamlConfiguration().getLogoForRequesterOrNull(request.getIssuer(), msg,
				imageAccessService);

		SPInfoComponent spInfo = new SPInfoComponent(msg, logo,
				Strings.isNullOrEmpty(displayedName) ? samlRequester : displayedName,
				returnAddress);

		contents.addComponents(spInfo);
	}

	protected Component createExposedDataPart(SAMLAuthnContext samlCtx)
	{
		SafePanel exposedInfoPanel = new SafePanel();
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setWidth(100, Unit.PERCENTAGE);
		exposedInfoPanel.setContent(eiLayout);
		idSelector = new IdentitySelectorComponent(msg, identityTypeSupport, validIdentities);

		Label info1 = new Label100(msg.getMessage("SamlIdPWebUI.allowForSignInInfo"));
		Label info2 = new Label100(msg.getMessage("SamlIdPWebUI.allowForReadingUserProfile"));
		eiLayout.addComponents(info1, info2);
		
		if (validIdentities.size() > 1)
			eiLayout.addComponent(idSelector);
		
		eiLayout.addComponent(HtmlTag.br());
		
		boolean userCanEditConsent = samlCtx.getSamlConfiguration().userCanEditConsent;
		Optional<IdentityParam> selectedIdentity = Optional.ofNullable(validIdentities.size() == 1 ? validIdentities.get(0) : null); 
		attrsPresenter = userCanEditConsent ? 
				new ExposedSelectableAttributesComponent(msg, identityTypeSupport, handlersRegistry, 
						attributeTypes, aTypeSupport, attributes, selectedIdentity) :
				new ROExposedAttributesComponent(msg, identityTypeSupport, attributes, handlersRegistry, 
						selectedIdentity);
		eiLayout.addComponent((Component)attrsPresenter);
		return exposedInfoPanel;
	}

	protected void createRememberMeCheckbox(VerticalLayout layout)
	{
		rememberCB = new CheckBox(msg.getMessage("SamlIdPWebUI.rememberSettings"));
		rememberCB.setWidth(100, Unit.PERCENTAGE);
		layout.addComponent(rememberCB);
	}
	
	private void createButtonsPart(final SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, action ->
		{
			if (Action.ACCEPT == action)
				confirm(samlCtx);
			else if (Action.DENY == action)
				decline();
		});
		
		contents.addComponent(buttons);
		contents.setComponentAlignment(buttons, Alignment.BOTTOM_RIGHT);
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
			setCompositionRoot(new VerticalLayout());
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
