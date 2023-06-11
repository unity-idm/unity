/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.username;

import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * {@link UsernameIdentity} editor
 * @author K. Benedyczak
 */
public class UsernameIdentityEditor implements IdentityEditor
{
	private final MessageSource msg;
	private TextField field;
	private IdentityEditorContext context;
	private SingleStringFieldBinder binder;
	
	public UsernameIdentityEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		this.context = context;
		field = new TextField();
		setLabel(new UsernameIdentity().getHumanFriendlyName(msg));
		field.setId("UsernameIdentityEditor.username");
		if (context.isCustomWidth())
			field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		if(context.isRequired())
		{
			field.setRequiredIndicatorVisible(true);
			field.getElement().setProperty("title", msg.getMessage("fieldRequired"));
		}
		binder = new SingleStringFieldBinder(msg);
		binder.forField(field, context.isRequired()).bind("value");
		binder.setBean(new StringBindingValue(""));
		
		return new ComponentsContainer(field);
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		binder.ensureValidityCatched(() -> new IllegalIdentityValueException(""));
		String username = field.getValue().trim();
		if (username.isEmpty())
			return null;
		return new IdentityParam(UsernameIdentity.ID, username);
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		binder.setBean(new StringBindingValue(value.getValue()));
	}
	

	@Override
	public void setLabel(String value)
	{
		if (context.isShowLabelInline())
			field.setPlaceholder(value);
		else
			field.setLabel(value + ":");
	}
}
