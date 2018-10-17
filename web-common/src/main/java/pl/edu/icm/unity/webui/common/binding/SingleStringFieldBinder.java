/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.binding;

import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Extension of {@link Binder} allowing for robust creation of binders for components with a single field
 * of string content. Simplifies validation, and setup up of required constraint.
 * 
 * @author K. Benedyczak
 */
public class SingleStringFieldBinder extends Binder<StringBindingValue>
{
	private UnityMessageSource msg;

	public SingleStringFieldBinder(UnityMessageSource msg)
	{
		super(StringBindingValue.class);
		this.msg = msg;
	}
	
	public BindingBuilder<StringBindingValue, String> forField(HasValue<String> field, boolean required)
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