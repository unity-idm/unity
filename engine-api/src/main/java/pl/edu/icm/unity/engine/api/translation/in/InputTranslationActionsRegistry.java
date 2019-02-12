/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link InputTranslationActionFactory}ies.
 * 
 * @author K. Benedyczak
 */
@Component
public class InputTranslationActionsRegistry extends TypesRegistryBase<InputTranslationActionFactory>
{
	@Autowired
	public InputTranslationActionsRegistry(List<InputTranslationActionFactory> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(InputTranslationActionFactory from)
	{
		return from.getActionType().getName();
	}
}
