/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.console;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.ldap.client.LdapCertVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link LdapAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class LdapCertAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{

	private MessageSource msg;
	private PKIManagement pkiMan;
	private RegistrationsManagement regMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;

	@Autowired
	LdapCertAuthenticatorEditorFactory(MessageSource msg, PKIManagement pkiMan,
			RegistrationsManagement regMan, InputTranslationProfileFieldFactory profileFieldFactory)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.regMan = regMan;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return LdapCertVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new LdapAuthenticatorEditor(msg, pkiMan, profileFieldFactory,
				regMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				LdapCertVerificator.NAME);
	}

}
