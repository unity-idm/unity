/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.idpcommon.ExposedSelectableAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar;
import pl.edu.icm.unity.webui.idpcommon.IdPButtonsBar.Action;
import pl.edu.icm.unity.webui.idpcommon.IdentitySelectorComponent;
import pl.edu.icm.unity.webui.idpcommon.SPInfoComponent;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

/**
 * Consent screen of the SAML web IdP. Fairly simple: shows who asks, what is going to be sent,
 * and optionally allows for some customization.
 *  
 * @author K. Benedyczak
 */
public class SamlConsentScreen extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlConsentScreen.class);
	protected final UnityMessageSource msg;
	protected final AttributeHandlerRegistry handlersRegistry;
	protected final IdentityTypeSupport identityTypeSupport;
	protected final PreferencesManagement preferencesMan;
	protected final StandardWebAuthenticationProcessor authnProcessor;
	protected final AttributeTypeSupport aTypeSupport;

	protected final List<IdentityParam> validIdentities;
	protected final Collection<DynamicAttribute> attributes;
	protected final Map<String, AttributeType> attributeTypes;
	
	protected final Runnable declineHandler;
	protected final ConfirmationConsumer acceptHandler;
	
	protected IdentitySelectorComponent idSelector;
	protected ExposedSelectableAttributesComponent attrsPresenter;
	protected SamlResponseHandler samlResponseHandler;
	protected CheckBox rememberCB;

	public SamlConsentScreen(UnityMessageSource msg, 
			AttributeHandlerRegistry handlersRegistry, 
			PreferencesManagement preferencesMan,
			StandardWebAuthenticationProcessor authnProcessor, 
			IdentityTypeSupport identityTypeSupport, 
			AttributeTypeSupport aTypeSupport,
			List<IdentityParam> validIdentities,
			Collection<DynamicAttribute> attributes,
			Map<String, AttributeType> attributeTypes,
			Runnable declineHandler,
			ConfirmationConsumer acceptHandler)
	{
		this.msg = msg;
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
		SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
		
		VerticalLayout vmain = new VerticalLayout();
		vmain.setMargin(false);
		vmain.setSpacing(false);
		
		VerticalLayout contents = new VerticalLayout();
		contents.addStyleName(Styles.maxWidthColumn.toString());
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.TOP_CENTER);

		createInfoPart(samlCtx, contents);

		Component exposedInfoPanel = createExposedDataPart(samlCtx);
		contents.addComponent(exposedInfoPanel);
		
		createButtonsPart(samlCtx, contents);

		setCompositionRoot(vmain);

		loadPreferences(samlCtx);
	}

	private void createInfoPart(SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
		String returnAddress = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
					samlCtx.getRequest());

		Label info1 = new Label(msg.getMessage("SamlIdPWebUI.info1"));
		info1.addStyleName(Styles.vLabelH1.toString());
		SPInfoComponent spInfo = new SPInfoComponent(msg, null, samlRequester, returnAddress);
		Label spc1 = HtmlTag.br();
		Label info2 = new Label(msg.getMessage("SamlIdPWebUI.info2"));
		
		contents.addComponents(info1, spInfo, spc1, info2);
	}

	protected Component createExposedDataPart(SAMLAuthnContext samlCtx)
	{
		SafePanel exposedInfoPanel = new SafePanel();
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setWidth(100, Unit.PERCENTAGE);
		exposedInfoPanel.setContent(eiLayout);
		idSelector = new IdentitySelectorComponent(msg, identityTypeSupport, validIdentities);
		eiLayout.addComponent(idSelector);

		eiLayout.addComponent(HtmlTag.br());
		boolean userCanEditConsent = samlCtx.getSamlConfiguration().getBooleanValue(SamlIdpProperties.USER_EDIT_CONSENT);
		attrsPresenter = new ExposedSelectableAttributesComponent(msg, handlersRegistry, attributeTypes, 
				aTypeSupport, attributes, userCanEditConsent);
		eiLayout.addComponent(attrsPresenter);
		
		rememberCB = new CheckBox(msg.getMessage("SamlIdPWebUI.rememberSettings"));
		eiLayout.addComponent(rememberCB);
		return exposedInfoPanel;
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
		contents.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
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
	 * @param preferences
	 * @param samlCtx
	 * @param defaultAccept
	 * @throws EngineException
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
		preferences.setSPSettings(reqIssuer, settings);
	}
	
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
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
		return attrsPresenter.getUserFilteredAttributes().values();
	}
	
	public interface ConfirmationConsumer
	{
		void onAccepted(IdentityParam selectedIdentity, Collection<Attribute> attributes);
	}
}
