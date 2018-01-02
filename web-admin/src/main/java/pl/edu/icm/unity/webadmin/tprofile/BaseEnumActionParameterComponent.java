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
	private List<String> values;
	private Binder<StringValueBean> binder;
	
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
		binder = new Binder<>(StringValueBean.class);
		binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
				.bind("value");
		binder.setBean(new StringValueBean(def));
		
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setCaption(desc.getName() + ":");
	}
	
	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
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
		binder.setBean(new StringValueBean(value));
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) -> { callback.run(); });	
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
