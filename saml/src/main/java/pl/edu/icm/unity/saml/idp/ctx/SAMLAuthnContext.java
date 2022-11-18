/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 21, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml.idp.ctx;

import eu.unicore.samly2.messages.SAMLVerifiableElement;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.time.Duration;
import java.time.Instant;

/**
 * SAML Context for authN request protocol.
 * 
 * @author K. Benedyczak
 */
public class SAMLAuthnContext extends SAMLAssertionResponseContext<AuthnRequestDocument, AuthnRequestType>
{
	private String relayState;
	private Instant creationTs;
	private final SAMLVerifiableElement verifiableElement;
	
	public SAMLAuthnContext(AuthnRequestDocument reqDoc, SAMLIdPConfiguration samlConfiguration,
			SAMLVerifiableElement verifiableElement)
	{
		super(reqDoc, reqDoc.getAuthnRequest(), samlConfiguration);
		this.verifiableElement = verifiableElement;
		creationTs = Instant.now();
	}

	public String getRelayState()
	{
		return relayState;
	}

	public void setRelayState(String relayState)
	{
		this.relayState = relayState;
	}

	public boolean isExpired()
	{
		Duration timeout = samlConfiguration.getAuthenticationTimeoutDuration();
		return creationTs.plus(timeout).isAfter(Instant.now());
	}
	
	public String getResponseDestination()
	{
		return getSamlConfiguration().getReturnAddressForRequester(getRequest());
	}

	public SAMLVerifiableElement getVerifiableElement()
	{
		return verifiableElement;
	}
}
