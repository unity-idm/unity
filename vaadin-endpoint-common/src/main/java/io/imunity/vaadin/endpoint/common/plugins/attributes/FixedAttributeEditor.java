/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FixedAttributeEditor
{
	private final MessageSource msg;
	private final AttributeHandlerRegistry registry;
	private String description;
	private final LabelContext labelContext;
	private final AttributeEditContext editContext;
	private ListOfEmbeddedElementsStub<LabelledValue> valuesComponent;
	private List<String> originalValues;

	public FixedAttributeEditor(MessageSource msg, AttributeHandlerRegistry registry,
	                            AttributeEditContext editContext, LabelContext labelContext,
	                            String description)
	{
		this.msg = msg;
		this.registry = registry;
		this.labelContext = labelContext;
		this.description = description;
		this.editContext = editContext;
		initUI();
	}
	
	public void addValueChangeListener(Runnable listener)
	{
		valuesComponent.setValueChangeListener(listener);
	}
	
	public void setAttributeValues(List<String> values)
	{
		this.originalValues = new ArrayList<>(values);
		List<LabelledValue> labelledValues = new ArrayList<>(values.size());
		for (String value: values)
			labelledValues.add(new LabelledValue(value, labelContext.getLabel()));
		valuesComponent.setEntries(labelledValues);
	}

	public void placeOnLayout(com.vaadin.flow.component.formlayout.FormLayout layout)
	{
		new CompositeLayoutAdapter(layout, getComponentsGroup());
	}

	public void placeOnLayout(VerticalLayout layout)
	{
		new CompositeLayoutAdapter(layout, getComponentsGroup());
	}
	
	public AttributeType getAttributeType()
	{
		return editContext.getAttributeType();
	}

	public ComponentsGroup getComponentsGroup()
	{
		return valuesComponent.getComponentsGroup();
	}
	
	public String getGroup()
	{
		return editContext.getAttributeGroup();
	}

	public boolean isChanged() throws FormValidationException
	{
		Optional<Attribute> current = getAttribute();
		if (originalValues == null)
			return current.isPresent();
		return current.map(attribute -> !originalValues.equals(attribute.getValues()))
				.orElseGet(() -> !originalValues.isEmpty());
	}
	
	public Optional<Attribute> getAttribute() throws FormValidationException
	{
		List<LabelledValue> values = valuesComponent.getElements();
		if (!editContext.isRequired())
		{
			values = values.stream().filter(lv -> lv.getValue() != null && !lv.getValue().isEmpty())
					.collect(Collectors.toList()); 
			if (values.isEmpty())
				return Optional.empty();
		}

		List<String> aValues = values.stream().map(LabelledValue::getValue).collect(Collectors.toList());
		return Optional.of(new Attribute(editContext.getAttributeType().getName(),
					editContext.getAttributeType().getValueSyntax(),
					editContext.getAttributeGroup(), aValues));
	}
	
	private void initUI()
	{
		if (description == null)
			description = editContext.getAttributeType().getDescription().getValue(msg);
		
		valuesComponent = getValuesPart(labelContext.getLabel());
	}
	
	public void clear()
	{
		valuesComponent.clearContents();
	}
	
	private ListOfEmbeddedElementsStub<LabelledValue> getValuesPart(String label)
	{
		ListOfEmbeddedElementsStub<LabelledValue> ret = new ListOfEmbeddedElementsStub<>(
				msg, new InternalAttributeValueEditor.Factory(registry, label, editContext),
				editContext.getAttributeType().getMinElements(),
				editContext.getAttributeType().getMaxElements(), false);
		ret.setLonelyLabel(label);
		return ret;
	}
	
}
