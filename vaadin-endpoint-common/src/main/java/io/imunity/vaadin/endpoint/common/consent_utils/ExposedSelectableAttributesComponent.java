/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.webui.idpcommon.SelectableAttributesComponent;

import java.util.*;
import java.util.Map.Entry;

/**
 * Component showing all attributes that are going to be sent to the requesting service. User
 * can select attributes which should be hidden.
 * By default attributes are collapsed.
 * @author K. Benedyczak
 */
public class ExposedSelectableAttributesComponent extends Details implements SelectableAttributesComponent
{
	private final MessageSource msg;
	private final AttributeHandlerRegistry handlersRegistry;
	
	private final Map<String, DynamicAttribute> attributes;
	private Map<String, SelectableAttributeWithValues> attributesHiding;
	private final AttributeTypeSupport aTypeSupport;
	private final Optional<IdentityParam> selectedIdentity;
	private final IdentityPresentationUtil identityPresenter;
	

	public ExposedSelectableAttributesComponent(MessageSource msg,
	                                            IdentityTypeSupport idTypeSupport, AttributeHandlerRegistry handlersRegistry,
	                                            AttributeTypeSupport aTypeSupport,
	                                            Collection<DynamicAttribute> attributesCol,
	                                            Optional<IdentityParam> selectedIdentity)
	{
		this.identityPresenter = new IdentityPresentationUtil(msg, idTypeSupport);
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;
		this.aTypeSupport = aTypeSupport;
		this.selectedIdentity = selectedIdentity;

		attributes = new HashMap<>();
		for (DynamicAttribute a: attributesCol)
			attributes.put(a.getAttribute().getName(), a);
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	@Override
	public Collection<Attribute> getUserFilteredAttributes()
	{
		List<Attribute> ret = new ArrayList<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
			if (!entry.getValue().isHidden())
				ret.add(entry.getValue().getWithoutHiddenValues());
		return ret;
	}

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	@Override
	public Map<String, Attribute> getHiddenAttributes()
	{
		Map<String, Attribute> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
		{
			Attribute hiddenValues = entry.getValue().getHiddenValues();
			if (hiddenValues != null)
				ret.put(entry.getKey(), hiddenValues);
		}
		return ret;
	}
	
	@Override
	public void setInitialState(Map<String, Attribute> savedState)
	{
		for (Entry<String, Attribute> entry : savedState.entrySet())
		{
			SelectableAttributeWithValues selectableAttributeWithValues = 
					attributesHiding.get(entry.getKey());
			if (selectableAttributeWithValues != null)
				selectableAttributeWithValues.setHiddenValues(entry.getValue());
		}
	}
	
	private void initUI()
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setPadding(false);
		setSummaryText(msg.getMessage("ExposedAttributesComponent.attributes"));

		Span attributesInfo = new Span(msg.getMessage("ExposedAttributesComponent.attributesInfo"));
		attributesInfo.setWidthFull();

		Span credInfo = new Span(msg.getMessage("ExposedAttributesComponent.credInfo"));
		add(new VerticalLayout(getIdentity(), attributesInfo, getAttributesListComponent(), credInfo));
	}
	
	private Component getIdentity()
	{
		if (selectedIdentity.isEmpty())
			return new Div();
		IdentityParam id = selectedIdentity.get();
		return getIdentityTF(id);
	}
	
	private Component getIdentityTF(IdentityParam identity)
	{
		Span title = new Span(msg.getMessage("IdentitySelectorComponent.identity"));
		title.getStyle().set("font-weight", "bold");
		Span content = new Span(identityPresenter.getIdentityVisualValue(identity));
		if (!content.getText().equals(identity.getValue()))
		{
			Tooltip.forComponent(content).setText(msg.getMessage(
					"IdentitySelectorComponent.fullValue", identity.getValue()));
		}
		VerticalLayout verticalLayout = new VerticalLayout(title, content);
		verticalLayout.setMargin(false);
		verticalLayout.setPadding(false);
		return verticalLayout;
	}

	
	private Component getAttributesListComponent()
	{
		VerticalLayout attributesList = new VerticalLayout();
		attributesList.setPadding(false);
		attributesList.getStyle().set("gap", "0.4em");

		attributesHiding = new HashMap<>();
		for (DynamicAttribute dat: attributes.values())
		{
			SelectableAttributeWithValues attributeComponent = getAttributeComponent(dat);
			attributesHiding.put(dat.getAttribute().getName(), attributeComponent);
			attributesList.add(attributeComponent);
		}
		
		return attributesList;	
		
	}
	
	private SelectableAttributeWithValues getAttributeComponent(DynamicAttribute dat)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		
		WebAttributeHandler handler;
		handler = handlersRegistry.getHandlerWithStringFallback(attributeType);
		
		SelectableAttributeWithValues attributeComponent = new SelectableAttributeWithValues(
				at, dat.getDisplayedName(),
				dat.getDescription(), !dat.isMandatory(),
				handler, aTypeSupport);
		attributeComponent.setWidthFull();
		
		return attributeComponent;
	
	}
}
