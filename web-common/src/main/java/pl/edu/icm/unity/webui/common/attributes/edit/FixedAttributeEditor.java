/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.ui.AbstractOrderedLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.composite.ComponentsGroup;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter;

/**
 * Attribute editor allowing to edit a fixed attribute type. It can show the (also fixed) group 
 * or not. The initial values can be optionally set.
 * <p>
 * This class is not a component on its own - instead it can be added to a parent container.
 * 
 * @author K. Benedyczak
 */
public class FixedAttributeEditor
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private String caption;
	private String description;
	private boolean showGroup;
	private AttributeEditContext editContext;
	private ListOfEmbeddedElementsStub<LabelledValue> valuesComponent;
	private List<String> originalValues;

	public FixedAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry,
			AttributeEditContext editContext, boolean showGroup, 
			String caption, String description)
	{
		this.msg = msg;
		this.registry = registry;
		this.showGroup = showGroup;
		this.caption = caption;
		this.description = description;
		this.editContext = editContext;
		initUI();
	}
	
	public void placeOnLayout(AbstractOrderedLayout layout)
	{
		new CompositeLayoutAdapter(layout, getComponentsGroup());
	}
	
	public void setAttributeValues(List<String> values)
	{
		this.originalValues = new ArrayList<>(values);
		List<LabelledValue> labelledValues = new ArrayList<>(values.size());
		for (String value: values)
			labelledValues.add(new LabelledValue(value, caption));
		valuesComponent.setEntries(labelledValues);
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
		if (!current.isPresent())
			return !originalValues.isEmpty();
		return !originalValues.equals(current.get().getValues());
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

		List<String> aValues = values.stream().map(lv -> lv.getValue()).collect(Collectors.toList());
		return Optional.of(new Attribute(editContext.getAttributeType().getName(),
					editContext.getAttributeType().getValueSyntax(),
					editContext.getAttributeGroup(), aValues));
	}
	
	private void initUI()
	{
		if (caption == null)
		{
			caption = editContext.getAttributeType().getDisplayedName().getValue(msg);
			String group = editContext.getAttributeGroup(); 
			if (showGroup && !group.equals("/"))
				caption = caption + " @" + group;
			if (!editContext.isShowLabelInline())
				caption += ":";
		}
		if (description == null)
			description = editContext.getAttributeType().getDescription().getValue(msg);
		
		valuesComponent = getValuesPart(caption);
	}
	
	public void clear()
	{
		valuesComponent.clearContents();
	}
	
	private ListOfEmbeddedElementsStub<LabelledValue> getValuesPart(String label)
	{
		ListOfEmbeddedElementsStub<LabelledValue> ret = new ListOfEmbeddedElementsStub<>(
				msg, new InternalAttributeValueEditor.Factory(msg, registry, label, editContext),
				editContext.getAttributeType().getMinElements(),
				editContext.getAttributeType().getMaxElements(), false);
		ret.setLonelyLabel(label);
		return ret;
	}
	
}
