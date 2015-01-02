/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.SessionManagement;

/**
 * Creates {@link IdpDispatcherServlet}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class IdpDispatcherServletFactory
{
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected FreemarkerHandler freemarker;
	protected SessionManagement sessionMan;

	@Autowired
	public IdpDispatcherServletFactory(PreferencesManagement preferencesMan,
			IdPEngine idpEngine, FreemarkerHandler freemarker,
			SessionManagement sessionMan)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.freemarker = freemarker;
		this.sessionMan = sessionMan;
	}
	
	public IdpDispatcherServlet getInstance(String uiServletPath)
	{
		return new IdpDispatcherServlet(preferencesMan, idpEngine, freemarker, sessionMan, uiServletPath);
	}
	
}
