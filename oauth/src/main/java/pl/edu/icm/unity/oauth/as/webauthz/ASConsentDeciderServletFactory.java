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
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter.OAuthIdpStatisticReporterFactory;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

@Component
class ASConsentDeciderServletFactory
{
	protected final PreferencesManagement preferencesMan;
	protected final IdPEngine idpEngine;
	private final OAuthSessionService oauthSessionService;
	private final EnquiryManagement enquiryManagement;
	private final OAuthProcessor processor;
	private final PolicyAgreementManagement policyAgreementManagement;
	private final OAuthIdpStatisticReporterFactory idpStatisticReporterFactory;
	private final MessageSource msg;

	@Autowired
	ASConsentDeciderServletFactory(PreferencesManagement preferencesMan, IdPEngine idpEngine,
			OAuthSessionService oauthSessionService, OAuthProcessor processor,
			@Qualifier("insecure") EnquiryManagement enquiryManagement,
			PolicyAgreementManagement policyAgreementManagement,
			OAuthIdpStatisticReporterFactory idpStatisticReporterFactory, MessageSource msg)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.oauthSessionService = oauthSessionService;
		this.processor = processor;
		this.enquiryManagement = enquiryManagement;
		this.policyAgreementManagement = policyAgreementManagement;
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
		this.msg = msg;
	}

	ASConsentDeciderServlet getInstance(String oauthUiServletPath, ResolvedEndpoint endpoint)
	{
		return new ASConsentDeciderServlet(preferencesMan, idpEngine, processor, oauthSessionService,
				oauthUiServletPath, enquiryManagement, policyAgreementManagement,
				idpStatisticReporterFactory.getForEndpoint(endpoint.getEndpoint()), msg);
	}
}
