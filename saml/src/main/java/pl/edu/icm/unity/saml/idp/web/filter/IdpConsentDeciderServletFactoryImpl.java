/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;

/**
 * Creates {@link IdpConsentDeciderServlet}s.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
public class IdpConsentDeciderServletFactoryImpl implements IdpConsentDeciderServletFactory
{
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected FreemarkerHandler freemarker;
	protected SessionManagement sessionMan;
	private AttributeTypeSupport aTypeSupport;

	@Autowired
	public IdpConsentDeciderServletFactoryImpl(PreferencesManagement preferencesMan,
			IdPEngine idpEngine, FreemarkerHandler freemarker,
			SessionManagement sessionMan,
			AttributeTypeSupport aTypeSupport)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.freemarker = freemarker;
		this.sessionMan = sessionMan;
		this.aTypeSupport = aTypeSupport;
	}
	
	@Override
	public IdpConsentDeciderServlet getInstance(String uiServletPath)
	{
		return new IdpConsentDeciderServlet(aTypeSupport, preferencesMan,  
				idpEngine, freemarker, sessionMan, uiServletPath);
	}
	
}
