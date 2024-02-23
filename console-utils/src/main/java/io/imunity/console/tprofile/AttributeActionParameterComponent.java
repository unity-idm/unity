/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.VaadinElementReadOnlySetter;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

import java.util.Collection;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

/**
 * {@link AttributeSelectionComboBox} based editor of attribute  parameter.
 * @author K. Benedyczak
 */
public class AttributeActionParameterComponent extends AttributeSelectionComboBox implements ActionParameterComponent
{
	private final Binder<StringValueBean> binder;
	private String caption;

	
	public AttributeActionParameterComponent(ActionParameterDefinition desc,
			MessageSource msg, Collection<AttributeType> attributeTypes)
	{
		super(desc.getName() + ":", attributeTypes);
		setWidth(TEXT_FIELD_MEDIUM.value());
		setTooltipText(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		
		if (desc.isMandatory())
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(AttributeType::getName,
							v -> attributeTypesByName.get(v) != null
									? attributeTypesByName
											.get(v)
									: new AttributeType(v,
											null))
					.withValidator(v -> attributeTypesByName
							.containsKey(v),
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
					.withValidator(v -> v == null || attributeTypesByName
									.containsKey(v),
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
		binder.addValueChangeListener((e) -> callback.run());
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
		VaadinElementReadOnlySetter.setReadOnly(getElement(), readOnly);
	}

	@Override
	public void setLabel(String label)
	{
		if(caption == null)
			caption = label;
		super.setLabel(label);
	}
	
	@Override
	public String getCaption()
	{
		return caption;
	}
}
