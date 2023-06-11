/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Displays an attribute name in the first row and all its values in subsequent rows. 
 * For each of attribute value it is possible to click a check box next to it to hide it.
 * @author K. Benedyczak
 */
class SelectableAttributeWithValues extends CustomComponent
{
	protected ListOfSelectableElements listOfValues;
	private Attribute attribute;
	private WebAttributeHandler webHandler;
	private AttributeValueSyntax<?> syntax;
	private String customAttrName;
	private String customAttrDesc;
	private boolean enableSelect;
	
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
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		
		Label attrNameLabel = new Label100(customAttrName);
		attrNameLabel.setDescription(customAttrDesc);
		main.addComponents(attrNameLabel);
		
		listOfValues = new ListOfSelectableElements(null, null, DisableMode.WHEN_DESELECTED);
		
		listOfValues.setWidth(100, Unit.PERCENTAGE);
		for (String value: attribute.getValues())
		{
			Component representation = webHandler.getRepresentation(value, AttributeViewerContext.EMPTY);
			representation.addStyleName(Styles.indent.toString());
			listOfValues.addEntry(representation, true);
		}
		listOfValues.setCheckBoxesVisible(enableSelect);
		
		if (!attribute.getValues().isEmpty())
			main.addComponent(listOfValues);

		setCompositionRoot(main);
	}
	
	/**
	 * Hides some selected values or, if the argument is null, then the whole attribute is marked as hidden.
	 */
	void setHiddenValues(Attribute hiddenValues)
	{
		//cannot hide if selecting is not enable
		if (!enableSelect)
			return;
		
		List<CheckBox> selection = listOfValues.getSelection();
		if (hiddenValues == null)
		{
			for (CheckBox sel: selection)
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
		List<CheckBox> selection = listOfValues.getSelection();
		for (CheckBox checkbox: selection)
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
		List<CheckBox> selection = listOfValues.getSelection();
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
