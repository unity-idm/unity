/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Small component showing that remote authentication is in progress and allowing to cancel it
 * @author K. Benedyczak
 */
public class RemoteAuthenticationProgress extends CustomComponent
{
	private final UnityMessageSource msg;
	private final Runnable cancelHandler;
	private HorizontalLayout internalHL;
	
	public RemoteAuthenticationProgress(UnityMessageSource msg, Runnable cancelHandler)
	{
		this.msg = msg;
		this.cancelHandler = cancelHandler;
		init();
	}

	public void setInternalVisibility(boolean visible)
	{
		internalHL.setVisible(visible);
	}
	
	private void init()
	{
		HorizontalLayout authnProgressHL = new HorizontalLayout();
		authnProgressHL.setWidth(100, Unit.PERCENTAGE);
		authnProgressHL.setMargin(new MarginInfo(false, true, false, true));
		Label filler = new Label("&nbsp", ContentMode.HTML);
		authnProgressHL.addComponent(filler);
		filler.addStyleName("u-verticalSpace1Unit");
		
		internalHL = new HorizontalLayout();
		internalHL.setMargin(false);
		Label info = new Label(msg.getMessage("AuthenticationUI.authnInProgress"));
		ProgressBar progress = new ProgressBar();
		progress.setIndeterminate(true);
		Button cancelOngoingAuthnButton = new Button(msg.getMessage("cancel")); 
		cancelOngoingAuthnButton.addStyleName(Styles.vButtonSmall.toString());
		cancelOngoingAuthnButton.addClickListener(event -> cancelHandler.run());
		internalHL.setHeight(100, Unit.PERCENTAGE);
		internalHL.addComponents(info, progress, cancelOngoingAuthnButton);
		internalHL.setComponentAlignment(info, Alignment.MIDDLE_RIGHT);
		internalHL.setComponentAlignment(progress, Alignment.MIDDLE_RIGHT);
		internalHL.setComponentAlignment(cancelOngoingAuthnButton, Alignment.MIDDLE_RIGHT);
		
		authnProgressHL.addComponent(internalHL);
		authnProgressHL.setComponentAlignment(internalHL, Alignment.MIDDLE_RIGHT);
		setCompositionRoot(authnProgressHL);
		addStyleName("u-remoteAuthnProgress");
	}
}
