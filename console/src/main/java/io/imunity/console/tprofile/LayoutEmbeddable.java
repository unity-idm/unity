/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class links a layout with other class which provides components to it. It is useful
 * whenever (reusable) part of a layout should be programmed as a separate class, but its components should
 * be added to layout directly to obtain uniform presentation (think about {@link FormLayout}).
 * <p>
 * This class provides operations to add/remove components, which are proxied to the layout.
 */
public class LayoutEmbeddable
{
	private HasComponents layout = null;
	private List<Component> components = new ArrayList<>();
	
	
	public void addToLayout(HasComponents layout)
	{
		this.layout = layout;
		for (Component component: components)
			layout.add(component);
	}

	public void addToFormLayout(FormLayout layout)
	{
		this.layout = layout;
		for (Component component: components)
		{
			if(component instanceof Label label)
			{
				layout.addFormItem(new Div(), label);
				continue;
			}
			layout.addFormItem(component, component.getElement().getProperty("label"));
			component.getElement().setProperty("label", "");
		}
	}
	
	public void addComponent(Component component)
	{
		components.add(component);
		if (layout != null)
			layout.add(component);
	}

	public void addComponents(Component... components)
	{
		for (Component c: components)
			addComponent(c);
	}
	
	public void removeComponents(Collection<? extends Component> components)
	{
		for (Component c: components)
			removeComponent(c);
	}

	public void removeComponent(Component component)
	{
		if (layout != null)
			layout.remove(component);
		components.remove(component);
	}

	public void removeAllComponents()
	{
		if (layout != null)
			for (Component component: components)
				layout.remove(component);
		components.clear();
	}
}
