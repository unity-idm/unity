/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;

/**
 * Third wizard step with with profile creation - used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class ProfileStep implements WizardStep 
{

	private UnityMessageSource msg;

	public ProfileStep(UnityMessageSource msg) 
	{
		this.msg = msg;
	}

	@Override
	public String getCaption() 
	{
		return msg.getMessage("Wizard.ProfileStep.caption");
	}

	@Override
	public Component getContent() 
	{
		return new ProfileStepComponent(msg);
	}

	@Override
	public boolean onAdvance() 
	{
		return true;
	}

	@Override
	public boolean onBack() 
	{
		return false;
	}

}
