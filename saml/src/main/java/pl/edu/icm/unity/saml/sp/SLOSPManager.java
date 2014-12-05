/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.idp.ws.SAMLSingleLogoutImpl;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOSAMLServlet;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.ws.CXFUtils;
import pl.edu.icm.unity.ws.XmlBeansNsHackOutHandler;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.webservice.SAMLLogoutInterface;

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
	public static final String HTTP_PATH = "/SPSLO/WEB/";
	public static final String SOAP_PATH = "/SPSLO/SOAP/";
	
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private SharedEndpointManagement sharedEndpointManagement;
	private Map<String, Servlet> deployedAsyncServlets = new HashMap<String, Servlet>();
	private Map<String, Servlet> deployedSyncServlets = new HashMap<String, Servlet>();
	
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
		if (deployedAsyncServlets.containsKey(pathSuffix))
			return;
		String prefixed = HTTP_PATH + pathSuffix;
		log.info("Enabling SAML HTTP Single Logout servlet for SP side (athenticator) at " + prefixed);
		
		SAMLLogoutProcessor logoutProcessor = logoutProcessorFactory.getInstance(
				identityTypeMapper, getAsyncServletURL(pathSuffix), requestValidity, localSamlId, 
				localSamlCredential, samlTrustProvider, realm);
		
		SLOSAMLServlet servlet = new SLOSAMLServlet(logoutProcessor);
		ServletHolder servletHolder = new ServletHolder(servlet);

		sharedEndpointManagement.deployInternalEndpointServlet(prefixed, servletHolder);
		deployedAsyncServlets.put(pathSuffix, servlet);
	}
	
	public String getAsyncServletURL(String suffix)
	{
		return sharedEndpointManagement.getServletUrl(HTTP_PATH + suffix);
	}
	
	public synchronized void deploySyncServlet(String pathSuffix, IdentityTypeMapper identityTypeMapper, 
			long requestValidity, String localSamlId,
			X509Credential localSamlCredential, SamlTrustProvider samlTrustProvider,
			String realm) throws EngineException
	{
		if (deployedSyncServlets.containsKey(pathSuffix))
			return;
		String prefixed = SOAP_PATH + pathSuffix;
		log.info("Enabling SAML SOAP Single Logout servlet for SP side (athenticator) at " + prefixed);
		
		SAMLLogoutProcessor logoutProcessor = logoutProcessorFactory.getInstance(
				identityTypeMapper, getSyncServletURL(pathSuffix), requestValidity, localSamlId, 
				localSamlCredential, samlTrustProvider, realm);
		
		SAMLSingleLogoutImpl webService = new SAMLSingleLogoutImpl(logoutProcessor);

		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		Bus bus = BusFactory.newInstance().createBus();
		cxfServlet.setBus(bus);
		ServletHolder holder = new ServletHolder(cxfServlet);
		Endpoint cxfEndpoint = CXFUtils.deployWebservice(bus, SAMLLogoutInterface.class, webService);
		cxfEndpoint.getOutInterceptors().add(new XmlBeansNsHackOutHandler());

		sharedEndpointManagement.deployInternalEndpointServlet(prefixed, holder);
		deployedSyncServlets.put(pathSuffix, cxfServlet);
	}

	public String getSyncServletURL(String suffix)
	{
		if (!deployedAsyncServlets.containsKey(suffix))
			return null;
		return sharedEndpointManagement.getServletUrl(SOAP_PATH + suffix + "/SingleLogoutService");
	}
	
	
}
