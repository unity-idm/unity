/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Random;

import org.bouncycastle.crypto.generators.SCrypt;

import com.google.common.util.concurrent.AtomicDouble;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.ScryptParams;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Shows a small dialog which first measures average password hashing time with given settings,
 * then presents the result with some hints.
 * 
 * @author K. Benedyczak
 */
class TestWorkFactorDialog extends AbstractDialog
{
	private PasswordCredential config;
	private static final int WARM_UP = 5;
	private static final int TEST = 10;
	private static final float TOO_FAST_THRESHOLD = 0.1f;
	private static final float TOO_SLOW_THRESHOLD = 0.49f;
	private VerticalLayout layout;
	
	public TestWorkFactorDialog(UnityMessageSource msg, PasswordCredential config)
	{
		super(msg, msg.getMessage("PasswordDefinitionEditor.testWorkFactor"), 
				msg.getMessage("close"));
		this.config = config;
		setSizeEm(45, 25);
	}

	@Override
	protected Component getContents() throws Exception
	{
		layout = new VerticalLayout();
		layout.setSizeFull();
		ProgressBar progress = new ProgressBar();
		progress.setCaption(msg.getMessage("PasswordDefinitionEditor.progress"));
		layout.addComponent(progress);
		layout.setComponentAlignment(progress, Alignment.MIDDLE_CENTER);
		progress.setWidth(80, Unit.PERCENTAGE);
		UI ui = UI.getCurrent();
		assert ui != null;
		Thread asyncMeasure = new Thread(() -> measureAvgHashTime(progress, ui));
		asyncMeasure.start();
		return layout;
	}

	private void showFinalResults(float time)
	{
		layout.removeAllComponents();
		DecimalFormat df = new DecimalFormat("0.####");
		Label resultInfo = new Label(msg.getMessage("PasswordDefinitionEditor.hashingTimeInfo"));
		Label resultTime = new Label(msg.getMessage("PasswordDefinitionEditor.hashingTime", 
				df.format(time)));
		resultTime.addStyleName(Styles.textLarge.toString());
		
		String hintCode = "PasswordDefinitionEditor.hashingOK";
		if (time < TOO_FAST_THRESHOLD)
			hintCode = "PasswordDefinitionEditor.hashingTooFast";
		else if (time > TOO_SLOW_THRESHOLD)
			hintCode = "PasswordDefinitionEditor.hashingSlow";
		Label resultComment = new Label(msg.getMessage(hintCode));
		resultComment.setWidth(100, Unit.PERCENTAGE);
		resultComment.addStyleName(Styles.textCenter.toString());
		
		layout.addComponents(resultInfo, resultTime, new Label(), resultComment);
		layout.setComponentAlignment(resultInfo, Alignment.MIDDLE_CENTER);
		layout.setComponentAlignment(resultTime, Alignment.MIDDLE_CENTER);
		layout.setComponentAlignment(resultComment, Alignment.MIDDLE_CENTER);
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
		ui.setPollInterval(-1);
	}
	
	private void performHashing(ProgressBar progress, UI ui, String password, String salt, 
			AtomicDouble progressValue, float progressIncrement)
	{
		encode(password, salt, config.getScryptParams());
		progressValue.addAndGet(progressIncrement);
		ui.access(() -> progress.setValue((float)progressValue.get()));
	}
	
	private byte[] encode(String password, String salt, ScryptParams scryptParams)
	{
		return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), 
				salt.getBytes(StandardCharsets.UTF_8),
				1 << scryptParams.getWorkFactor(), 
				scryptParams.getBlockSize(),
				scryptParams.getParallelization(),
				scryptParams.getLength());
	}
	
	@Override
	protected void onConfirm()
	{
		close();
	}
}