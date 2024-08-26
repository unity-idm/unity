/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.console;

import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.ldap.client.LdapCertVerificator;

import java.util.stream.Collectors;


@Component
class LdapCertAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{

	private final MessageSource msg;
	private final PKIManagement pkiMan;
	private final RegistrationsManagement regMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private final HtmlTooltipFactory htmlTooltipFactory;

	@Autowired
	LdapCertAuthenticatorEditorFactory(MessageSource msg, PKIManagement pkiMan,
			RegistrationsManagement regMan, InputTranslationProfileFieldFactory profileFieldFactory, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.regMan = regMan;
		this.htmlTooltipFactory = htmlTooltipFactory;

	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return LdapCertVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new LdapAuthenticatorEditor(msg, pkiMan, htmlTooltipFactory, profileFieldFactory,
				regMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				LdapCertVerificator.NAME);
	}

}
