/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_classes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.attribute.AttributeFieldWithEdit;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.*;


public class RequiredAttributesDialog extends DialogWithActionFooter
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final Set<String> missingAttributes;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final Collection<AttributeType> attributeTypes;
	private final String group;
	private final Callback callback;
	
	private List<AttributeFieldWithEdit> attrEdits;
	private final String info;

	public RequiredAttributesDialog(MessageSource msg, String info,
			Set<String> missingAttributes, AttributeHandlerRegistry attrHandlerRegistry,
			Collection<AttributeType> attributeTypes, String group, Callback callback,
			NotificationPresenter notificationPresenter)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.missingAttributes = missingAttributes;
		this.info = info;
		this.group = group;
		this.attributeTypes = attributeTypes;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(msg.getMessage("RequiredAttributesDialog.caption"));
		add(getContents());
	}

	protected Component getContents()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		Span infoL = new Span(info);
		infoL.setWidthFull();
		Span spacer = new Span(" ");
		main.add(infoL, spacer);
		
		Map<String, AttributeType> typesMap = new HashMap<String, AttributeType>();
		for (AttributeType at: attributeTypes)
			typesMap.put(at.getName(), at);
		attrEdits = new ArrayList<>(missingAttributes.size());
		for (String a: missingAttributes)
		{
			AttributeFieldWithEdit aEdit = new AttributeFieldWithEdit(msg, a, 
					attrHandlerRegistry, attributeTypes, group, null, true, notificationPresenter);
			aEdit.setFixedType(typesMap.get(a));
			attrEdits.add(aEdit);
			main.add(aEdit);
		}
		
		return main;
	}
	
	protected void onCancel()
	{
		callback.onCancel();
	}
	
	protected void onConfirm()
	{
		List<Attribute> ret = new ArrayList<>(attrEdits.size());
		for (AttributeFieldWithEdit aEdit: attrEdits)
		{
			Attribute a;
			try
			{
				a = aEdit.getAttribute();
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"),
						msg.getMessage("RequiredAttributesDialog.someAttributesUnset"));
				return;
			}
			ret.add(a);
		}
		callback.onConfirm(ret);
		close();
	}
	
	public interface Callback
	{
		void onConfirm(List<Attribute> attributes);
		void onCancel();
	}
}
