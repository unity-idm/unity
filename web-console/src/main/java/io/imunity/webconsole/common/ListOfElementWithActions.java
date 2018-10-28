/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Component maintaining a list of values with actions buttons. The values are displayed as labels. 
 * Buttons bar is builder based on  {@link SingleActionHandler} elements.
 * 
 * @author P.Piernik
 */
public class ListOfElementWithActions<T> extends VerticalLayout
{
	private List<Entry> components;
	private LabelConverter<T> labelConverter;
	private boolean addSeparatorLine;
	private List<SingleActionHandler<T>> actionHandlers;

	public ListOfElementWithActions(LabelConverter<T> labelConverter)
	{

		this.labelConverter = labelConverter;
		this.components = new ArrayList<>();
		this.actionHandlers = new ArrayList<>();
		this.setMargin(false);
		this.setSpacing(false);
	}

	public ListOfElementWithActions(UnityMessageSource msg)
	{
		this(e -> new Label(e.toString()));
	}

	public void addEntry(T entry)
	{
		Entry component = new Entry(entry);
		components.add(component);
		addComponent(component);
	}

	public void removeEntry(Entry entry)
	{
		components.remove(entry);
		removeComponent(entry);
	}

	public void removeEntry(T element)
	{
		Entry entry = getElement(element);
		if (entry != null)
			removeEntry(entry);
	}

	public Entry getElement(T element)
	{
		for (Entry e : components)
		{
			if (e.getElement() == element)
				return e;
		}
		return null;
	}

	public void clearContents()
	{
		components.clear();
		removeAllComponents();
	}

	public List<T> getElements()
	{
		List<T> ret = new ArrayList<>(components.size());
		for (Entry e : components)
			ret.add(e.getElement());
		return ret;
	}

	public int size()
	{
		return components.size();
	}

	public void setAddSeparatorLine(boolean addSeparatorLine)
	{
		this.addSeparatorLine = addSeparatorLine;
	}

	public void addActionHandler(SingleActionHandler<T> handler)
	{
		actionHandlers.add(handler);
	}

	public void addHeader(String labelTitle, String actionTitle)
	{

		Label labelTitleL = new Label(labelTitle);
		labelTitleL.setStyleName(Styles.captionBold.toString());

		Label actionTitleL = new Label(actionTitle);
		actionTitleL.setStyleName(Styles.captionBold.toString());

		addComponent(getEntryLine(labelTitleL, actionTitleL), 0);

	}

	private Component getEntryLine(Component comp1, Component comp2)
	{
		HorizontalLayout cont = new HorizontalLayout();
		cont.setMargin(false);
		cont.setSpacing(false);
		cont.setWidth(100, Unit.PERCENTAGE);
		cont.addComponents(comp1, comp2);

		cont.setComponentAlignment(comp1, Alignment.MIDDLE_LEFT);
		cont.setComponentAlignment(comp2, Alignment.MIDDLE_RIGHT);

		VerticalLayout main = new VerticalLayout(cont);
		main.setSpacing(false);
		main.setMargin(false);

		main.addComponent(cont);
		if (addSeparatorLine)
		{
			main.addComponent(HtmlTag.horizontalLine());
		}
		return main;
	}

	private class Entry extends CustomComponent
	{
		private T element;

		public Entry(T elementV)
		{

			this.element = elementV;
			setSpacing(true);
			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setMargin(false);
			buttons.setSpacing(false);
			//TODO full support for single actions handlers, disable predicate etc.
			for (SingleActionHandler<T> handler : actionHandlers)
			{
				Button actionButton = new Button();
				actionButton.setIcon(handler.getIcon());
				actionButton.setDescription(handler.getCaption());
				actionButton.setStyleName(Styles.vButtonSmall.toString());
				actionButton.addClickListener(new Button.ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						handler.handle(Stream.of(elementV)
								.collect(Collectors.toSet()));
					}
				});
				buttons.addComponent(actionButton);

			}

			setCompositionRoot(getEntryLine(labelConverter.toLabel(element), buttons));
		}

		public T getElement()
		{
			return element;
		}
	}

	public interface LabelConverter<T>
	{
		public Component toLabel(T value);
	}
}
