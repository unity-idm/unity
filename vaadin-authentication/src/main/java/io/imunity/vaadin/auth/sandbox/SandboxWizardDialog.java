/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.sandbox;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;

/**
 * Dialog allowing for bootstrapping a wizard which opens a sandbox popup. Responsible for showing and closing
 * the main dialog window with the {@link Wizard} component inside.
 */
public class SandboxWizardDialog extends Dialog
{
	public SandboxWizardDialog()
	{
		setModal(true);
		setWidth("80%");
		setHeight("60%");
	}
	public SandboxWizardDialog(Component wizard, String title)
	{
		this();
		setHeaderTitle(title);
		add(wizard);
	}
}
