/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;

/**
 * Very similar to {@link FormLayout}. However it is based on GridLayout. 
 * The error/required marks are displayed on components right. Caption as a separate component on the left column.
 * 
 * What is the added value, this form can display a 3rd component on the right-hand side of the main component.
 * This is useful for 'edit' or 'disable' buttons.
 * @author K. Benedyczak
 */
public class FlexibleFormLayout extends GridLayout
{
	private int row=0;
	
	public FlexibleFormLayout()
	{
		super(3, 1);
		setColumnExpandRatio(0, 1.0f);
		setColumnExpandRatio(1, 4.0f);
		setColumnExpandRatio(2, 1.0f);
		setSpacing(true);
		setMargin(new MarginInfo(true, false, true, false));
	}
	
	@Override
	public void addComponent(Component component)
	{
		addLine(component);
	}
	
	public void addLine(Component component)
	{
		addTwoFirstGuessingCaption(component, true);
	}

	private void addTwoFirstGuessingCaption(Component component, boolean consumeAll)
	{
		if (component instanceof CheckBox)
			addTwoFirst("", component, consumeAll);
		else
		{
			String caption = component.getCaption();
			component.setCaption(null);
			addTwoFirst(caption, component, consumeAll);
		}
	}
	
	private void addTwoFirst(String captionStr, Component component, boolean consumeAll)
	{
		insertRow(row+1);
		Label caption = new Label(captionStr); 
		caption.setSizeUndefined();
		addComponent(caption, 0, row);
		setComponentAlignment(caption, Alignment.MIDDLE_RIGHT);
		if (consumeAll)
			addComponent(component, 1, row, 2, row);
		else
			addComponent(component, 1, row);
		setComponentAlignment(component, Alignment.MIDDLE_LEFT);
		row++;
	}
	
	public void addLine(Component component, Component additional)
	{
		addTwoFirstGuessingCaption(component, false);
		addComponent(additional, 2, row-1);
		setComponentAlignment(additional, Alignment.MIDDLE_LEFT);
	}
}
