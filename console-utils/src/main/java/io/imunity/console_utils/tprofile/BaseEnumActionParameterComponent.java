/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console_utils.tprofile;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.Binder;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

/**
 * {@link ComboBox} based editor of all enumerated parameters.
 */
public class BaseEnumActionParameterComponent extends ComboBox<String> implements ActionParameterComponent
{
	private final List<String> values;
	private Binder<StringValueBean> binder;
	private String label;
	
	public BaseEnumActionParameterComponent(ActionParameterDefinition desc, MessageSource msg, 
			Collection<?> vals)
	{
		values = vals.stream().map(Object::toString).collect(Collectors.toList());
		setItems(values);
		String def = values.isEmpty() ? null : values.iterator().next().toString();

		initCommon(desc, msg, def, desc.isMandatory());
	}

	public BaseEnumActionParameterComponent(ActionParameterDefinition desc, MessageSource msg, 
			Object[] vals)
	{
		values = Arrays.stream(vals).map(Object::toString).collect(Collectors.toList());
		setItems(values);
		String def = values.isEmpty() ? null : values.iterator().next();
		initCommon(desc, msg, def, desc.isMandatory());
	}
	
	protected final void initCommon(ActionParameterDefinition desc, MessageSource msg,
			String def, boolean mandatory)
	{
		setRequired(mandatory);
		binder = new Binder<>(StringValueBean.class);
		if (mandatory)
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
					.withValidator(v -> values.contains(v), msg.getMessage(
							"TranslationProfileEditor.outdatedValue",
							desc.getName()))
					.bind("value");
		} else
		{
			binder.forField(this)
					.withValidator(v -> v == null || values.contains(v), msg.getMessage(
							"TranslationProfileEditor.outdatedValue",
							desc.getName()))
					.bind("value");
		}	
		binder.setBean(new StringValueBean(def));
		setTooltipText(msg.getMessage(desc.getDescriptionKey()));
		setLabel(desc.getName() + ":");
		setWidth(TEXT_FIELD_MEDIUM.value());
	}
	
	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
	}

	@Override
	public void setLabel(String label)
	{
		this.label = label;
		super.setLabel(label);
	}
	
	@Override
	public String getActionValueRepresentation(MessageSource msg)
	{
		String value = binder.getBean().getValue();
		return value == null ? null : getItemLabelGenerator().apply(binder.getBean().getValue());
	}

	/**
	 * Warning: The code copied to {@link AttributeActionParameterComponent#setActionValue(String)}.
	 * It is hard to provide static method for this and Java as no multi inheritance. 
	 */
	@Override
	public void setActionValue(String value)
	{
		binder.setBean(new StringValueBean(value));
		binder.validate();
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

	@Override
	public void setReadOnly(boolean readOnly)
	{
		super.setReadOnly(readOnly);
	}

	@Override
	public String getLabel()
	{
		return label;
	}
}
