/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * ECP endpoint used to enable ECP support in Unity. The endpoint doesn't use any authenticator by itself.
 * @author K. Benedyczak
 */
public class ECPEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	private Properties properties;
	private SAMLSPProperties samlProperties;
	private String servletPath;
	private PKIManagement pkiManagement;
	private ECPContextManagement samlContextManagement;
	private URL baseAddress;
	private ReplayAttackChecker replayAttackChecker;
	private IdentityResolver identityResolver;
	private TranslationProfileManagement profileManagement;
	private AttributesManagement attrMan;
	
	public ECPEndpoint(EndpointTypeDescription type, String servletPath, PKIManagement pkiManagement,
			ECPContextManagement samlContextManagement, URL baseAddress, 
			ReplayAttackChecker replayAttackChecker, IdentityResolver identityResolver,
			TranslationProfileManagement profileManagement, AttributesManagement attrMan)
	{
		super(type);
		this.pkiManagement = pkiManagement;
		this.servletPath = servletPath;
		this.samlContextManagement = samlContextManagement;
		this.baseAddress = baseAddress;
		this.replayAttackChecker = replayAttackChecker;
		this.identityResolver = identityResolver;
		this.profileManagement = profileManagement;
		this.attrMan = attrMan;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new ByteArrayInputStream(serializedState.getBytes()));
			samlProperties = new SAMLSPProperties(properties, pkiManagement);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML ECP" +
					" endpoint's configuration", e);
		}
	}

	@Override
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		String endpointAddress = baseAddress.toExternalForm() + description.getContextAddress() +
				servletPath;
		ECPServlet ecpServlet = new ECPServlet(samlProperties, samlContextManagement, endpointAddress, 
				replayAttackChecker, identityResolver, profileManagement, attrMan);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath(description.getContextAddress());
		ServletHolder holder = new ServletHolder(ecpServlet);
		context.addServlet(holder, servletPath + "/*");
		return context;
	}
	
	@Override
	public void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
}
