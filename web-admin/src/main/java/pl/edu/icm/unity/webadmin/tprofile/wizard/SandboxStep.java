/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;

/**
 * Second wizard step with sandbox authentication to get input 
 * for profile creation- used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class SandboxStep implements WizardStep 
{

	private UnityMessageSource msg;
	private String sandboxURL;

	public SandboxStep(UnityMessageSource msg, String sandboxURL) 
	{
		this.msg = msg;
		this.sandboxURL = sandboxURL;
	}

	@Override
	public String getCaption() 
	{
		return msg.getMessage("Wizard.SandboxStep.caption");
	}

	@Override
	public Component getContent() 
	{
		return new SandboxStepComponent(msg, sandboxURL);
	}

	@Override
	public boolean onAdvance() 
	{
		return false;
	}

	@Override
	public boolean onBack() 
	{
		// TODO Auto-generated method stub
		return true;
	}

}
