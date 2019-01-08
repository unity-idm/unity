/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.binding.SingleStringFieldBinder;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;

/**
 * {@link IdentifierIdentity} editor
 * @author K. Benedyczak
 */
public class IdentifierIdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private IdentityEditorContext context;
	private SingleStringFieldBinder binder;
	
	public IdentifierIdentityEditor(UnityMessageSource msg)
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
		if (context.isShowLabelInline())
			field.setPlaceholder(value);
		else
			field.setCaption(value + ":");
	}
}
