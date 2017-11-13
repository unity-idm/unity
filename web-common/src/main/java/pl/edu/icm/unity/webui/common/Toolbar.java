/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.html.HtmlEscapers;
import com.vaadin.event.Action;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.Property.ValueChangeNotifier;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.Tree;

/**
 * Component with a list of small buttons. Buttons are bound to {@link Action}s via 
 * {@link SingleActionHandler}. The wrapped Action must have at least caption or image set.
 * 
 * Additionally toolbar's buttons have their state enabled or disabled depending whether the toolbar's target is set 
 * or not.
 * @deprecated use {@link Toolbar2}
 * @author K. Benedyczak
 */
@Deprecated
public class Toolbar extends CustomComponent
{
	private Orientation orientation;
	private ValueChangeNotifier source;
	private Object target;
	private List<Button> buttons;
	private AbstractOrderedLayout main;
	
	public Toolbar(ValueChangeNotifier source, Orientation orientation)
	{
		this.source = source;
		this.orientation = orientation;
		this.main = orientation == Orientation.HORIZONTAL ? new HorizontalLayout() : new VerticalLayout();
		
		source.addValueChangeListener(getValueChangeListener());
		buttons = new ArrayList<>();
		main.setSpacing(true);
		main.setMargin(false);
		main.addStyleName(Styles.tinySpacing.toString());
		setCompositionRoot(main);
		setSizeUndefined();
	}

	public Orientation getOrientation()
	{
		return orientation;
	}

	/**
	 * @return a listener that can be registered on a selectable component as {@link Tree} or {@link Table}
	 * to update the toolbar's target.
	 */
	public ValueChangeListener getValueChangeListener()
	{
		return new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				target = event.getProperty().getValue();
				for (Button button: buttons)
				{
					updateButtonState(button);
				}
			}
		};
	}
	
	private void updateButtonState(Button button)
	{
		Object buttonData = button.getData();
		if (buttonData == null || !(buttonData instanceof SingleActionHandler))
			return;
		SingleActionHandler handler = (SingleActionHandler) button.getData();
		if (handler.isNeeded())
		{
			button.setVisible(true);
			if (handler.isNeedsTarget() && target == null)
			{
				button.setEnabled(false);
				if (handler.isHideIfNotNeeded())
					button.setVisible(false);
			} else
			{
				boolean en = handler.getActions(target, source).length == 1;
				button.setEnabled(en);
				if (handler.isHideIfNotNeeded())
					button.setVisible(en);
				
			}
		} else
		{
			button.setVisible(false);
		}
	}
	
	public void addActionHandlers(Collection<SingleActionHandler> handlers)
	{
		for (SingleActionHandler handler: handlers)
			addActionHandler(handler);
	}

	public void addActionHandlers(SingleActionHandler... handlers)
	{
		for (SingleActionHandler handler: handlers)
			addActionHandler(handler);
	}

	public void addButtons(Button... buttons)
	{
		for (Button button: buttons)
			addButton(button);
	}
	
	public void addSeparator()
	{
		Label sep = new Label();
		String style = orientation == Orientation.HORIZONTAL ? Styles.verticalBar.toString() :
			Styles.horizontalBar.toString();
		sep.addStyleName(style);
		main.addComponent(sep);
		main.setComponentAlignment(sep, Alignment.MIDDLE_CENTER);
	}
	
	/**
	 * Adds a custom button. It is styled in the same way as other in the toolbar, but its actions must 
	 * be configured manually.
	 */
	public void addButton(Button button)
	{
		button.addStyleName(Styles.vButtonLink.toString());
		button.addStyleName(Styles.toolbarButton.toString());
		buttons.add(button);
		main.addComponent(button);
	}
	
	public void addActionHandler(SingleActionHandler handler)
	{
		Action action = handler.getActionUnconditionally();
		final Button button = new Button();
		button.setData(handler);
		if (action.getIcon() != null)
			button.setIcon(action.getIcon());
		else
			button.setCaption(action.getCaption());
		if (action.getCaption() != null)
			button.setDescription(HtmlEscapers.htmlEscaper().escape(action.getCaption()));
		button.addStyleName(Styles.vButtonLink.toString());
		button.addStyleName(Styles.toolbarButton.toString());
		button.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				SingleActionHandler handler = (SingleActionHandler) button.getData();
				if (handler.isNeedsTarget() && target == null)
					return;
				handler.handleAction(handler.getActionUnconditionally(), source, target);
			}
		});
		buttons.add(button);
		main.addComponent(button);
		updateButtonState(button);
	}
}
