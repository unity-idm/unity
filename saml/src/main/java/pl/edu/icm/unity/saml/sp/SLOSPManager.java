/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOSAMLServlet;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.utils.Log;
import eu.emi.security.authn.x509.X509Credential;

/**
 * Keeps track of SLO servlet installation under the /unitygw. Installs the servlet only if it was not yet 
 * deployed, makes sure the subpaths are not overlapping.
 * 
 * @author K. Benedyczak
 */
@Component
public class SLOSPManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOSPManager.class);
	public static final String PATH = "/SPSLO/";
	
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private SharedEndpointManagement sharedEndpointManagement;
	private Map<String, Servlet> deployedServlets = new HashMap<String, Servlet>();
	
	@Autowired
	public SLOSPManager(SAMLLogoutProcessorFactory logoutProcessorFactory,
			SharedEndpointManagement sharedEndpointManagement)
	{
		super();
		this.logoutProcessorFactory = logoutProcessorFactory;
		this.sharedEndpointManagement = sharedEndpointManagement;
	}
	
	public synchronized void deployAsyncServlet(String pathSuffix, IdentityTypeMapper identityTypeMapper, 
			long requestValidity, String localSamlId,
			X509Credential localSamlCredential, SamlTrustProvider samlTrustProvider,
			String realm) throws EngineException
	{
		if (deployedServlets.containsKey(pathSuffix))
		{
			log.debug("SLO servlet at " + pathSuffix + " already installed, skipping re-installation");
			return;
		}
		String prefixed = PATH + pathSuffix;
		log.info("Enabling SAML Single Logout servlet for SP side (athenticator) at " + prefixed);
		
		SAMLLogoutProcessor logoutProcessor = logoutProcessorFactory.getInstance(
				identityTypeMapper, getAsyncServletURL(pathSuffix), requestValidity, localSamlId, 
				localSamlCredential, samlTrustProvider, realm);
		
		SLOSAMLServlet servlet = new SLOSAMLServlet(logoutProcessor);
		ServletHolder servletHolder = new ServletHolder(servlet);

		sharedEndpointManagement.deployInternalEndpointServlet(prefixed, servletHolder);
		deployedServlets.put(pathSuffix, servlet);
	}
	
	public String getAsyncServletURL(String suffix)
	{
		if (!deployedServlets.containsKey(suffix))
			return null;
		return sharedEndpointManagement.getServletUrl(PATH + suffix);
	}
}
