/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.console.v8;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.ldap.client.LdapPasswordVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

import java.util.stream.Collectors;

/**
 * Factory for {@link LdapAuthenticatorEditor} - Ldap cert option
 * 
 * @author P.Piernik
 *
 */
@Component("LdapAuthenticatorEditorFactoryV8")
public class LdapAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private MessageSource msg;
	private PKIManagement pkiMan;
	private RegistrationsManagement regMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;

	@Autowired
	public LdapAuthenticatorEditorFactory(MessageSource msg, PKIManagement pkiMan,
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
		return LdapPasswordVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new LdapAuthenticatorEditor(msg, pkiMan, profileFieldFactory,
				regMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				LdapPasswordVerificator.NAME);
	}

}
