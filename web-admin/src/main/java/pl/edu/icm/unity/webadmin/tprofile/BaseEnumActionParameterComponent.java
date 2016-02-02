/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Collection;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

import com.vaadin.server.UserError;
import com.vaadin.ui.ComboBox;

/**
 * {@link ComboBox} based editor of all enumerated parameters.
 * @author K. Benedyczak
 */
public class BaseEnumActionParameterComponent extends ComboBox implements ActionParameterComponent
{
	private UnityMessageSource msg;
	private ActionParameterDefinition desc;
	
	public BaseEnumActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, 
			Collection<?> values)
	{
		for (Object o: values)
			addItem(o.toString());
		String def = values.isEmpty() ? null : values.iterator().next().toString(); 
		initCommon(desc, msg, def);
	}

	public BaseEnumActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, 
			Object[] values)
	{
		for (Object o: values)
			addItem(o.toString());
		String def = values.length == 0 ? null : values[0].toString(); 
		initCommon(desc, msg, def);
	}
	
	protected final void initCommon(ActionParameterDefinition desc, UnityMessageSource msg, String def)
	{
		this.msg = msg;
		this.desc = desc;
		setNullSelectionAllowed(false);
		if (def != null)
			select(def);
		setRequired(true);
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setCaption(desc.getName() + ":");
	}
	
	@Override
	public String getActionValue()
	{
		return (String) getValue();
	}

	/**
	 * Warning: The code copied to {@link AttributeActionParameterComponent#setActionValue(String)}.
	 * It is hard to provide static method for this and Java as no multi inheritance. 
	 */
	@Override
	public void setActionValue(String value)
	{
		if (!getItemIds().contains(value) && value != null)
		{
			String def = (String) getItemIds().iterator().next();
			setComponentError(new UserError(msg.getMessage("TranslationProfileEditor.outdatedValue", 
					value, def, desc.getName())));
			setValidationVisible(true);
			value = def;
		}
		select(value);
	}
}
