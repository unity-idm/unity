/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;

/**
 * Factory of JWT verificators.
 * @author K. Benedyczak
 */
@Component
public class JWTVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "jwt";
	private PKIManagement pkiManagement;
	
	@Autowired
	public JWTVerificatorFactory(PKIManagement pkiManagement)
	{
		super();
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifies JWT";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new JWTVerificator(NAME, getDescription(), pkiManagement);
	}
}
