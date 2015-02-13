/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

/**
 * Retrieves certificate from the TLS
 * @author K. Benedyczak
 */
public class TLSRetrieval extends TLSRetrievalBase implements JAXRSAuthentication
{
	public TLSRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}
}
