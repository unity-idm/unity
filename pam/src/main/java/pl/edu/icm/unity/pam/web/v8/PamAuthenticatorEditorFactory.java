/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.pam.web.v8;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

import java.util.stream.Collectors;

/**
 * Factory for {@link PamAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component("PamAuthenticatorEditorFactoryV8")
public class PamAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{

	private MessageSource msg;
	private RegistrationsManagement regMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;;

	@Autowired
	public PamAuthenticatorEditorFactory(MessageSource msg, RegistrationsManagement regMan,
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
				regMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				profileFieldFactory);
	}

}