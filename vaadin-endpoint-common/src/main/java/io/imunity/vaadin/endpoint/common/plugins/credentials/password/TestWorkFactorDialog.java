/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;

import com.google.common.util.concurrent.AtomicDouble;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.SCryptEncoder;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Random;


class TestWorkFactorDialog extends ConfirmDialog
{
	private final PasswordCredential config;
	private final MessageSource msg;
	private static final int WARM_UP = 5;
	private static final int TEST = 10;
	private static final float TOO_FAST_THRESHOLD = 0.1f;
	private static final float TOO_SLOW_THRESHOLD = 0.49f;
	private VerticalLayout layout;
	private final SCryptEncoder scryptEncoder;
	
	public TestWorkFactorDialog(MessageSource msg, PasswordCredential config, 
			SCryptEncoder scryptEncoder)
	{
		this.msg = msg;
		this.config = config;
		this.scryptEncoder = scryptEncoder;
		setConfirmButton(new Button(msg.getMessage("close")));
		setHeader(msg.getMessage("PasswordDefinitionEditor.testWorkFactor"));
		setWidth("35em");
		setHeight("20em");
		add(getContents());
	}

	private Component getContents()
	{
		layout = new VerticalLayout();
		layout.setSizeFull();
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		ProgressBar progress = new ProgressBar();
		layout.add(new Span(msg.getMessage("PasswordDefinitionEditor.progress")), progress);
		progress.setWidth(80, Unit.PERCENTAGE);
		UI ui = UI.getCurrent();
		assert ui != null;
		Thread asyncMeasure = new Thread(() -> measureAvgHashTime(progress, ui));
		asyncMeasure.start();
		return layout;
	}

	private void showFinalResults(float time)
	{
		layout.removeAll();
		DecimalFormat df = new DecimalFormat("0.####");
		Span resultInfo = new Span(msg.getMessage("PasswordDefinitionEditor.hashingTimeInfo"));
		H2 resultTime = new H2(msg.getMessage("PasswordDefinitionEditor.hashingTime", df.format(time)));

		String hintCode = "PasswordDefinitionEditor.hashingOK";
		if (time < TOO_FAST_THRESHOLD)
			hintCode = "PasswordDefinitionEditor.hashingTooFast";
		else if (time > TOO_SLOW_THRESHOLD)
			hintCode = "PasswordDefinitionEditor.hashingSlow";
		Span resultComment = new Span(msg.getMessage(hintCode));

		layout.add(resultInfo, resultTime, new Span(), resultComment);
	}

	
	private void measureAvgHashTime(ProgressBar progress, UI ui)
	{
		String password = "MargharetThacherIron";
		String salt = "1234567890123456789012345678901234567890123456789012345678901234";
		AtomicDouble progressValue = new AtomicDouble();
		float progressIncrement = 1f/(TEST+WARM_UP);
		Random random = new Random();
		ui.setPollInterval(250);
		for (int i=0; i<WARM_UP; i++)
			performHashing(progress, ui, password+random.nextInt(), salt, progressValue, progressIncrement);
		
		long start = System.currentTimeMillis();
		for (int i=0; i<TEST; i++)
			performHashing(progress, ui, password+random.nextInt(), salt, progressValue, progressIncrement);

		long end = System.currentTimeMillis();
		float result = ((end-start)/(float)TEST)/1000f;
		ui.accessSynchronously(() -> showFinalResults(result));
		try
		{
			Thread.sleep(300);
		}
		catch (InterruptedException ignored) {}
		ui.setPollInterval(-1);
	}
	
	private void performHashing(ProgressBar progress, UI ui, String password, String salt, 
			AtomicDouble progressValue, float progressIncrement)
	{
		scryptEncoder.scrypt(password, salt.getBytes(StandardCharsets.UTF_8), config.getScryptParams());
		progressValue.addAndGet(progressIncrement);
		ui.access(() -> progress.setValue((float)progressValue.get()));
	}
}