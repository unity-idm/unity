/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.translationProfile.dryrun;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;

/**
 * Fourth dryrun step with profile validation - used in {@link DryRunDialogComponent}.
 * 
 * @author Roman Krysinski
 */
class DryRunStep implements WizardStep 
{

	private MessageSource msg;
	private DryRunStepComponent dryRunComponent;

	
	DryRunStep(MessageSource msg, String sandboxURL, 
			TranslationProfileManagement tpMan, InputTranslationActionsRegistry taRegistry) 
	{
		this.msg = msg;
		dryRunComponent = new DryRunStepComponent(msg, sandboxURL, tpMan, taRegistry);
	}

	void handle(SandboxAuthnEvent event) 
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
