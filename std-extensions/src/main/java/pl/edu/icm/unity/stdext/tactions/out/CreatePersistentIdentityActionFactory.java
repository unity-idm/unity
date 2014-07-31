/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Factory for {@link CreatePersistentIdentityAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreatePersistentIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "createPersistedIdentity";
	
	private IdentityTypesRegistry idTypesReg;

	@Autowired
	public CreatePersistentIdentityActionFactory(IdentityTypesRegistry idTypesReg)
	{
		super();
		this.idTypesReg = idTypesReg;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.createPersistedIdentity.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"identityType",
						"TranslationAction.createPersistedIdentity.paramDesc.idType",
						1, 1, Type.UNITY_ID_TYPE),
				new ActionParameterDesc(
						"expression",
						"TranslationAction.createPersistedIdentity.paramDesc.idValueExpression",
						1, 1, Type.EXPRESSION)};
	}
	
	@Override
	public CreatePersistentIdentityAction getInstance(String... parameters) throws EngineException
	{
		return new CreatePersistentIdentityAction(parameters, this, idTypesReg);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
