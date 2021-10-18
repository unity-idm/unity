/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.Properties;

import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;

class TrustAllSPProperties extends SAMLSPProperties
{
	public TrustAllSPProperties(Properties properties, PKIManagement pkiMan) throws ConfigurationException
	{
		super(properties, pkiMan);
	}

	protected TrustAllSPProperties(TrustAllSPProperties cloned) throws ConfigurationException
	{
		super(cloned);
	}
	
	@Override
	public SamlTrustChecker getTrustChecker() throws ConfigurationException
	{
		return new TrustAllTrustChecker();
	}
	
	@Override
	public SAMLSPProperties clone()
	{
		return new TrustAllSPProperties(this);
	}
}