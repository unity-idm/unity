/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.UIBgThread;

/**
 * Shows a dialog box with information about temporarily blocked access. After the timer goes off, 
 * the dialog disappears.
 * 
 * @author K. Benedyczak
 */
public class AccessBlockedDialog extends Window
{
	private UnityMessageSource msg;
	private ExecutorsService execService;
	
	
	
	public AccessBlockedDialog(UnityMessageSource msg, ExecutorsService execService)
	{
		this.msg = msg;
		this.execService = execService;
	}

	
	
	public void show()
	{
		setModal(true);
		setClosable(false);
		setCaption(msg.getMessage("error"));
		
		HorizontalLayout main = new HorizontalLayout();
		main.setMargin(true);
		main.setSpacing(true);
		Image img = new Image();
		img.setSource(Images.stderror64.getResource());
		main.addComponent(img);
		main.setComponentAlignment(img, Alignment.MIDDLE_CENTER);
		main.addComponent(new Label("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", ContentMode.HTML));
		
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.setMargin(true);
		
		Label info = new Label(msg.getMessage("AccessBlockedDialog.info"));
		info.addStyleName(Styles.textLarge.toString());
		info.addStyleName(Styles.bold.toString());
		ProgressBar progress = new ProgressBar(0);
		String ip = VaadinService.getCurrentRequest().getRemoteAddr();		
		UnsuccessfulAuthenticationCounter counter = AuthenticationProcessor.getLoginCounter();
		int initial = getRemainingBlockedTime(counter, ip);
		progress.setCaption(msg.getMessage("AccessBlockedDialog.remaining", initial));
		progress.setWidth(300, Unit.PIXELS);
		vl.addComponents(info, progress);
		vl.setComponentAlignment(progress, Alignment.MIDDLE_CENTER);

		main.addComponent(vl);
		setContent(main);
		UI ui = UI.getCurrent();
		ui.addWindow(this);
		
		execService.getService().submit(new WaiterThread(initial, progress, ip, counter, ui));
	}
	
	private int getRemainingBlockedTime(UnsuccessfulAuthenticationCounter counter, String ip)
	{
		return (int) Math.ceil(counter.getRemainingBlockedTime(ip)/1000.0);
	}
	
	private class WaiterThread extends UIBgThread
	{
		private int initial;
		private ProgressBar progress;
		private String ip;
		private UnsuccessfulAuthenticationCounter counter;
		private UI ui;
		
		public WaiterThread(int initial, ProgressBar progress, String ip, 
				UnsuccessfulAuthenticationCounter counter, UI ui)
		{
			this.initial = initial;
			this.progress = progress;
			this.ip = ip;
			this.counter = counter;
			this.ui = ui;
		}
		
		@Override
		public void safeRun()
		{
			int remaining;
			while ((remaining = getRemainingBlockedTime(counter, ip)) > 0)
			{
				final int remainingF = remaining; 
				ui.accessSynchronously(new Runnable()
				{
					@Override
					public void run()
					{
						progress.setCaption(msg.getMessage("AccessBlockedDialog.remaining", 
								remainingF));
						progress.setValue(1-(remainingF/(float)initial));
						ui.push();
					}
				});
				try
				{
					Thread.sleep(500);
				} catch (InterruptedException e)
				{
					//ignore
				}
			}
			ui.accessSynchronously(new Runnable()
			{
				@Override
				public void run()
				{
					close();
					ui.push();
				}
			});
		}
	}
}
