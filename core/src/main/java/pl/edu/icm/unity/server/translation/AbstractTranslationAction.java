/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;


/**
 * Minimal base for translation actions.
 * @author K. Benedyczak
 */
public abstract class AbstractTranslationAction implements TranslationAction
{
	protected TranslationActionDescription description;
	protected String[] params;
	
	public AbstractTranslationAction(TranslationActionDescription description, String[] params)
	{
		this.description = description;
		this.params = params;
	}

	@Override
	public TranslationActionDescription getActionDescription()
	{
		return description;
	}
	
	@Override
	public String[] getParameters()
	{
		return params;
	}
}
