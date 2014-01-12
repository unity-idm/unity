/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import eu.unicore.samly2.SAMLConstants;

/**
 * SAML name formats as easy to write enum. Full names are defined in {@link SAMLConstants}
 * @author K. Benedyczak
 */
public enum NameFormat
{
	emailAddress(SAMLConstants.NFORMAT_EMAIL),
	X509SubjectName(SAMLConstants.NFORMAT_DN),
	WindowsDomainQualifiedName(SAMLConstants.NFORMAT_WDQN),
	encrypted(SAMLConstants.NFORMAT_ENC),
	kerberos(SAMLConstants.NFORMAT_KERBEROS),
	entityId(SAMLConstants.NFORMAT_ENTITY),
	persistentId(SAMLConstants.NFORMAT_PERSISTENT),
	transientId(SAMLConstants.NFORMAT_TRANSIENT);
	
	
	private String full;
	
	NameFormat(String full)
	{
		this.full = full;
	}
	
	public String getSamlRepresentation()
	{
		return full;
	}
}
