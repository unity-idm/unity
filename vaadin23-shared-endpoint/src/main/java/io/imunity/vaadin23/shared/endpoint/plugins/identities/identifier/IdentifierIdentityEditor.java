/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.identities.identifier;

import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.forms.registration.SingleStringFieldBinder;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorContext;
import io.imunity.vaadin23.shared.endpoint.components.StringBindingValue;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class IdentifierIdentityEditor implements IdentityEditor
{
	private final MessageSource msg;
	private TextField field;
	private IdentityEditorContext context;
	private SingleStringFieldBinder binder;
	
	public IdentifierIdentityEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		binder = new SingleStringFieldBinder(msg);
		this.context = context;
		field = new TextField();
		setLabel(new IdentifierIdentity().getHumanFriendlyName(msg));
		if (context.isCustomWidth())
			field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		
		binder.forField(field, context.isRequired()).bind("value");
		binder.setBean(new StringBindingValue(""));
		
		return new ComponentsContainer(field);
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		binder.ensureValidityCatched(() -> new IllegalIdentityValueException(""));
		String value = binder.getBean().getValue().trim();
		if (value.isEmpty())
			return null;
		return new IdentityParam(IdentifierIdentity.ID, value);
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		binder.setBean(new StringBindingValue(value.getValue()));
	}
	
	@Override
	public void setLabel(String value)
	{
			field.setLabel(value + ":");
	}
}
