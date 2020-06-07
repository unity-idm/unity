/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Creates {@link ASConsentDeciderServlet}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class ASConsentDeciderServletFactory
{
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	private SessionManagement sessionMan;
	private EnquiryManagement enquiryManagement;
	private OAuthProcessor processor;
	private PolicyAgreementManagement policyAgreementManagement;
	private MessageSource msg;

	@Autowired
	public ASConsentDeciderServletFactory(PreferencesManagement preferencesMan,
			IdPEngine idpEngine, 
			SessionManagement sessionMan,
			OAuthProcessor processor,
			@Qualifier("insecure") EnquiryManagement enquiryManagement,
			PolicyAgreementManagement policyAgreementManagement,
			MessageSource msg)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.sessionMan = sessionMan;
		this.processor = processor;
		this.enquiryManagement = enquiryManagement;
		this.policyAgreementManagement = policyAgreementManagement;
		this.msg = msg;
	}


	public ASConsentDeciderServlet getInstance(String oauthUiServletPath, String authenticationUIServletPath)
	{
		return new ASConsentDeciderServlet(preferencesMan, idpEngine,  
				processor, sessionMan, oauthUiServletPath, authenticationUIServletPath, 
				enquiryManagement, policyAgreementManagement, msg);
	}
}
