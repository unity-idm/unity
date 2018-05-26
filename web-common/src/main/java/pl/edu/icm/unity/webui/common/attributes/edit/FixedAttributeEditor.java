/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.edit;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.AbstractOrderedLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

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
	private AbstractOrderedLayout parent;

	public FixedAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry,
			AttributeEditContext editContext, boolean showGroup, 
			String caption, String description, 
			AbstractOrderedLayout parent)
	{
		this.msg = msg;
		this.registry = registry;
		this.showGroup = showGroup;
		this.caption = caption;
		this.description = description;
		this.parent = parent;
		this.editContext = editContext;
		initUI();
	}
	
	public void setAttributeValues(List<String> values)
	{
		List<LabelledValue> labelledValues = new ArrayList<>(values.size());
		for (String value: values)
			labelledValues.add(new LabelledValue(value, caption));
		valuesComponent.setEntries(labelledValues);
	}
	
	public AttributeType getAttributeType()
	{
		return editContext.getAttributeType();
	}

	public String getGroup()
	{
		return editContext.getAttributeGroup();
	}

	public Attribute getAttribute() throws FormValidationException
	{
		List<LabelledValue> values = valuesComponent.getElements();
		List<String> aValues = new ArrayList<>(values.size());
		boolean allNull = true;
		for (LabelledValue v: values)
		{
			aValues.add(v.getValue());
			if (v.getValue() != null)
				allNull = false;
		}
		
		return allNull ? null
				: new Attribute(editContext.getAttributeType().getName(),
						editContext.getAttributeType().getValueSyntax(),
						editContext.getAttributeGroup(), aValues);
	}
	
	private void initUI()
	{
		if (caption == null)
		{
			caption = editContext.getAttributeType().getDisplayedName().getValue(msg);
			String group = editContext.getAttributeGroup(); 
			if (showGroup && !group.equals("/"))
				caption = caption + " @" + group;
			caption = caption + ":";
		}
		if (description == null)
			description = editContext.getAttributeType().getDescription().getValue(msg);
		
		valuesComponent = getValuesPart(editContext, caption, parent);
	}
	
	public void clear()
	{
		valuesComponent.clearContents();
	}
	
	private ListOfEmbeddedElementsStub<LabelledValue> getValuesPart(
			AttributeEditContext editContext, String label,
			AbstractOrderedLayout layout)
	{
		ListOfEmbeddedElementsStub<LabelledValue> ret = new ListOfEmbeddedElementsStub<>(
				msg, new InternalAttributeValueEditor.Factory(msg, registry, label, editContext),
				editContext.getAttributeType().getMinElements(),
				editContext.getAttributeType().getMaxElements(), false, layout);
		ret.setLonelyLabel(label);
		return ret;
	}
	
}
