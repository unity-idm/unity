/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.security.etd.DelegationRestrictions;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.web.SAMLContextSupport;
import pl.edu.icm.unity.saml.idp.web.SamlConsentScreen;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;
import pl.edu.icm.unity.webui.idpcommon.ExposedSelectableAttributesComponent;
import pl.edu.icm.unity.webui.idpcommon.IdentitySelectorComponent;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;


/**
 * The main UI of the SAML web IdP. It is an extension of the {@link SamlConsentScreen}, adding a possibility
 * to configure UNICORE bootstrap ETD generation.
 *  
 * @author K. Benedyczak
 */
class UnicoreConsentScreen extends SamlConsentScreen
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, UnicoreConsentScreen.class);

	private ETDSettingsEditor etdEditor;

	private UnicoreConfirmationConsumer acceptHandler;

	public UnicoreConsentScreen(UnityMessageSource msg, 
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan, 
			StandardWebAuthenticationProcessor authnProcessor,
			IdentityTypeSupport identityTypeSupport, 
			AttributeTypeSupport aTypeSupport,
			List<IdentityParam> validIdentities,
			Collection<DynamicAttribute> attributes, 
			Map<String, AttributeType> attributeTypes,
			Runnable declineHandler, 
			UnicoreConfirmationConsumer acceptHandler)
	{
		super(msg, handlersRegistry, preferencesMan, authnProcessor, identityTypeSupport, aTypeSupport, 
				validIdentities, attributes, attributeTypes, declineHandler, null);
		this.acceptHandler = acceptHandler;
	}
	
	@Override
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

		eiLayout.addComponent(HtmlTag.br());
		createETDPart(eiLayout);

		rememberCB = new CheckBox(msg.getMessage("SamlIdPWebUI.rememberSettings"));
		eiLayout.addComponent(rememberCB);
		return exposedInfoPanel;
	}
	
	private void createETDPart(VerticalLayout eiLayout)
	{
		Label titleL = new Label(msg.getMessage("SamlUnicoreIdPWebUI.gridSettings"));
		titleL.addStyleName(Styles.bold.toString());
		eiLayout.addComponents(titleL);
		etdEditor = new ETDSettingsEditor(msg, eiLayout);
	}
	
	@Override
	protected void loadPreferences(SAMLAuthnContext samlCtx)
	{
		try
		{
			SamlPreferencesWithETD preferences = SamlPreferencesWithETD.getPreferences(preferencesMan);
			NameIDType samlRequester = samlCtx.getRequest().getIssuer();
			SPSettings baseSettings = preferences.getSPSettings(samlRequester);
			SPETDSettings settings = preferences.getSPETDSettings(samlRequester);
			updateETDUIFromPreferences(settings, samlCtx);
			super.updateUIFromPreferences(baseSettings, samlCtx);
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleExceptionNotThrowing(e, true);
			return;
		}
	}
	
	private void updateETDUIFromPreferences(SPETDSettings settings, SAMLAuthnContext samlCtx) throws EngineException
	{
		if (settings == null)
			return;
		etdEditor.setValues(settings);
	}
	
	private void updatePreferencesFromUI(SamlPreferencesWithETD preferences, SAMLAuthnContext samlCtx, 
			boolean defaultAccept) throws EngineException
	{
		super.updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
		if (!rememberCB.getValue())
			return;
		SPETDSettings settings = etdEditor.getSPETDSettings();
		preferences.setSPETDSettings(samlCtx.getRequest().getIssuer(), settings);
	}
	
	@Override
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
			SamlPreferencesWithETD preferences = SamlPreferencesWithETD.getPreferences(preferencesMan);
			updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
			SamlPreferencesWithETD.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}
	
	@Override
	protected void confirm(SAMLAuthnContext samlCtx)
	{
		storePreferences(true);
		DelegationRestrictions restrictions = etdEditor.getSPETDSettings().toDelegationRestrictions();
		acceptHandler.onAccepted(idSelector.getSelectedIdentity(), 
				getExposedAttributes(), restrictions);
	}
	
	public interface UnicoreConfirmationConsumer
	{
		void onAccepted(IdentityParam selectedIdentity, Collection<Attribute> attributes, 
				DelegationRestrictions restrictions);
	}
}
