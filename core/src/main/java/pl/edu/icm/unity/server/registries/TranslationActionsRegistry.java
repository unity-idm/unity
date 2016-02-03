/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Maintains a simple registry of available {@link TranslationActionFactory}ies.
 * 
 * @author K. Benedyczak
 */
@Component
public class TranslationActionsRegistry extends TypesRegistryBase<TranslationActionFactory>
{
	@Autowired
	public TranslationActionsRegistry(List<TranslationActionFactory> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(TranslationActionFactory from)
	{
		return from.getActionType().getName();
	}
}
