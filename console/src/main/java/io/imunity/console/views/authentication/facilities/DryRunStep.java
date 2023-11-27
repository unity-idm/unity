/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import io.imunity.console.tprofile.MappingResultComponent;
import io.imunity.console.tprofile.TranslationProfileViewer;
import io.imunity.vaadin.elements.wizard.WizardStep;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;

import java.io.CharArrayWriter;
import java.io.PrintWriter;


class DryRunStep extends WizardStep
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, DryRunStep.class);

	private final InputTranslationActionsRegistry taRegistry;
	private final TranslationProfileManagement tpMan;
	private final MessageSource msg;
	private final MappingResultComponent mappingResult;
	private final RemotelyAuthenticatedInputComponent remoteIdpInput;
	private VerticalLayout resultWrapper;
	private Span capturedLogs;
	private Html logsLabel;
	private Hr hr_2;
	private VerticalLayout mappingResultWrap;
	private VerticalLayout remoteIdpWrap;
	private Span authnResultLabel;
	private TranslationProfileViewer profileViewer;
	private SandboxAuthnContext ctx;

	DryRunStep(MessageSource msg,
			TranslationProfileManagement tpMan, InputTranslationActionsRegistry taRegistry) 
	{
		super(msg.getMessage("DryRun.DryRunStep.caption"), new VerticalLayout());
		this.msg = msg;
		this.tpMan = tpMan;
		this.taRegistry = taRegistry;

		resultWrapper = buildResultWrapper();
		((VerticalLayout)component).add(resultWrapper);
		capturedLogs.setText("");

		logsLabel.setHtmlContent("<div></div>");

		mappingResult = new MappingResultComponent(msg);
		mappingResultWrap.add(mappingResult);
		
		remoteIdpInput = new RemotelyAuthenticatedInputComponent(msg);
		remoteIdpWrap.add(remoteIdpInput);
	}

	void prepareStep(SandboxAuthnEvent event)
	{
		ctx = event.ctx;
	}
	
	private void showProfile(String profile)
	{
		boolean isHRVisible = (profile != null);
		try
		{
			TranslationProfile tp = tpMan.listInputProfiles().get(profile);
			profileViewer.setInput(tp, taRegistry);
			profileViewer.setVisible(true);
		} catch (EngineException e)
		{
			isHRVisible = false;
			log.error(e);
		}
		hr_2.setVisible(isHRVisible);
	}

	private VerticalLayout buildResultWrapper()
	{
		resultWrapper = new VerticalLayout();

		authnResultLabel = new Span();
		authnResultLabel.setWidthFull();
		resultWrapper.add(authnResultLabel);
		
		Hr hr_3 = new Hr();
		resultWrapper.add(hr_3);
		
		// remoteIdpWrap
		remoteIdpWrap = new VerticalLayout();
		remoteIdpWrap.setMargin(false);

		// mappingResultWrap
		mappingResultWrap = new VerticalLayout();
		mappingResultWrap.setMargin(false);
		
		SplitLayout panel = new SplitLayout(remoteIdpWrap, mappingResultWrap);
		panel.setSizeFull();
		panel.setSplitterPosition(50);
		resultWrapper.add(panel);
		
		// hr_1
		Hr hr_1 = new Hr();
		resultWrapper.add(hr_1);
		
		profileViewer = new TranslationProfileViewer(msg);
		
		resultWrapper.add(profileViewer);
		
		// hr_2
		hr_2 = new Hr();
		resultWrapper.add(hr_2);
		
		// logsLabel
		logsLabel = new Html("<div></div>");
		resultWrapper.add(logsLabel);
		
		capturedLogs = new Span();
		capturedLogs.setWidthFull();
		capturedLogs.setHeight("-1px");
		resultWrapper.add(capturedLogs);
		
		return resultWrapper;
	}

	@Override
	protected void initialize()
	{
		if (ctx.getAuthnException().isEmpty())
		{
			authnResultLabel.setText(msg.getMessage("DryRun.DryRunStepComponent.authnResultLabel.success"));
			authnResultLabel.getStyle().set("color", "var(--lumo-success-text-color)");
		} else
		{
			authnResultLabel.setText(msg.getMessage("DryRun.DryRunStepComponent.authnResultLabel.error"));
			authnResultLabel.getStyle().set("color", "var(--lumo-error-text-color)");
		}
		logsLabel.setHtmlContent("<div>" + msg.getMessage("DryRun.DryRunStepComponent.logsLabel") + "</div>");
		if (ctx.getRemotePrincipal().isPresent())
		{
			RemotelyAuthenticatedPrincipal remoteAuthnContext = ctx.getRemotePrincipal().get();
			remoteIdpInput.displayAuthnInput(remoteAuthnContext.getAuthnInput());
			mappingResult.displayMappingResult(remoteAuthnContext.getMappingResult(),
					remoteAuthnContext.getInputTranslationProfile());
			showProfile(remoteAuthnContext.getInputTranslationProfile());
		} else
		{
			profileViewer.setVisible(false);
			hr_2.setVisible(false);
		}
		StringBuilder logs = new StringBuilder(ctx.getLogs());
		if (ctx.getAuthnException().isPresent())
		{
			CharArrayWriter writer = new CharArrayWriter();
			ctx.getAuthnException().get().printStackTrace(new PrintWriter(writer));
			logs.append("\n\n").append(writer);
		}
		capturedLogs.setText(logs.toString());
		stepComplited();
		refreshWizard();
	}
}
