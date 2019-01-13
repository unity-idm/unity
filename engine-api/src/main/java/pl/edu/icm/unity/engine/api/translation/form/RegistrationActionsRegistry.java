/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link RegistrationTranslationActionFactory}ies.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationActionsRegistry extends TypesRegistryBase<RegistrationTranslationActionFactory>
{
	@Autowired
	public RegistrationActionsRegistry(List<RegistrationTranslationActionFactory> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(RegistrationTranslationActionFactory from)
	{
		return from.getActionType().getName();
	}
}
