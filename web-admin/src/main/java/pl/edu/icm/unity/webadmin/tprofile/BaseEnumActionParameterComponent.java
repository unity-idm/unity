/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.server.UserError;
import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * {@link ComboBox} based editor of all enumerated parameters.
 * @author K. Benedyczak
 */
public class BaseEnumActionParameterComponent extends ComboBox<String> implements ActionParameterComponent
{
	private UnityMessageSource msg;
	private ActionParameterDefinition desc;
	private String selectedValue;
	private List<String> values;
	private Binder<String> binder;
	
	public BaseEnumActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, 
			Collection<?> vals)
	{
		values = vals.stream().map(v -> v.toString()).collect(Collectors.toList());
		setItems(values);
		String def = values.isEmpty() ? null : values.iterator().next().toString(); 
		
		initCommon(desc, msg, def);
	}

	public BaseEnumActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, 
			Object[] vals)
	{
		values = Arrays.stream(vals).map(v -> v.toString()).collect(Collectors.toList());
		setItems(values);
		String def = values.isEmpty() ? null : values.iterator().next().toString(); 
		initCommon(desc, msg, def);
	}
	
	protected final void initCommon(ActionParameterDefinition desc, UnityMessageSource msg,
			String def)
	{
		this.msg = msg;
		this.desc = desc;
		setEmptySelectionAllowed(false);
		binder = new Binder<>(String.class);
		binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
				.bind(v -> this.selectedValue, (c, v) -> {
					this.selectedValue = v;
				});

		
		selectedValue = def == null ? new String() : def;
		binder.setBean(selectedValue);
		
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setCaption(desc.getName() + ":");
	}
	
	@Override
	public String getActionValue()
	{
		return selectedValue;
	}

	/**
	 * Warning: The code copied to {@link AttributeActionParameterComponent#setActionValue(String)}.
	 * It is hard to provide static method for this and Java as no multi inheritance. 
	 */
	@Override
	public void setActionValue(String value)
	{
		if (!values.contains(value) && value != null)
		{
			String def = (String) values.iterator().next();
			setComponentError(new UserError(msg.getMessage("TranslationProfileEditor.outdatedValue", 
					value, def, desc.getName())));
			value = def;
		}

		selectedValue = value;
		binder.setBean(selectedValue);
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		addValueChangeListener((e) -> { callback.run(); });	
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
