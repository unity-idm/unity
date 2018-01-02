/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Collection;

import com.vaadin.data.Binder;
import com.vaadin.server.UserError;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox2;

/**
 * {@link AttributeSelectionComboBox2} based editor of attribute  parameter.
 * @author K. Benedyczak
 */
public class AttributeActionParameterComponent extends AttributeSelectionComboBox2 implements ActionParameterComponent
{
	private UnityMessageSource msg;
	private ActionParameterDefinition desc;
	private Binder<StringValueBean> binder;

	public AttributeActionParameterComponent(ActionParameterDefinition desc,
			UnityMessageSource msg, Collection<AttributeType> attributeTypes)
	{
		super(desc.getName() + ":", attributeTypes);
		this.msg = msg;
		this.desc = desc;
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(v -> v.getName(), v -> attributeTypesByName.get(v))
				.bind("value");
		binder.setBean(new StringValueBean(getValue().getName()));
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
		if (!attributeTypesByName.keySet().contains(value) && value != null)
		{
			String def = attributeTypesByName.keySet().iterator().next();
			setComponentError(new UserError(
					msg.getMessage("TranslationProfileEditor.outdatedValue",
							value, def, desc.getName())));
			value = def;
		}
		binder.setBean(new StringValueBean(value));
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
