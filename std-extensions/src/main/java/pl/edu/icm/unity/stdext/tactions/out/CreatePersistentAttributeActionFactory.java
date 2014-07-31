/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Factory for {@link CreatePersistentAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreatePersistentAttributeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "createPersistentAttribute";
	private AttributesManagement attrsMan;
	
	@Autowired
	public CreatePersistentAttributeActionFactory(AttributesManagement attrsMan)
	{
		super();
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
		return "TranslationAction.createPersistentAttribute.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"attributeName",
						"TranslationAction.createPersistentAttribute.paramDesc.attributeName",
						1, 1, Type.UNITY_ATTRIBUTE),
				new ActionParameterDesc(
						"expression",
						"TranslationAction.createPersistentAttribute.paramDesc.expression",
						1, 1, Type.EXPRESSION),
				new ActionParameterDesc(
						"group",
						"TranslationAction.createPersistentAttribute.paramDesc.group",
						1, 1, Type.UNITY_GROUP)
		};
	}
	
	@Override
	public CreatePersistentAttributeAction getInstance(String... parameters) throws EngineException
	{
		return new CreatePersistentAttributeAction(parameters, this, attrsMan);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
