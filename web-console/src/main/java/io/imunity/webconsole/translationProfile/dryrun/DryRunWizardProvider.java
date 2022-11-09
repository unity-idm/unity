/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.translationProfile.dryrun;

import java.net.URISyntaxException;

import org.apache.hc.core5.net.URIBuilder;
import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.webui.association.IntroStep;
import pl.edu.icm.unity.webui.sandbox.TranslationProfileSandboxUI;
import pl.edu.icm.unity.webui.sandbox.wizard.AbstractSandboxWizardProvider;

/**
 * Configures profile dry run wizard 
 * @author K. Benedyczak
 */
public class DryRunWizardProvider extends AbstractSandboxWizardProvider
{
	private MessageSource msg;
	private TranslationProfileManagement tpMan;
	private InputTranslationActionsRegistry taRegistry;

	public DryRunWizardProvider(MessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier, 
			TranslationProfileManagement tpMan, InputTranslationActionsRegistry taRegistry)
	{
		super(getURLForDryRun(sandboxURL), sandboxNotifier);
		this.msg = msg;
		this.tpMan = tpMan;
		this.taRegistry = taRegistry;
	}

	private static String getURLForDryRun(String baseSandboxURL)
	{
		URIBuilder builder;
		try
		{
			builder = new URIBuilder(baseSandboxURL);
		} catch (URISyntaxException e)
		{
			throw new IllegalArgumentException("Sandbox URL is invalid: " + baseSandboxURL, e);
		}
		builder.addParameter(TranslationProfileSandboxUI.PROFILE_VALIDATION, Boolean.TRUE.toString());
		return builder.toString();
	}
	
	@Override
	public Wizard getWizardInstance()
	{
		final Wizard wizard = new Wizard();
		wizard.setSizeFull();
		final DryRunStep dryrunStep = new DryRunStep(msg, sandboxURL, tpMan, taRegistry);
		wizard.addStep(new IntroStep(msg, "DryRun.IntroStepComponent.introLabel"));
		wizard.addStep(dryrunStep);
		
		//for the initial page
		openSandboxPopupOnNextButton(wizard);
		
		//and when the page is loaded with back button
		showSandboxPopupAfterGivenStep(wizard, IntroStep.class);
		
		addSandboxListener(dryrunStep::handle, wizard, false);
		return wizard;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("DryRun.wizardCaption");
	}
}
