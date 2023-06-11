/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;

import java.util.*;

/**
 * Component showing all attributes that are going to be sent to the requesting
 * service. By default attributes are collapsed.
 * 
 * @author K. Benedyczak
 */
public class ExposedAttributesComponent extends VerticalLayout
{
	private static final String INPUT_WIDTH = "100%";
	private final MessageSource msg;
	private final IdentityPresentationUtil identityPresenter;
	
	protected Map<String, DynamicAttribute> attributes;
	private final AttributeHandlerRegistry handlersRegistry;
	private final Optional<IdentityParam> selectedIdentity;

	public ExposedAttributesComponent(MessageSource msg,
	                                  IdentityTypeSupport idTypeSupport,
	                                  AttributeHandlerRegistry handlersRegistry,
	                                  Collection<DynamicAttribute> attributesCol,
	                                  Optional<IdentityParam> selectedIdentity)
	{
		this.msg = msg;
		this.identityPresenter = new IdentityPresentationUtil(msg, idTypeSupport);
		this.handlersRegistry = handlersRegistry;
		this.selectedIdentity = selectedIdentity;

		attributes = new HashMap<>();
		for (DynamicAttribute a : attributesCol)
			attributes.put(a.getAttribute().getName(), a);
		initUI();
	}

	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public List<DynamicAttribute> getUserFilteredAttributes()
	{
		return new ArrayList<>(attributes.values());
	}

	private void initUI()
	{
		setPadding(false);
		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		Details details = new Details(
				msg.getMessage("ExposedAttributesComponent.attributes"),
				content);
		details.setId("ExposedAttributes.showDetails");
		
		Label credInfo = new Label(msg.getMessage("ExposedAttributesComponent.credInfo"));
		credInfo.setWidthFull();

		addIdentity(content);
		addAttributesList(content);
		content.add(credInfo);
		add(details);
	}

	private void addAttributesList(VerticalLayout attribtuesFL)
	{
		for (DynamicAttribute dat : attributes.values())
		{
			List<Component> components = getAttributeComponent(dat);
			components.forEach(attribtuesFL::add);
			components.forEach(component -> component.getElement().getStyle().set("width", INPUT_WIDTH));
			components.forEach(component -> component.getElement().getStyle().set("padding", "0"));
			components.stream().skip(1).forEach(component -> {
				if(component instanceof HasLabel label)
					label.setLabel("");
			});
		}
	}

	private void addIdentity(VerticalLayout attribtuesFL)
	{
		if (selectedIdentity.isEmpty())
			return;
		IdentityParam id = selectedIdentity.get();
		attribtuesFL.add(getIdentityTF(id));
	}
	
	private Component getIdentityTF(IdentityParam identity)
	{
		TextField identityField = new TextField(msg.getMessage("IdentitySelectorComponent.identity"));
		identityField.setWidth(INPUT_WIDTH);
		identityField.setValue(identityPresenter.getIdentityVisualValue(identity));
		identityField.setReadOnly(true);
		if (!identityField.getValue().equals(identity.getValue()))
		{
			identityField.getElement().setProperty("title", msg.getMessage(
					"IdentitySelectorComponent.fullValue", identity.getValue()));
		}
		return identityField;
	}
	
	private List<Component> getAttributeComponent(DynamicAttribute dat)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		AttributeViewer attrViewer = new AttributeViewer(msg, handlersRegistry,
				attributeType, at, false, AttributeViewerContext.EMPTY);
		return attrViewer.getAsComponents(dat.getDisplayedName(), dat.getDescription());
	}
}
