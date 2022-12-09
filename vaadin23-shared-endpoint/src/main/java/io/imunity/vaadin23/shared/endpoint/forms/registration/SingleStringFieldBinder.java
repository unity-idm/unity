/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.registration;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin23.shared.endpoint.components.StringBindingValue;
import pl.edu.icm.unity.MessageSource;

import java.util.function.Supplier;


public class SingleStringFieldBinder extends Binder<StringBindingValue>
{
	private MessageSource msg;

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
	
	public void ensureValidity(Supplier<RuntimeException> errorProvider)
	{
		if (!isValid())
		{	
			validate();
			throw errorProvider.get();
		}
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