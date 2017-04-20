/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * List of elements displayed in two columns. The first column contains an arbitrary component
 * and takes the most of space. The 2nd column displays a {@link CheckBox}. It is possible to check whether any of the 
 * elements was checked or not. 
 * <p>
 * It is possible to enable value disabling: whenever the checkbox is selected (or deselected - this is also configurable)
 * the value in the first column is disabled. 
 * 
 * @author K. Benedyczak
 */
public class ListOfSelectableElements extends CustomComponent
{
	protected List<CheckBox> selects;
	protected GridLayout gl;
	protected int row;
	public enum DisableMode {NONE, WHEN_SELECTED, WHEN_DESELECTED};
	protected DisableMode disableMode;
	
	
	public ListOfSelectableElements(Component firstHeader, Component secondHeader, DisableMode disableMode)
	{
		gl = new GridLayout(2, 1);
		gl.setSpacing(true);
		gl.setWidth(100, Unit.PERCENTAGE);
		if (firstHeader != null)
			gl.addComponent(new VerticalLayout(firstHeader), 0, 0);
		if (secondHeader != null)
			gl.addComponent(new VerticalLayout(secondHeader), 1, 0);
		gl.setColumnExpandRatio(0, 10);
		gl.setColumnExpandRatio(1, 1);
		selects = new ArrayList<CheckBox>();
		setCompositionRoot(gl);
		row = 1;
		this.disableMode = disableMode;
	}

	public void addEntry(Component representation, boolean selected)
	{
		addEntry(representation, selected, null);
	}
	
	public void addEntry(Component representation, boolean selected, Object data)
	{
		gl.setRows(gl.getRows()+1);
		gl.addComponent(new VerticalLayout(representation), 0, row);

		CheckBox cb = new CheckBox();
		if (data != null)
			cb.setData(data);
		cb.setValue(selected);
		
		if (disableMode != DisableMode.NONE)
		{
			cb.setImmediate(true);
			cb.addValueChangeListener(new ValueDisableHandler(representation));
		}
		gl.addComponent(new VerticalLayout(cb), 1, row);
		selects.add(cb);
		row++;
		
	}
	
	public void clearEntries()
	{
		for (int i=1; i<gl.getRows(); i++)
		{
			gl.removeComponent(0, i);
			gl.removeComponent(1, i);
		}
		gl.setRows(1);
		selects.clear();
		row = 1;
	}
	
	public boolean isEmpty()
	{
		return row == 1;
	}
	
	public List<CheckBox> getSelection()
	{
		return new ArrayList<>(selects);
	}
	
	public void setCheckBoxesEnabled(boolean enabled)
	{
		for (CheckBox cb : selects)
			cb.setEnabled(enabled);
	}
	
	public void setCheckBoxesVisible(boolean visible)
	{
		for (CheckBox cb : selects)
			cb.setVisible(visible);
	}
	
	private class ValueDisableHandler implements ValueChangeListener
	{
		private Component representation;
		
		public ValueDisableHandler(Component representation)
		{
			this.representation = representation;
		}

		@Override
		public void valueChange(ValueChangeEvent event)
		{
			Boolean value = (Boolean)event.getProperty().getValue();
			if (disableMode == DisableMode.WHEN_SELECTED)
				value = !value;
			representation.setEnabled(value);
		}
	}
}
