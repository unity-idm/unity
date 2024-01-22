/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;


import static io.imunity.vaadin.endpoint.common.HtmlLabelFactory.getHtmlLabel;

import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

public class TranslationActionPresenter extends LayoutEmbeddable
{	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, TranslationActionPresenter.class);
	
	private final MessageSource msg;
	private final TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	
	public TranslationActionPresenter(MessageSource msg, 
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
		addComponent(getHtmlLabel(msg, name, msgKey, unsafeArgs));
	}
	
	private Object getParamValue(ActionParameterDefinition desc, String value)
	{
		if (value == null)
			return "";
		try
		{
			value = Constants.MAPPER.readValue(value, I18nString.class).getDefaultLocaleValue(msg);
		} catch (Exception e)
		{
			log.error("Can not parse i18n string", e);
		}
		
		return value.replace("\n", " | ");
	}
}
