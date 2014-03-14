/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.stdext.credential.CertificateExchange;
import pl.edu.icm.unity.ws.authn.CXFAuthentication;

/**
 * Retrieves certificate from the TLS
 * @author K. Benedyczak
 */
public class TLSRetrieval implements CredentialRetrieval, CXFAuthentication
{
	private CertificateExchange credentialExchange;
	
	@Override
	public String getBindingName()
	{
		return CXFAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
	}

	@Override
	public AbstractPhaseInterceptor<Message> getInterceptor()
	{
		return null;
	}

	@Override
	public AuthenticationResult getAuthenticationResult()
	{
		X509Certificate[] certificates = getTLSCertificates();
		if (certificates == null)
		{
			return new AuthenticationResult(Status.notApplicable, null);
		}
		try
		{
			return credentialExchange.checkCertificate(certificates);
		} catch (Exception e)
		{
			return new AuthenticationResult(Status.deny, null);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (CertificateExchange) e;
	}
	
	/**
	 * @return null if not available, authenticated certificates othwerwise.
	 */
	public static X509Certificate[] getTLSCertificates()
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		if (message == null)
			return null;
		HttpServletRequest req =(HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if(req!=null)
			return (X509Certificate[])req.getAttribute("javax.servlet.request.X509Certificate");
		return null;
	}
}
