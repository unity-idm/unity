/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Displays an attribute name in the first row and all its values in subsequent rows. 
 * For attribute and each its value it is possible to click a check box next to it to hide it.
 * @author K. Benedyczak
 */
public class SelectableAttributeWithValues<T> extends CustomComponent
{
	protected ListOfSelectableElements selectableAttr;
	protected ListOfSelectableElements listOfValues;
	private Attribute<T> attribute;
	private WebAttributeHandler<T> webHandler;
	private AttributeType at;
	private UnityMessageSource msg;
	private Component firstheader;
	private Component secondHeader;
	
	public SelectableAttributeWithValues(Component firstheader, Component secondHeader,
			Attribute<T> attribute, AttributeType at, 
			WebAttributeHandler<T> webHandler, UnityMessageSource msg)
	{
		this.firstheader = firstheader;
		this.secondHeader = secondHeader;
		this.attribute = attribute;
		this.at = at;
		this.webHandler = webHandler;
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		
		selectableAttr = new ListOfSelectableElements(firstheader, secondHeader, DisableMode.WHEN_SELECTED);
		selectableAttr.addEntry(new Label(at.getDisplayedName().getValue(msg)), false);
		selectableAttr.setWidth(100, Unit.PERCENTAGE);
		final CheckBox mainDisable = selectableAttr.getSelection().iterator().next();
		mainDisable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				listOfValues.setEnabled(!mainDisable.getValue());
			}
		});
		
		main.addComponents(selectableAttr);
		
		listOfValues = new ListOfSelectableElements(null, null, DisableMode.WHEN_SELECTED);
		listOfValues.setWidth(100, Unit.PERCENTAGE);
		
		for (T value: attribute.getValues())
		{
			Component representation = webHandler.getRepresentation(value, 
					attribute.getAttributeSyntax(), RepresentationSize.LINE);
			representation.addStyleName(Styles.indent.toString());
			listOfValues.addEntry(representation, false);
		}
		
		if (!attribute.getValues().isEmpty())
			main.addComponent(listOfValues);

		setCompositionRoot(main);
	}
	
	/**
	 * Hides some selected values or, if the argument is null, then the whole attribute is marked as hidden.
	 */
	public void setHiddenValues(Attribute<?> hiddenValues)
	{
		if (hiddenValues == null)
		{
			selectableAttr.getSelection().get(0).setValue(true);
			return;
		}
		AttributeValueSyntax<T> attributeSyntax = attribute.getAttributeSyntax();
		List<CheckBox> selection = listOfValues.getSelection();
		for (Object svalue: hiddenValues.getValues())
		{
			int i=0;
			for (T value: attribute.getValues())
			{
				if (attributeSyntax.areEqual(value, svalue))
				{
					selection.get(i).setValue(true);
					break;
				}
				i++;
			}
		}
	}
	
	/**
	 * @return the attribute without any hidden value. Null if the whole attribute is hidden.
	 */
	public Attribute<?> getHiddenAttributeValues()
	{
		if (selectableAttr.getSelection().get(0).getValue())
			return null;
		List<CheckBox> selection = listOfValues.getSelection();
		List<T> filteredValues = new ArrayList<>(attribute.getValues().size());
		for (int i = 0; i < attribute.getValues().size(); i++)
		{
			T t = attribute.getValues().get(i);
			if (selection.get(i).getValue())
				filteredValues.add(t);
		}
		attribute.setValues(filteredValues);
		return attribute;
	}

	
	/**
	 * @return the attribute without any hidden value. Null if the whole attribute is hidden.
	 */
	public Attribute<?> getAttributeWithoutHidden()
	{
		if (selectableAttr.getSelection().get(0).getValue())
			return null;
		List<CheckBox> selection = listOfValues.getSelection();
		List<T> filteredValues = new ArrayList<>(attribute.getValues().size());
		for (int i = 0; i < attribute.getValues().size(); i++)
		{
			T t = attribute.getValues().get(i);
			if (!selection.get(i).getValue())
				filteredValues.add(t);
		}
		attribute.setValues(filteredValues);
		return attribute;
	}
	
}
