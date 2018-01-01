/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextArea;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Trivial, {@link TextArea} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class TextAreaActionParameterComponent extends TextArea implements ActionParameterComponent
{
	private String value;
	private Binder<String> binder;
	
	public TextAreaActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));
//		setColumns(Styles.WIDE_TEXT_FIELD);
		binder = new Binder<>(String.class);
		binder.forField(this).bind(v -> this.value, (c, v) -> {
				this.value = v;
			});
		binder.setBean(this.value);	
	}
	
	@Override
	public String getActionValue()
	{
		return this.value;
	}

	@Override
	public void setActionValue(String value)
	{
		this.value = value;
		binder.setBean(this.value);
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
