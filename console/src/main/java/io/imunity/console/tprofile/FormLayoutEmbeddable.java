/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.Component;
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
public class FormLayoutEmbeddable
{
	private FormLayout layout = null;
	private List<Component> components = new ArrayList<>();
	
	
	public void addToLayout(FormLayout layout)
	{
		this.layout = layout;
		List<Component> components = new ArrayList<>();
		for (Component component: this.components)
		{
			if(component instanceof Label label)
			{
				components.add(layout.addFormItem(new Div(), label));
				continue;
			}
			components.add(layout.addFormItem(component, component.getElement().getProperty("label")));
			component.getElement().setProperty("label", "");
		}
		this.components = components;
	}
	
	public void addComponent(Component component)
	{
		if (layout != null)
		{
			if(component instanceof Label label)
			{
				components.add(layout.addFormItem(new Div(), label));
			}
			components.add(layout.addFormItem(component, component.getElement().getProperty("label")));
			component.getElement().setProperty("label", "");
			return;
		}
		components.add(component);
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
		{
			layout.getChildren()
					.filter(child -> child.getChildren().anyMatch(grandchild -> grandchild.equals(component)))
					.findFirst()
					.ifPresent(child ->
					{
						layout.remove(child);
						components.remove(child);
					});
			return;
		}
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
