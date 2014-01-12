/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;

/**
 * Factory of {@link SAMLValidator}s.
 * @author K. Benedyczak
 */
@Component
public class SAMLValidatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "saml2";
	
	private TranslationProfileManagement profileManagement;
	private AttributesManagement attrMan;
	
	@Autowired
	public SAMLValidatorFactory(TranslationProfileManagement profileManagement,
			AttributesManagement attrMan)
	{
		this.profileManagement = profileManagement;
		this.attrMan = attrMan;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Handles SAML assertions obtained from remote IdPs";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new SAMLValidator(NAME, getDescription(), profileManagement, attrMan);
	}
}
