/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.rest.authn.ext.TLSRetrievalBase;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Retrieves certificate from the TLS
 * @author K. Benedyczak
 */
@Component("SOAPTLSRetrieval")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TLSRetrieval extends TLSRetrievalBase implements WebServiceAuthentication
{
	public static final String NAME = "cxf-certificate";
	public static final String DESC = "CXFTLSRetrievalFactory.desc";
	
	public TLSRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
	
	@Component("SOAPTLSRetrievalFactory")
	public static class Factory extends AbstractCredentialRetrievalFactory<TLSRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<TLSRetrieval> factory)
		{
			super(NAME, DESC, WebServiceAuthentication.NAME, factory, CertificateExchange.class);
		}
	}
}
