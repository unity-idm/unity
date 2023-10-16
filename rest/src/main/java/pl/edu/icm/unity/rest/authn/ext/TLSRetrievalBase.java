/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import java.security.cert.X509Certificate;
import java.util.Properties;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import jakarta.servlet.http.HttpServletRequest;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;

/**
 * Retrieves certificate from the TLS
 * @author K. Benedyczak
 */
public abstract class TLSRetrievalBase extends AbstractCredentialRetrieval<CertificateExchange> implements CXFAuthentication
{
	public TLSRetrievalBase(String bindingName)
	{
		super(bindingName);
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
	public AuthenticationResult getAuthenticationResult(Properties endpointFeatures)
	{
		X509Certificate[] certificates = getTLSCertificates();
		if (certificates == null)
			return LocalAuthenticationResult.failed(new ResolvableError("TLSRetrievalBase.certificatesNotFound"),
					DenyReason.undefinedCredential);
		try
		{
			return credentialExchange.checkCertificate(certificates, null, false,
					AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());
		} catch (Exception e)
		{
			return LocalAuthenticationResult.failed(e);
		}
	}

	/**
	 * @return null if not available, authenticated certificates otherwise.
	 */
	public static X509Certificate[] getTLSCertificates()
	{
		Message message = PhaseInterceptorChain.getCurrentMessage();
		if (message == null)
			return null;
		HttpServletRequest req =(HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if(req!=null)
			return (X509Certificate[])req.getAttribute("jakarta.servlet.request.X509Certificate");
		return null;
	}
}
