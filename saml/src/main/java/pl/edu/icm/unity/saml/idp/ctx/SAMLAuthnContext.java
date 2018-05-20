/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 21, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml.idp.ctx;

import java.util.Date;

import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

/**
 * SAML Context for authN request protocol.
 * 
 * @author K. Benedyczak
 */
public class SAMLAuthnContext extends SAMLAssertionResponseContext<AuthnRequestDocument, AuthnRequestType>
{
	private String relayState;
	private Date creationTs;
	
	public SAMLAuthnContext(AuthnRequestDocument reqDoc, SamlIdpProperties samlConfiguration)
	{
		super(reqDoc, reqDoc.getAuthnRequest(), samlConfiguration);
		creationTs = new Date();
	}

	public String getRelayState()
	{
		return relayState;
	}

	public void setRelayState(String relayState)
	{
		this.relayState = relayState;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}
	
	public boolean isExpired()
	{
		long timeout = 1000L*samlConfiguration.getIntValue(SamlIdpProperties.AUTHENTICATION_TIMEOUT);
		return System.currentTimeMillis() > timeout+creationTs.getTime();
	}
	
	public String getResponseDestination()
	{
		return getSamlConfiguration().getReturnAddressForRequester(getRequest());
	}
}
