/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.out;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link OutputTranslationActionFactory}ies.
 * 
 * @author K. Benedyczak
 */
@Component
public class OutputTranslationActionsRegistry extends TypesRegistryBase<OutputTranslationActionFactory>
{
	@Autowired
	public OutputTranslationActionsRegistry(List<OutputTranslationActionFactory> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(OutputTranslationActionFactory from)
	{
		return from.getActionType().getName();
	}
}
