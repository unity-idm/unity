/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

import com.vaadin.ui.UI;

/**
 * All typical Unity endpoint {@link UI}s should extend this class. Extends {@link UnityUIBase}
 * with support for presenting enquires after loading the UI.
 * @author K. Benedyczak
 */
public abstract class UnityEndpointUIBase extends UnityUIBase
{
	private EnquiresDialogLauncher enquiryDialogLauncher;
	
	public UnityEndpointUIBase(UnityMessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher)
	{
		super(msg);
		this.enquiryDialogLauncher = enquiryDialogLauncher;
	}

	@Override
	protected void initExtensions()
	{
		enquiryDialogLauncher.showEnquiryDialogIfNeeded();
	}
}