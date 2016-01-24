/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;

/**
 * {@link FormLayout} presenting a {@link TranslationAction}.
 * Can be treated as {@link ComponentContainer} to retrieve a list of member elements to be added individually
 * to a parent component.
 * @author K. Benedyczak
 */
public class TranslationActionPresenter extends FormLayout
{	
	private UnityMessageSource msg;
	private TranslationActionsRegistry registry;
	
	public TranslationActionPresenter(UnityMessageSource msg, TranslationActionsRegistry registry,
			TranslationAction action)
	{
		this.msg = msg;
		this.registry = registry;
		setInput(action);
	}

	private void setInput(TranslationAction action)
	{       
		String actionName = action.getActionDescription().getName();
		ActionParameterDesc[] pd = null;
		try 
		{
			TranslationActionFactory f = registry.getByName(actionName);
			pd = f.getParameters();
		} catch (IllegalTypeException e)
		{
			throw new InternalException("The action " + actionName + 
					" is unsupported", e);
		}
		addField(msg.getMessage("TranslationActionPresenter.action"),
				"TranslationActionPresenter.codeValue", 
				actionName);
		String[] par = action.getParameters();
		for (int j = 0; j < par.length; j++)
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
	
	private Object getParamValue(ActionParameterDesc desc, String value)
	{
		if (value == null)
			return "";
		return value.replace("\n", " | ");
	}
}
