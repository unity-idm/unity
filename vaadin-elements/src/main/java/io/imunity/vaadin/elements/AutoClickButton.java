/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import java.util.Timer;
import java.util.TimerTask;

public class AutoClickButton extends Button
{
	private static final int COUNTDOWN_PROGRESS = 1000;
	private static final int UI_POOL_INTERVAL = 100;
	
	private long seconds;
	private Timer timer;
	private final UI currentUI;
	private final String caption;

	public AutoClickButton(String caption, UI ui, long seconds)
	{
		super();
		this.seconds = seconds;
		this.caption = caption;
		currentUI = ui;
		currentUI.setPollInterval(UI_POOL_INTERVAL);
		ui.addDetachListener(e -> timer.cancel());
		startCountdown();
		addThemeVariants(ButtonVariant.LUMO_PRIMARY);
	}

	public void startCountdown()
	{
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				if (seconds <= 0)
				{
					timer.cancel();
					click();
					currentUI.setPollInterval(-1);
					return;
				}
				seconds--;
				currentUI.access(() -> setText(
						caption + ((seconds > 0) ? " (" + seconds + ")" : "")));
			}
		}, 0, COUNTDOWN_PROGRESS);
	}

	public void stop()
	{
		if (timer != null)
		{
			timer.cancel();
			currentUI.setPollInterval(-1);
		}
	}

}
