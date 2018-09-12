/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Displays an attribute name in the first row and all its values in subsequent rows. 
 * For attribute and each its value it is possible to click a check box next to it to hide it.
 * @author K. Benedyczak
 */
public class SelectableAttributeWithValues extends CustomComponent
{
	protected ListOfSelectableElements selectableAttr;
	protected ListOfSelectableElements listOfValues;
	private Attribute attribute;
	private WebAttributeHandler webHandler;
	private Component firstheader;
	private Component secondHeader;
	private AttributeValueSyntax<?> syntax;
	private String customAttrName;
	private String customAttrDesc;
	private boolean enableSelect;
	
	public SelectableAttributeWithValues(Component firstheader, Component secondHeader,
			Attribute attribute, AttributeType at, 
			WebAttributeHandler webHandler, UnityMessageSource msg, 
			AttributeTypeSupport aTypeSupport)
	{
		this.firstheader = firstheader;
		this.secondHeader = secondHeader;
		this.attribute = attribute;
		this.webHandler = webHandler;
		syntax = aTypeSupport.getSyntaxFallingBackToDefault(attribute);
		this.customAttrName = at.getName();
		this.customAttrDesc = at.getDescription() == null ? 
				at.getName():at.getDescription().getValue(msg);
	        this.enableSelect = true;
		initUI();
	}

	public SelectableAttributeWithValues(Component firstheader, Component secondHeader,
			Attribute attribute, String customAttrName, String customAttrDesc,
			boolean enableSelect, AttributeType at,
			WebAttributeHandler webHandler, UnityMessageSource msg, 
			AttributeTypeSupport aTypeSupport)
	{
		this(firstheader, secondHeader, attribute, at, webHandler, msg, aTypeSupport);
		this.customAttrName = customAttrName;
		this.customAttrDesc = customAttrDesc;
		this.enableSelect = enableSelect;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		
		selectableAttr = new ListOfSelectableElements(firstheader, secondHeader, DisableMode.WHEN_SELECTED);
		
		Label attrNameLabel = new Label(customAttrName);
		attrNameLabel.setDescription(customAttrDesc);
		selectableAttr.addEntry(attrNameLabel, false);
		selectableAttr.setWidth(100, Unit.PERCENTAGE);
		final CheckBox mainDisable = selectableAttr.getSelection().iterator().next();
		mainDisable.addValueChangeListener(event -> listOfValues.setEnabled(!mainDisable.getValue()));
		selectableAttr.setCheckBoxesVisible(enableSelect);
		
		
		main.addComponents(selectableAttr);
		
		listOfValues = new ListOfSelectableElements(null, null, DisableMode.WHEN_SELECTED);
		
		listOfValues.setWidth(100, Unit.PERCENTAGE);
		for (String value: attribute.getValues())
		{
			Component representation = webHandler.getRepresentation(value, AttributeViewerContext.EMPTY);
			representation.addStyleName(Styles.indent.toString());
			listOfValues.addEntry(representation, false);
		}
		listOfValues.setCheckBoxesVisible(enableSelect);
		
		if (!attribute.getValues().isEmpty())
			main.addComponent(listOfValues);

		setCompositionRoot(main);
	}
	
	/**
	 * Hides some selected values or, if the argument is null, then the whole attribute is marked as hidden.
	 */
	public void setHiddenValues(Attribute hiddenValues)
	{
		//cannot hide if selecting is not enable
		if (!enableSelect)
			return;
		
		if (hiddenValues == null)
		{
			selectableAttr.getSelection().get(0).setValue(true);
			return;
		}
		List<CheckBox> selection = listOfValues.getSelection();
		for (String svalue: hiddenValues.getValues())
		{
			int i=0;
			for (String value: attribute.getValues())
			{
				if (syntax.areEqualStringValue(value, svalue))
				{
					selection.get(i).setValue(true);
					break;
				}
				i++;
			}
		}
	}
	
	/**
	 * @return whether an attribute is completely hidden
	 */
	public boolean isHidden()
	{
		return selectableAttr.getSelection().get(0).getValue();
	}
	
	/**
	 * @return the attribute with only hidden values. Null if the attribute is not hidden.
	 */
	public Attribute getHiddenValues()
	{
		return isAnythingHidden() ? getAttribute(true) : null;
	}
	
	private boolean isAnythingHidden()
	{
		if (selectableAttr.getSelection().get(0).getValue())
			return true;
		List<CheckBox> selection = listOfValues.getSelection();
		for (CheckBox checkbox: selection)
			if (checkbox.getValue())
				return true;
		return false;
	}
		
	/**
	 * @return the attribute without any hidden value. Null if the whole attribute is hidden.
	 */
	public Attribute getWithoutHiddenValues()
	{
		if (selectableAttr.getSelection().get(0).getValue())
			return null;
		return getAttribute(false);
	}

	private Attribute getAttribute(boolean hidden)
	{
		List<CheckBox> selection = listOfValues.getSelection();
		List<String> filteredValues = new ArrayList<>(attribute.getValues().size());
		for (int i = 0; i < attribute.getValues().size(); i++)
		{
			String t = attribute.getValues().get(i);
			if (selection.get(i).getValue() == hidden)
				filteredValues.add(t);
		}
		return new Attribute(attribute.getName(), attribute.getValueSyntax(), 
				attribute.getGroupPath(), filteredValues);
	}
}
