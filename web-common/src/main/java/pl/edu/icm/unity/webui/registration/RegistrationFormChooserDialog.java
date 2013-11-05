/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Dialog allowing to choose a registration form. It takes an {@link RegistrationFormsChooserComponent} as an argument.
 * @author K. Benedyczak
 */
public class RegistrationFormChooserDialog extends AbstractDialog
{
	private RegistrationFormsChooserComponent editor;
	
	public RegistrationFormChooserDialog(UnityMessageSource msg, String caption, 
			RegistrationFormsChooserComponent editor)
	{
		super(msg, caption, msg.getMessage("close"));
		this.editor = editor;
		defaultSizeUndfined = true;
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_CENTER);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		close();
	}
}
