/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

import java.util.ArrayList;
import java.util.List;

class SelectableAttributeWithValues extends VerticalLayout
{
	protected ListOfSelectableElements listOfValues;
	private final Attribute attribute;
	private final WebAttributeHandler webHandler;
	private final AttributeValueSyntax<?> syntax;
	private final String customAttrName;
	private final String customAttrDesc;
	private final boolean enableSelect;
	
	SelectableAttributeWithValues(Attribute attribute, String customAttrName, String customAttrDesc,
	                              boolean enableSelect, AttributeType at,
	                              WebAttributeHandler webHandler, MessageSource msg,
	                              AttributeTypeSupport aTypeSupport)
	{
		this.attribute = attribute;
		this.webHandler = webHandler;
		syntax = aTypeSupport.getSyntaxFallingBackToDefault(attribute);
		this.customAttrName = customAttrName;
		this.customAttrDesc = customAttrDesc;
		this.enableSelect = enableSelect;
		initUI();
	}
	
	private void initUI()
	{
		setPadding(false);
		setSpacing(false);

		Label attrNameLabel = new Label(customAttrName);
		attrNameLabel.getElement().setProperty("title", customAttrDesc);
		add(attrNameLabel);
		
		listOfValues = new ListOfSelectableElements(null, null, ListOfSelectableElements.DisableMode.WHEN_DESELECTED);
		
		listOfValues.setWidthFull();
		for (String value: attribute.getValues())
		{
			Component representation = webHandler.getRepresentation(value, AttributeViewerContext.EMPTY);
			((HasStyle)representation).getStyle().set("width", "100%");
			listOfValues.addEntry(representation, true);
		}
		listOfValues.setCheckBoxesVisible(enableSelect);
		
		if (!attribute.getValues().isEmpty())
			add(listOfValues);
	}
	
	/**
	 * Hides some selected values or, if the argument is null, then the whole attribute is marked as hidden.
	 */
	void setHiddenValues(Attribute hiddenValues)
	{
		//cannot hide if selecting is not enable
		if (!enableSelect)
			return;
		
		List<Checkbox> selection = listOfValues.getSelection();
		if (hiddenValues == null)
		{
			for (Checkbox sel: selection)
				sel.setValue(true);
			return;
		}
		for (String svalue: hiddenValues.getValues())
		{
			int i=0;
			for (String value: attribute.getValues())
			{
				if (syntax.areEqualStringValue(value, svalue))
				{
					selection.get(i).setValue(false);
					break;
				}
				i++;
			}
		}
	}
	
	/**
	 * @return whether an attribute is completely hidden
	 */
	boolean isHidden()
	{
		return getWithoutHiddenValues() == null;
	}
	
	/**
	 * @return the attribute with only hidden values. Null if the attribute is not hidden.
	 */
	Attribute getHiddenValues()
	{
		return isAnythingHidden() ? getAttribute(true) : null;
	}
	
	private boolean isAnythingHidden()
	{
		List<Checkbox> selection = listOfValues.getSelection();
		for (Checkbox checkbox: selection)
			if (!checkbox.getValue())
				return true;
		return false;
	}
		
	/**
	 * @return the attribute without any hidden value. Null if the whole attribute is hidden.
	 */
	Attribute getWithoutHiddenValues()
	{
		Attribute attr = getAttribute(false);
		return attr.getValues().isEmpty() ? null : attr;
	}

	private Attribute getAttribute(boolean hidden)
	{
		List<Checkbox> selection = listOfValues.getSelection();
		List<String> filteredValues = new ArrayList<>(attribute.getValues().size());
		for (int i = 0; i < attribute.getValues().size(); i++)
		{
			String t = attribute.getValues().get(i);
			if (selection.get(i).getValue() != hidden)
				filteredValues.add(t);
		}
		return new Attribute(attribute.getName(), attribute.getValueSyntax(), 
				attribute.getGroupPath(), filteredValues);
	}
}
