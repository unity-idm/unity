/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.userimport.UserImportSPI;
import pl.edu.icm.unity.server.api.userimport.UserImportSPIFactory;


/**
 * Imports user from LDAP. Shares similar code and configuration with the LDAP verificator, however
 * does not verify user's password, and bindAs=user is not allowed.
 * @author K. Benedyczak
 */
@Component
public class LdapImporterFactory implements UserImportSPIFactory
{
	public static final String NAME = "ldap";
	
	private final PKIManagement pkiManagement;

	@Autowired
	public LdapImporterFactory(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public UserImportSPI getInstance(Properties configuration, String idpName)
	{
		return new LdapImporter(pkiManagement, configuration, idpName);
	}

}
