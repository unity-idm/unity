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
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;

/**
 * Factory for {@link UpdateAttributesAction}
 *   
 * @author K. Benedyczak
 */
@Component
public class UpdateAttributesActionFactory implements TranslationActionFactory
{
	public static final String NAME = "updateAttributes";
	private AttributesManagement attrsMan;
	
	@Autowired
	public UpdateAttributesActionFactory(@Qualifier("insecure") AttributesManagement attrsMan)
	{
		this.attrsMan = attrsMan;
	}
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.updateAttributes.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						"pattern",
						"TranslationAction.updateAttributes.param.pattern.desc",
						20),
				new ActionParameterDesc(
						true,
						"valuesOnly",
						"TranslationAction.updateAttributes.param.valuesOnly.desc",
						10) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new UpdateAttributesAction(parameters, attrsMan);
	}
}
