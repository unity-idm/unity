/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServlet;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServletFactory;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.saml.AuthnWithETDResponseProcessor;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Trivial extension of {@link IdpConsentDeciderServlet}, which uses UNICORE preferences instead of SAML preferences
 * and UNICORE SAML processor to generate automatic replay.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class UnicoreIdpConsentDeciderServlet extends IdpConsentDeciderServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			UnicoreIdpConsentDeciderServlet.class);
	
	@Autowired
	public UnicoreIdpConsentDeciderServlet(AttributeTypeSupport aTypeSupport, 
			PreferencesManagement preferencesMan, 
			IdPEngine idpEngine,
			FreemarkerAppHandler freemarker,
			SessionManagement sessionMan,
			@Qualifier("insecure") EnquiryManagement enquiryManagement)
	{
		super(aTypeSupport, preferencesMan, idpEngine, 
				freemarker, sessionMan, enquiryManagement);
	}
	
	
	
	@Override
	protected SPSettings loadPreferences(SAMLAuthnContext samlCtx) throws EngineException
	{
		SamlPreferencesWithETD preferences = SamlPreferencesWithETD.getPreferences(preferencesMan);
		return preferences.getSPSettings(samlCtx.getRequest().getIssuer());
	}
	
	/**
	 * Automatically sends a SAML response, without the consent screen.
	 * @throws IOException 
	 * @throws EopException 
	 */
	protected void autoReplay(SPSettings spPreferences, SAMLAuthnContext samlCtx, HttpServletRequest request,
			HttpServletResponse response) throws EopException, IOException
	{
		AuthnWithETDResponseProcessor samlProcessor = new AuthnWithETDResponseProcessor(aTypeSupport,
				samlCtx, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		
		String serviceUrl = getServiceUrl(samlCtx);
		
		if (!spPreferences.isDefaultAccept())
		{
			AuthenticationException ea = new AuthenticationException("Authentication was declined");
			ssoResponseHandler.handleException(samlProcessor, ea, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
		}
		ResponseDocument respDoc;
		try
		{
			SamlPreferencesWithETD preferences = SamlPreferencesWithETD.getPreferences(preferencesMan);
			SPETDSettings etdSettings = preferences.getSPETDSettings(samlCtx.getRequest().getIssuer());
			
			TranslationResult userInfo = getUserInfo(samlCtx.getSamlConfiguration(), samlProcessor, 
					SAMLConstants.BINDING_HTTP_POST);
			IdentityParam selectedIdentity = getIdentity(userInfo, samlProcessor, spPreferences);
			log.debug("Authentication of " + selectedIdentity);
			Collection<Attribute> attributes = samlProcessor.getAttributes(userInfo, spPreferences);
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity, attributes, 
					samlCtx.getResponseDestination(),
					etdSettings.toDelegationRestrictions());
		} catch (Exception e)
		{
			ssoResponseHandler.handleException(samlProcessor, e, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
			return;
		}
		addSessionParticipant(samlCtx, samlProcessor.getAuthenticatedSubject().getNameID(), 
				samlProcessor.getSessionId(), sessionMan);
		ssoResponseHandler.sendResponse(Binding.HTTP_POST, respDoc, serviceUrl, 
				samlCtx.getRelayState(), request, response);
	}
	
	@Component
	public static class Factory implements IdpConsentDeciderServletFactory
	{
		@Autowired
		private ObjectFactory<UnicoreIdpConsentDeciderServlet> factory;
		
		@Override
		public IdpConsentDeciderServlet getInstance(String uiServletPath, String authenticationUIServletPath)
		{
			UnicoreIdpConsentDeciderServlet ret = factory.getObject();
			ret.init(uiServletPath, authenticationUIServletPath);
			return ret;
		}
	}
}
