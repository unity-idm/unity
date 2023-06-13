/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.components;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.StringBindingValue;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.function.Supplier;


public class SingleStringFieldBinder extends Binder<StringBindingValue>
{
	private final MessageSource msg;

	public SingleStringFieldBinder(MessageSource msg)
	{
		super(StringBindingValue.class);
		this.msg = msg;
	}
	
	public BindingBuilder<StringBindingValue, String> forField(HasValue<?, String> field, boolean required)
	{
		BindingBuilder<StringBindingValue, String> bindingBuilder = forField(field);
		if (required)
			bindingBuilder.asRequired(msg.getMessage("fieldRequired"));
		return bindingBuilder;
	}

	public <T extends Exception> void ensureValidityCatched(Supplier<T> errorProvider) throws T
	{
		if (!isValid())
		{	
			validate();
			throw errorProvider.get();
		}
	}
}