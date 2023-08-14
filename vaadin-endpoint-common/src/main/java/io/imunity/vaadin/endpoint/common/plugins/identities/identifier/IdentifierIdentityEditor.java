/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.identifier;

import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;


public class IdentifierIdentityEditor implements IdentityEditor
{
	private final MessageSource msg;
	private TextField field;
	private SingleStringFieldBinder binder;
	
	public IdentifierIdentityEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		binder = new SingleStringFieldBinder(msg);
		field = new TextField();
		setLabel(new IdentifierIdentity().getHumanFriendlyName(msg));
		if (context.isCustomWidth())
			field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		if(context.isRequired())
		{
			field.setRequiredIndicatorVisible(true);
			field.setTooltipText(msg.getMessage("fieldRequired"));
		}
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
