/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.Calendar;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

import eu.unicore.security.etd.DelegationRestrictions;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.web.SAMLContextSupport;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebUI;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD;
import pl.edu.icm.unity.unicore.samlidp.saml.AuthnWithETDResponseProcessor;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;


/**
 * The main UI of the SAML web IdP. It is an extension of the {@link SamlIdPWebUI}, using UNICORE specific 
 * consent screen and SAML processor returning responses with ETD. 
 *  
 * @author K. Benedyczak
 */
@Component("SamlUnicoreIdPWebUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class SamlUnicoreIdPWebUI extends SamlIdPWebUI implements UnityWebUI
{
	private AuthnWithETDResponseProcessor samlWithEtdProcessor;

	@Autowired
	public SamlUnicoreIdPWebUI(UnityMessageSource msg, FreemarkerAppHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan,
			StandardWebAuthenticationProcessor authnProcessor, IdPEngine idpEngine, 
			IdentityTypeSupport idTypeSupport, SessionManagement sessionMan, 
			AttributeTypeManagement attrMan, 
			EnquiresDialogLauncher enquiryDialogLauncher,
			AttributeTypeSupport aTypeSupport)
	{
		super(msg, freemarkerHandler, handlersRegistry, preferencesMan,	authnProcessor, idpEngine,
				idTypeSupport, sessionMan, attrMan,  
				enquiryDialogLauncher, aTypeSupport);
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
		samlWithEtdProcessor = new AuthnWithETDResponseProcessor(aTypeSupport, samlCtx, 
				Calendar.getInstance());
		super.enter(request);
	}
	
	@Override
	protected void gotoConsentStage(Collection<DynamicAttribute> attributes)
	{
		if (SAMLContextSupport.getContext().getSamlConfiguration().getBooleanValue(CommonIdPProperties.SKIP_CONSENT))
		{
			onAccepted(validIdentities.get(0), attributes.stream()
					.map(da -> da.getAttribute())
					.collect(Collectors.toList()),
					new SamlPreferencesWithETD.SPETDSettings().toDelegationRestrictions());
			return;
		}
		
		UnicoreConsentScreen consentScreen = new UnicoreConsentScreen(msg, 
				handlersRegistry, 
				preferencesMan, 
				authnProcessor, 
				identityTypeSupport, 
				aTypeSupport, 
				validIdentities, 
				attributes, 
				attributeTypes, 
				this::onDecline, 
				this::onAccepted);
		setContent(consentScreen);
	}

	private void onAccepted(IdentityParam selectedIdentity, Collection<Attribute> attributes, 
			DelegationRestrictions restrictions)
	{
		SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
		ResponseDocument respDoc;
		try
		{
			respDoc = samlWithEtdProcessor.processAuthnRequest(selectedIdentity, 
					attributes, samlCtx.getResponseDestination(),
					restrictions);
		} catch (Exception e)
		{
			samlResponseHandler.handleExceptionNotThrowing(e, false);
			return;
		}
		addSessionParticipant(samlCtx, samlWithEtdProcessor.getAuthenticatedSubject().getNameID(), 
				samlWithEtdProcessor.getSessionId());
		samlResponseHandler.returnSamlResponse(respDoc);
	}
}
