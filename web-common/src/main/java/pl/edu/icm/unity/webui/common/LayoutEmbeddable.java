/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Layout;

/**
 * This class links a layout with other class which provides components to it. It is useful
 * whenever (reusable) part of a layout should be programmed as a separate class, but its components should
 * be added to layout directly to obtain uniform presentation (think about {@link FormLayout}).
 * <p>
 * This class provides operations to add/remove componets, which are proxied to the layout. 
 *    
 * @author K. Benedyczak
 */
public class LayoutEmbeddable
{
	private Layout layout = null;
	private List<Component> components = new ArrayList<>();
	
	
	public void addToLayout(Layout layout)
	{
		this.layout = layout;
		for (Component component: components)
			layout.addComponent(component);
	}
	
	public void addComponent(Component component)
	{
		components.add(component);
		if (layout != null)
			layout.addComponent(component);
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
			layout.removeComponent(component);
		components.remove(component);
	}

	public void removeAllComponents()
	{
		if (layout != null)
			for (Component component: components)
				layout.removeComponent(component);
		components.clear();
	}
}
