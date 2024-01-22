/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.pam.web;

import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.pam.PAMVerificator;

import java.util.stream.Collectors;


@Component
public class PamAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{

	private final MessageSource msg;
	private final RegistrationsManagement regMan;
	private final InputTranslationProfileFieldFactory profileFieldFactory;

	PamAuthenticatorEditorFactory(MessageSource msg, RegistrationsManagement regMan,
			InputTranslationProfileFieldFactory profileFieldFactory)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.profileFieldFactory = profileFieldFactory;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return PAMVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new PamAuthenticatorEditor(msg,
				regMan.getForms().stream().map(DescribedObjectROImpl::getName)
						.collect(Collectors.toList()), profileFieldFactory);
	}

}
