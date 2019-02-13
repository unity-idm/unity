/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Presents a {@link TranslationAction}.
 * 
 * @author K. Benedyczak
 */
public class TranslationActionPresenter extends LayoutEmbeddable
{	
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	
	public TranslationActionPresenter(UnityMessageSource msg, 
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry,
			TranslationAction action)
	{
		this.msg = msg;
		this.registry = registry;
		setInput(action);
	}

	private void setInput(TranslationAction action)
	{       
		String actionName = action.getName();
		ActionParameterDefinition[] pd = null;
		try 
		{
			TranslationActionFactory<?> f = registry.getByName(actionName);
			pd = f.getActionType().getParameters();
		} catch (IllegalArgumentException e)
		{
			throw new InternalException("The action " + actionName + 
					" is unsupported", e);
		}
		addField(msg.getMessage("TranslationActionPresenter.action"),
				"TranslationActionPresenter.codeValue", 
				actionName);
		String[] par = action.getParameters();
		for (int j = 0; j < pd.length && j < par.length; j++)
		{
			if (j == 0)
			{
				addField(msg.getMessage("TranslationActionPresenter.actionParameters"),
						"TranslationActionPresenter.actionParameter",
						pd[j].getName(), getParamValue(pd[j], par[j]));
			}else
			{
				addField("", "TranslationActionPresenter.actionParameter",
						pd[j].getName(), getParamValue(pd[j], par[j]));
			}
		}		
	}

	protected void addField(String name, String msgKey, Object... unsafeArgs)
	{
		HtmlLabel val = new HtmlLabel(msg);
		val.setCaption(name);
		val.setHtmlValue(msgKey, unsafeArgs);
		addComponent(val);
	}
	
	private Object getParamValue(ActionParameterDefinition desc, String value)
	{
		if (value == null)
			return "";
		return value.replace("\n", " | ");
	}
}
