/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * UI Component presenting a list of objects in a single column table.
 * Additionally entries may have actions attached, which are displayed as
 * buttons in each line. Also note for close future: this component will need to
 * be enhanced to support multiple columns (although rather only few).
 * 
 * @author P.Piernik
 */
public class ListOfElementsWithActions<T> extends CustomComponent
{
	public enum ButtonsPosition
	{
		Left, Right
	};

	private List<Entry> components;
	private LabelConverter<T> labelConverter;
	private boolean addSeparatorLine;
	private List<SingleActionHandler<T>> actionHandlers;
	private VerticalLayout main;
	private ButtonsPosition buttonsPosition;

	public ListOfElementsWithActions()
	{
		this(t -> new Label(t.toString()));
	}

	public ListOfElementsWithActions(LabelConverter<T> labelConverter)
	{

		this.labelConverter = labelConverter;
		this.components = new ArrayList<>();
		this.actionHandlers = new ArrayList<>();
		this.buttonsPosition = ButtonsPosition.Right;
		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.setId("ListOfElements");
		setCompositionRoot(main);
	}

	public ListOfElementsWithActions(UnityMessageSource msg)
	{
		this(e -> new Label(e.toString()));
	}

	public void addEntry(T entry)
	{
		Entry component = new Entry(entry);
		components.add(component);
		main.addComponent(component);
	}

	public void removeEntry(Entry entry)
	{
		components.remove(entry);
		main.removeComponent(entry);
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
		main.removeAllComponents();
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

	public void setButtonsPosition(ButtonsPosition buttonsPosition)
	{
		this.buttonsPosition = buttonsPosition;
	}

	public void addHeader(String labelTitle, String actionTitle)
	{

		Label labelTitleL = new Label(labelTitle);
		labelTitleL.setStyleName(Styles.captionBold.toString());

		Label actionTitleL = new Label(actionTitle);
		actionTitleL.setStyleName(Styles.captionBold.toString());

		main.addComponent(getEntryLine(labelTitleL, actionTitleL), 0);

	}

	private Component getEntryLine(Component comp1, Component buttons)
	{
		HorizontalLayout cont = new HorizontalLayout();
		cont.setMargin(false);
		cont.setSpacing(true);

		if (buttonsPosition == ButtonsPosition.Right)
		{
			cont.setWidth(100, Unit.PERCENTAGE);
			cont.addComponents(comp1, buttons);
			cont.setComponentAlignment(comp1, Alignment.MIDDLE_LEFT);
			cont.setComponentAlignment(buttons, Alignment.MIDDLE_RIGHT);
		} else
		{
			cont.addComponents(buttons, comp1);
		}

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
			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setMargin(false);
			buttons.setSpacing(false);
			for (SingleActionHandler<T> handler : actionHandlers)
			{

				Set<T> elementsSet = new HashSet<>();
				elementsSet.add(element);
				if (handler.isVisible(elementsSet))
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
							handler.handle(Stream.of(elementV).collect(
									Collectors.toSet()));
						}
					});
					actionButton.setEnabled(handler.isEnabled(elementsSet));

					buttons.addComponent(actionButton);
				}
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
