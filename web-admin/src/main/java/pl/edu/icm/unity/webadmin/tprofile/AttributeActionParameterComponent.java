/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Collection;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;

import com.vaadin.server.UserError;
import com.vaadin.ui.ComboBox;

/**
 * {@link ComboBox} based editor of attribute  parameter.
 * @author K. Benedyczak
 */
public class AttributeActionParameterComponent extends AttributeSelectionComboBox implements ActionParameterComponent
{
	private UnityMessageSource msg;
	private ActionParameterDesc desc;
	
	public AttributeActionParameterComponent(ActionParameterDesc desc, UnityMessageSource msg, 
			Collection<AttributeType> attributeTypes)
	{
		super(desc.getName() + ":", attributeTypes);
		this.msg = msg;
		this.desc = desc;
		setRequired(true);
		setDescription(msg.getMessage(desc.getDescriptionKey()));
	}
	
	
	@Override
	public String getActionValue()
	{
		return (String) getSelectedValue().getName();
	}

	/**
	 * Warning: The code copied from {@link BaseEnumActionParameterComponent#setActionValue(String)}.
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
