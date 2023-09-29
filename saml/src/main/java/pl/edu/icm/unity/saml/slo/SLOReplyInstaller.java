/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;

/**
 * Allows to install the {@link SLOReplyServlet} under unitygw. Ensures that the servlet is installed only once.
 * The intention is to install the servlet whenever there is at least one SAML IdP or SP enabled in the system. 
 * @author K. Benedyczak
 */
@Component
public class SLOReplyInstaller
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOReplyInstaller.class);
	public static final String PATH = "/SAMLSLOResponseConsumer";
	
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private SharedEndpointManagement sharedEndpointManagement;
	
	private boolean enabled = false;
	
	@Autowired
	public SLOReplyInstaller(SAMLLogoutProcessorFactory logoutProcessorFactory,
			SharedEndpointManagement sharedEndpointManagement)
	{
		this.logoutProcessorFactory = logoutProcessorFactory;
		this.sharedEndpointManagement = sharedEndpointManagement;
	}
	
	/**
	 * Installs SLO reply servlet, only if it was not installed before
	 * @throws EngineException 
	 */
	public synchronized void enable() throws EngineException
	{
		if (enabled)
			return;
		log.info("Enabling SAML Single Logout reply servlet at " + PATH);
		
		InternalLogoutProcessor internalProcessor = logoutProcessorFactory.getInternalProcessorInstance(
				sharedEndpointManagement.getServletUrl(PATH));
		SLOReplyServlet replyServlet = new SLOReplyServlet(internalProcessor);
		ServletHolder servlet = new ServletHolder(replyServlet);

		sharedEndpointManagement.deployInternalEndpointServlet(PATH, servlet, false);
		enabled = true;
	}
	
	public String getServletURL()
	{
		return sharedEndpointManagement.getServletUrl(PATH);
	}
}
