/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.google.common.html.HtmlEscapers;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;

import java.util.*;
import java.util.function.Consumer;


/**
 * Component with a list of small buttons. Buttons are bound to actions via 
 * {@link SingleActionHandler}.
 * 
 * Additionally toolbar's buttons have their state enabled or disabled depending 
 * whether the toolbar's target is set or not.
 *  
 * @author P.Piernik
 */
public class Toolbar<T> extends HorizontalLayout
{
	private Set<T> target;
	private Map<Button, SingleActionHandler<T>> buttons;
	
	
	public Toolbar()
	{
		target = Collections.emptySet();
		setWidthFull();
		this.buttons = new HashMap<>();
		setJustifyContentMode(JustifyContentMode.BETWEEN);
		setPadding(false);
		setSpacing(false);

	}

	/**
	 * @return a listener that can be registered on a selectable component as {@link Tree} or {@link Table}
	 * to update the toolbar's target.
	 */
	public Consumer<Set<T>> getSelectionListener()
	{
		return event ->
		{
			target = event;
			for (Button button: buttons.keySet())
				updateButtonState(button);
		};
	}
	
	private void updateButtonState(Button button)
	{
		SingleActionHandler<T> buttonData = buttons.get(button);
		if (button == null)
			return;
		
		if (buttonData.isVisible(target))
		{
			button.setVisible(true);
			button.setEnabled(buttonData.isEnabled(target));
		} else
		{
			button.setVisible(false);
		}
	}
	
	public void addActionHandlers(Collection<SingleActionHandler<T>> handlers)
	{
		for (SingleActionHandler<T> handler: handlers)
			addActionHandler(handler);
	}
	
	public void refresh()
	{
		target = new HashSet<>();
		for (Button button: buttons.keySet())
			updateButtonState(button);
	}
	
	/**
	 * Adds a custom button. It is styled in the same way as other in the toolbar, but its actions must 
	 * be configured manually.
	 */
	public void addButton(Button button, SingleActionHandler<T> handler)
	{
		buttons.put(button, handler);
		add(button);
	}

	public void addHamburger(ActionMenuWithHandlerSupport<?> menuBar)
	{
		add(menuBar.getTarget());
		menuBar.getTarget().getElement().getStyle().set("margin-left", "1.3em");
		setAlignSelf(FlexComponent.Alignment.START, menuBar.getTarget());
	}

	public void addHamburger(ActionMenuWithHandlerSupport<?> menuBar, FlexComponent.Alignment alignment)
	{
		add(menuBar.getTarget());
		menuBar.getTarget().getElement().getStyle().set("margin-left", "1.3em");
		setAlignSelf(alignment, menuBar.getTarget());
	}
	
	public void addSearch(SearchField search)
	{
		add(search);
		setAlignSelf(FlexComponent.Alignment.END, search);
	}

	public void addActionHandler(SingleActionHandler<T> handler)
	{
		final Button button = new Button();
		if (handler.getIcon() != null)
			button.setIcon(handler.getIcon().create());
		else if (handler.getCaption() != null)
			button.setText(handler.getCaption());
		if (handler.getCaption() != null)
			button.setTooltipText(HtmlEscapers.htmlEscaper().escape(handler.getCaption()));
		button.addClickListener(event ->
		{
			if (!handler.isEnabled(target))
				return;
			handler.handle(target);
		});
		addButton(button, handler);
		updateButtonState(button);
	}
}
