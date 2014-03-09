/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.web.EopException;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebUI;
import pl.edu.icm.unity.saml.idp.web.SamlResponseHandler;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.saml.AuthnWithETDResponseProcessor;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.security.etd.DelegationRestrictions;


/**
 * The main UI of the SAML web IdP. It is an extension of the {@link SamlIdPWebUI}, adding a possibility
 * to configure UNICORE bootstrap ETD generation and using the {@link AuthnWithETDResponseProcessor}.
 *  
 * @author K. Benedyczak
 */
@Component("SamlUnicoreIdPWebUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class SamlUnicoreIdPWebUI extends SamlIdPWebUI implements UnityWebUI
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlUnicoreIdPWebUI.class);

	private AuthnWithETDResponseProcessor samlWithEtdProcessor;
	private ETDSettingsEditor etdEditor;

	@Autowired
	public SamlUnicoreIdPWebUI(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			AttributesManagement attributesMan, FreemarkerHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan)
	{
		super(msg, identitiesMan, attributesMan, freemarkerHandler, handlersRegistry, preferencesMan);
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = SamlResponseHandler.getContext();
		samlWithEtdProcessor = new AuthnWithETDResponseProcessor(samlCtx, Calendar.getInstance());
		super.appInit(request);
	}
	
	private SamlPreferencesWithETD getPreferencesWithETD() throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		String raw = preferencesMan.getPreference(entity, SamlPreferencesWithETD.ID);
		SamlPreferencesWithETD ret = new SamlPreferencesWithETD();
		ret.setSerializedConfiguration(raw);
		return ret;
	}
	
	@Override
	protected void createExposedDataPart(VerticalLayout contents) throws EopException
	{
		Panel exposedInfoPanel = new Panel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			createIdentityPart(eiLayout);
			eiLayout.addComponent(new Label("<br>", ContentMode.HTML));
			createAttributesPart(eiLayout);
			eiLayout.addComponent(new Label("<br>", ContentMode.HTML));
			createETDPart(eiLayout);
		} catch (SAMLRequesterException e)
		{
			//we kill the session as the user may want to log as different user if has access to several entities.
			log.debug("SAML problem when handling client request", e);
			samlResponseHandler.handleException(e, true);
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleException(e, true);
			return;
		}
		
		rememberCB = new CheckBox("Remember the settings for this service and do not show this dialog again");
		contents.addComponent(rememberCB);
	}

	protected void createETDPart(VerticalLayout eiLayout)
	{
		Label titleL = new Label(msg.getMessage("SamlUnicoreIdPWebUI.gridSettings"));
		titleL.setStyleName(Styles.bold.toString());
		eiLayout.addComponents(titleL);
		etdEditor = new ETDSettingsEditor(msg, eiLayout);
	}
	
	
	@Override
	protected void loadPreferences(SAMLAuthnContext samlCtx) throws EopException
	{
		try
		{
			SamlPreferencesWithETD preferences = getPreferencesWithETD();
			NameIDType samlRequester = samlCtx.getRequest().getIssuer();
			SPSettings baseSettings = preferences.getSPSettings(samlRequester);
			SPETDSettings settings = preferences.getSPETDSettings(samlRequester);
			updateETDUIFromPreferences(settings, samlCtx);
			super.updateUIFromPreferences(baseSettings, samlCtx);
		} catch (EopException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleException(e, true);
			return;
		}
	}
	
	protected void updateETDUIFromPreferences(SPETDSettings settings, SAMLAuthnContext samlCtx) throws EngineException
	{
		if (settings == null)
			return;
		etdEditor.setValues(settings);
	}
	
	/**
	 * Applies UI selected values to the given preferences object
	 * @param preferences
	 * @param samlCtx
	 * @param defaultAccept
	 * @throws EngineException
	 */
	protected void updatePreferencesFromUI(SamlPreferencesWithETD preferences, SAMLAuthnContext samlCtx, 
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
			SAMLAuthnContext samlCtx = SamlResponseHandler.getContext();
			SamlPreferencesWithETD preferences = getPreferencesWithETD();
			updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
			SamlPreferencesWithETD.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}
	
	protected DelegationRestrictions getRestrictions()
	{
		SPETDSettings settings = etdEditor.getSPETDSettings();
		if (!settings.isGenerateETD())
			return null;
		
		long ms = settings.getEtdValidity();
		Date start = new Date();
		Date end = new Date(start.getTime() + ms);
		return new DelegationRestrictions(start, end, -1);
	}
	
	@Override
	protected String getMyAddress()
	{
		return endpointDescription.getContextAddress() +
				SamlUnicoreIdPWebEndpointFactory.SAML_UI_SERVLET_PATH;
	}
	
	@Override
	protected void confirm() throws EopException
	{
		storePreferences(true);
		ResponseDocument respDoc;
		try
		{
			Collection<Attribute<?>> attributes = getUserFilteredAttributes();
			respDoc = samlWithEtdProcessor.processAuthnRequest(selectedIdentity, attributes, 
					getRestrictions());
		} catch (Exception e)
		{
			samlResponseHandler.handleException(e, false);
			return;
		}
		samlResponseHandler.returnSamlResponse(respDoc);
	}
}
