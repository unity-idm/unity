/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import java.util.Collection;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;

/**
 * {@link AttributeSelectionComboBox} based editor of attribute  parameter.
 * @author K. Benedyczak
 */
public class AttributeActionParameterComponent extends AttributeSelectionComboBox implements ActionParameterComponent
{
	private Binder<StringValueBean> binder;

	public AttributeActionParameterComponent(ActionParameterDefinition desc,
			UnityMessageSource msg, Collection<AttributeType> attributeTypes)
	{
		super(desc.getName() + ":", attributeTypes);
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		
		if (desc.isMandatory())
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(v -> v.getName(),
							v -> attributeTypesByName.get(v) != null
									? attributeTypesByName
											.get(v)
									: new AttributeType(v,
											null))
					.withValidator(v -> attributeTypesByName.keySet()
							.contains(v),
							msg.getMessage("TranslationProfileEditor.outdatedValue",
									desc.getName()))
					.bind("value");
			binder.setBean(new StringValueBean(
					getValue() != null ? getValue().getName() : null));

		} else
		{
			binder.forField(this).withConverter(v -> v != null ? v.getName() : null,
					v -> v != null ? attributeTypesByName.get(v) != null
							? attributeTypesByName.get(v)
							: new AttributeType(v, null) : null)
					.withValidator(v -> v != null ? attributeTypesByName.keySet()
							.contains(v) : true,
							msg.getMessage("TranslationProfileEditor.outdatedValue",
									desc.getName()))
					.bind("value");
			binder.setBean(new StringValueBean());
		}

	}

	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
	}

	/**
	 * Warning: The code copied from
	 * {@link BaseEnumActionParameterComponent#setActionValue(String)}. It
	 * is hard to provide static method for this and Java as no multi
	 * inheritance.
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
		binder.addValueChangeListener((e) -> {
			callback.run();
		});

	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
