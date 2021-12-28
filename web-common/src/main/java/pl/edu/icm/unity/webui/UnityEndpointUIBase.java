/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * All typical Unity endpoint {@link UI}s should extend this class. Extends {@link UnityUIBase}
 * with support for presenting enquires after loading the UI.
 * @author K. Benedyczak
 */
public abstract class UnityEndpointUIBase extends UnityUIBase
{
	private EnquiresDialogLauncher enquiryDialogLauncher;
	
	public UnityEndpointUIBase(MessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher)
	{
		super(msg);
		this.enquiryDialogLauncher = enquiryDialogLauncher;
	}
	
	public UnityEndpointUIBase(MessageSource msg)
	{
		super(msg);
	}

	@Override
	protected final void appInit(VaadinRequest request)
	{
		if (enquiryDialogLauncher != null)
		{
			enquiryDialogLauncher.showEnquiryDialogIfNeeded(() -> enter(request));
		}else
		{
			enter(request);
		}
	}
	
	protected abstract void enter(VaadinRequest request);
}