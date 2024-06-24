/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

/**
 * Shows a dialog box with information about temporarily blocked access. After the timer goes off, 
 * the dialog disappears.
 */
public class AccessBlockedDialog extends Dialog
{
	private final MessageSource msg;
	private final ExecutorsService execService;
	
	public AccessBlockedDialog(MessageSource msg, ExecutorsService execService)
	{
		this.msg = msg;
		this.execService = execService;
	}

	
	
	public void show()
	{
		setModal(true);
		setAriaLabel(msg.getMessage("error"));
		
		HorizontalLayout main = new HorizontalLayout();
		Icon img = VaadinIcon.EXCLAMATION_CIRCLE.create();
		
		main.add(img);
		main.setAlignItems(FlexComponent.Alignment.CENTER);

		VerticalLayout vl = new VerticalLayout();

		Span info = new Span(msg.getMessage("AccessBlockedDialog.info"));
		ProgressBar progress = new ProgressBar();
		String ip = HTTPRequestContext.getCurrent().getClientIP();		
		UnsuccessfulAuthenticationCounter counter = VaadinWebLogoutHandler.getLoginCounter();
		int initial = getRemainingBlockedTime(counter, ip);

		Span label = new Span(msg.getMessage("AccessBlockedDialog.remaining", initial));
		progress.setWidth(300, Unit.PIXELS);
		vl.add(label, info, progress);

		main.add(vl);
		add(main);
		UI ui = UI.getCurrent();

		ui.setPollInterval(1000);
		execService.getScheduledService().submit(new WaiterThread(initial, label, progress, ip, counter, ui));
	}
	
	private int getRemainingBlockedTime(UnsuccessfulAuthenticationCounter counter, String ip)
	{
		return (int) Math.ceil(counter.getRemainingBlockedTime(ip)/1000.0);
	}
	
	private class WaiterThread implements Runnable
	{
		private final int initial;
		private final Span label;
		private final ProgressBar progress;
		private final String ip;
		private final UnsuccessfulAuthenticationCounter counter;
		private final UI ui;
		
		public WaiterThread(int initial, Span label, ProgressBar progress, String ip,
				UnsuccessfulAuthenticationCounter counter, UI ui)
		{
			this.initial = initial;
			this.label = label;
			this.progress = progress;
			this.ip = ip;
			this.counter = counter;
			this.ui = ui;
		}

		@Override
		public void run()
		{
			int remaining;
			while ((remaining = getRemainingBlockedTime(counter, ip)) > 0)
			{
				final int remainingF = remaining; 
				ui.accessSynchronously(() ->
				{
					label.setText(msg.getMessage("AccessBlockedDialog.remaining",
							remainingF));
					progress.setValue(1-(remainingF/(float)initial));
				});
				try
				{
					Thread.sleep(500);
				} catch (InterruptedException e)
				{
					//ignore
				}
			}
			ui.accessSynchronously(() ->
			{
				ui.setPollInterval(-1);
				close();
			});
		}
	}
}
