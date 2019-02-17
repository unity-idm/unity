/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile.dryrun;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnEvent;

/**
 * Fourth dryrun step with profile validation - used in {@link DryRunDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class DryRunStep implements WizardStep 
{

	private UnityMessageSource msg;
	private DryRunStepComponent dryRunComponent;

	
	public DryRunStep(UnityMessageSource msg, String sandboxURL, 
			TranslationProfileManagement tpMan, InputTranslationActionsRegistry taRegistry) 
	{
		this.msg = msg;
		dryRunComponent = new DryRunStepComponent(msg, sandboxURL, tpMan, taRegistry);
	}

	public void handle(SandboxAuthnEvent event) 
	{
		dryRunComponent.handle(event);
	}

	@Override
	public String getCaption() 
	{
		return msg.getMessage("DryRun.DryRunStep.caption");
	}

	@Override
	public Component getContent() 
	{
		return dryRunComponent;
	}

	@Override
	public boolean onAdvance() 
	{
		return true;
	}

	@Override
	public boolean onBack() 
	{
		dryRunComponent.indicateProgress();
		return true;
	}
}
