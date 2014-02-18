/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Factory for {@link CreateUserAction}
 *   
 * @author K. Benedyczak
 */
@Component
public class CreateUserActionFactory implements TranslationActionFactory
{
	public static final String NAME = "createUser";
	private UnityMessageSource msg;
	private AttributesManagement attrsMan;
	private IdentitiesManagement idsMan;

	@Autowired
	public CreateUserActionFactory(@Qualifier("insecure") AttributesManagement attrsMan, 
			@Qualifier("insecure") IdentitiesManagement idsMan, UnityMessageSource msg)
	{
		this.attrsMan = attrsMan;
		this.idsMan = idsMan;
		this.msg = msg;
	}
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return msg.getMessage("TranslationAction.createUser.desc");
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] { new ActionParameterDesc(true,
				msg.getMessage("TranslationAction.createUser.param.1.name"),
				msg.getMessage("TranslationAction.createUser.param.1.desc"), 20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new CreateUserAction(parameters, idsMan, attrsMan);
	}
}
