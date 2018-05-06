/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;

/**
 * Retrieves certificate from the TLS
 * @author K. Benedyczak
 */
@PrototypeComponent
public class TLSRetrieval extends TLSRetrievalBase implements JAXRSAuthentication
{
	public static final String NAME = "rest-certificate";
	public static final String DESC = "CXFTLSRetrievalFactory.desc";
	
	public TLSRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<TLSRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<TLSRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, CertificateExchange.class);
		}
	}
}
