/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import pl.edu.icm.unity.rest.authn.ext.TLSRetrievalBase;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Retrieves certificate from the TLS
 * @author K. Benedyczak
 */
public class TLSRetrieval extends TLSRetrievalBase implements WebServiceAuthentication
{
	@Override
	public String getBindingName()
	{
		return WebServiceAuthentication.NAME;
	}
}
