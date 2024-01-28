/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;

import io.imunity.vaadin.elements.CssClassNames;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Base for all service editor. Editor based on two tabs - general and
 * authentication.
 * 
 * @author P.Piernik
 *
 */
public abstract class ServiceEditorBase extends TabSheet implements ServiceEditorComponent
{
	protected MessageSource msg;

	private Map<String, Tab> defTabs;
	private Map<String, TabLayout> defTabContents;

	public ServiceEditorBase(MessageSource msg)
	{
		this.msg = msg;
		defTabs = new HashMap<>();
		defTabContents = new HashMap<>();

		addSelectedChangeListener(e -> setErrorInTabs());
		new TextField().setErrorMessage(null);
	}

	// TODO
	protected void setErrorInTabs()
	{

		for (String tab : defTabs.keySet())
		{
			defTabContents.get(tab).error.setVisible(false);
			Tab tabContent = defTabs.get(tab);

			if (assertErrorComponent(Stream.of(getComponent(tabContent))))
			{
				defTabContents.get(tab).error.setVisible(true);
			}
		}
	}

	// TODO
	boolean assertErrorComponent(Stream<Component> stream)
	{
		Set<Component> list = stream.collect(Collectors.toSet());
		for (Component component : list)
		{
			if (component instanceof HasValidation c)
			{
				if (c.isInvalid())
					return true;

			}
			
			if (component instanceof Component c)
			{
				return assertErrorComponent(c.getChildren());
			}
		}
		return false;
	}

	protected void registerTab(EditorTab editorTab)
	{
		TabLayout tabLayout = new TabLayout(editorTab.getIcon(), editorTab.getCaption());
		Tab add = add(tabLayout, editorTab.getComponent());
		defTabs.put(editorTab.getType(), add);
		defTabContents.put(editorTab.getType(), tabLayout);
	}

	public void setActiveTab(String tab)
	{
		setSelectedTab(defTabs.get(tab));
	}

	public interface EditorTab
	{
		VaadinIcon getIcon();

		String getType();

		Component getComponent();

		String getCaption();
	}

	public static class TabLayout extends HorizontalLayout
	{
		public final Icon error = VaadinIcon.EXCLAMATION.create();

		public TabLayout(VaadinIcon icon, String caption)
		{
			add(new Icon(icon));
			add(new NativeLabel(caption));
			add(error);
			error.addClassName(CssClassNames.SMALL_ICON.getName());
			error.addClassName(CssClassNames.ERROR.getName());
			error.setVisible(false);
			setSpacing(true);
			setPadding(false);
		}

	}

}
