/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * List of elements displayed in two columns. The first column contains an arbitrary component
 * and takes the most of space. The 2nd column displays a {@link Checkbox}. It is possible to check whether any of the
 * elements was checked or not. 
 * <p>
 * It is possible to enable value disabling: whenever the checkbox is selected (or deselected - this is also configurable)
 * the value in the first column is disabled. 
 * 
 * @author K. Benedyczak
 */
public class ListOfSelectableElements extends VerticalLayout
{
	protected List<Checkbox> selects;
	public enum DisableMode {NONE, WHEN_SELECTED, WHEN_DESELECTED};
	protected DisableMode disableMode;
	private List<Component> elements;
	
	public ListOfSelectableElements(Component firstHeader, Component secondHeader, DisableMode disableMode)
	{
		setWidthFull();
		getStyle().set("gap", "0.4em");
		setPadding(false);

		if (firstHeader != null)
			add(wrapWithLayout(firstHeader));
		if (secondHeader != null)
			add(wrapWithLayout(secondHeader));

		selects = new ArrayList<>();
		elements = new ArrayList<>();
		this.disableMode = disableMode;
	}
	
	public void addEntry(Component representation, boolean selected)
	{
		Checkbox checkbox = new Checkbox();
		checkbox.setValue(selected);
		
		if (disableMode != DisableMode.NONE)
		{
			checkbox.addValueChangeListener(new ValueDisableHandler((HasEnabled) representation));
		}
		selects.add(checkbox);

		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
		horizontalLayout.setAlignItems(Alignment.BASELINE);
		horizontalLayout.setMargin(false);
		horizontalLayout.setPadding(false);
		horizontalLayout.setWidthFull();
		horizontalLayout.add(representation, checkbox);
		elements.add(horizontalLayout);
		add(horizontalLayout);
	}
	
	private VerticalLayout wrapWithLayout(Component component)
	{
		VerticalLayout layout = new VerticalLayout(component);
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.setPadding(false);
		layout.setWidthFull();
		return layout;
	}

	
	
	public List<Checkbox> getSelection()
	{
		return new ArrayList<>(selects);
	}
	
	public void setCheckBoxesEnabled(boolean enabled)
	{
		for (Checkbox cb : selects)
			cb.setEnabled(enabled);
	}
	
	public void setCheckBoxesVisible(boolean visible)
	{
		for (Checkbox cb : selects)
			cb.setVisible(visible);
	}
	
	private class ValueDisableHandler implements HasValue.ValueChangeListener<HasValue.ValueChangeEvent<Boolean>>
	{
		private final HasEnabled representation;
		
		public ValueDisableHandler(HasEnabled representation)
		{
			this.representation = representation;
		}

		@Override
		public void valueChanged(HasValue.ValueChangeEvent<Boolean> event)
		{
			Boolean value = event.getValue();
			if (disableMode == DisableMode.WHEN_SELECTED)
				value = !value;
			representation.setEnabled(value);
		}
	}

	public void clearEntries()
	{
		elements.forEach(this::remove);
		elements.clear();
	}

	public boolean isEmpty()
	{
		return elements.isEmpty();
	}
}
