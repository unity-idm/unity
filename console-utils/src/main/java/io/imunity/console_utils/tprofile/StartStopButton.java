/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console_utils.tprofile;


import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;

public class StartStopButton extends Button
{
	private boolean checked = false;

	public StartStopButton() 
	{
		setIcon();
		addClickListener(e -> {
				if (checked)
				{
					fireEvent(new ClickStopEvent(StartStopButton.this));
				} else
				{
					fireEvent(new ClickStartEvent(StartStopButton.this));
				}
				checked = !checked;
				setIcon();
			}
		);
	}

	private void setIcon() 
	{
		if (checked)
		{
			setIcon(VaadinIcon.PAUSE.create());
		} else
		{
			setIcon(VaadinIcon.PLAY.create());
		}
	}

	public void addClickListener(StartListener startListener, StopListener stopListener)
	{
		addListener(ClickStartEvent.class, startListener);
		addListener(ClickStopEvent.class, stopListener);
	}

	@FunctionalInterface
	public interface StartListener extends ComponentEventListener<ClickStartEvent>
	{
	}

	@FunctionalInterface
	public interface StopListener extends ComponentEventListener<ClickStopEvent>
	{
	}

	public static class ClickStartEvent extends ComponentEvent<Button>
	{
		public ClickStartEvent(Button source)

		{
			super(source, false);
		}
	}

	public static class ClickStopEvent extends ComponentEvent<Button>
	{
		public ClickStopEvent(Button source)
		{
			super(source, false);
		}
	}
}
