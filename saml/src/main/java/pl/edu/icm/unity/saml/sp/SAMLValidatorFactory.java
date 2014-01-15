/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.validators.ReplayAttackChecker;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
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
	private PKIManagement pkiMan;
	private ReplayAttackChecker replayAttackChecker;
	
	@Autowired
	public SAMLValidatorFactory(@Qualifier("insecure") TranslationProfileManagement profileManagement,
			@Qualifier("insecure") AttributesManagement attrMan, 
			PKIManagement pkiMan, ReplayAttackChecker replayAttackChecker)
	{
		this.profileManagement = profileManagement;
		this.attrMan = attrMan;
		this.pkiMan = pkiMan;
		this.replayAttackChecker = replayAttackChecker;
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
		return new SAMLValidator(NAME, getDescription(), profileManagement, attrMan, pkiMan, 
				replayAttackChecker);
	}
}
