/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Marker interface. Implementations are used to exchange credentials between credential
 * verificator and credential retriever.
 * The actual exchange process might be complicated and require several interactions. E.g.
 * challenge based verificator might first give a challenge to the retrieval and then retrieval
 * return the challenge response. Typical implementation will define a method setting a callback.
 * The method will be invoked by verificator, and callback activated by retriever.
 * <p>
 * Example credentials: SAML (IdP name/address), OpenID (IdP name/address), Username and password,
 * XX challenge, SMS
 * 
 * @author K. Benedyczak
 */
public interface CredentialExchange
{
	public String getExchangeId();
}
