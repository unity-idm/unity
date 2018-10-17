/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

/**
 * Password quality meter with all password hints that can be configured.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class PasswordQualityComponent extends CustomComponent
{
	private final UnityMessageSource msg;
	private final PasswordCredential config;

	private ProgressBar qualityMeter;
	private Label mainInfo;
	private Label minLengthStatus;
	private Label minClassesStatus;
	private Label sequencesStatus;

	public PasswordQualityComponent(UnityMessageSource msg, PasswordCredential config, 
			CredentialEditorContext context)
	{
		super();
		this.msg = msg;
		this.config = config;
		
		initComponent(context);
	}

	private void initComponent(CredentialEditorContext context)
	{
		qualityMeter = new ProgressBar();
		qualityMeter.setCaption(msg.getMessage("PasswordCredentialEditor.qualityMeter"));
		qualityMeter.setWidth(15, Unit.EM);
		qualityMeter.addStyleName("u-password-quality");
		mainInfo = new Label("", ContentMode.HTML);
		mainInfo.setWidth(16, Unit.EM);
		mainInfo.addStyleName(Styles.emphasized.toString());
		mainInfo.addStyleName("u-password-hint");
		minLengthStatus = new Label("", ContentMode.HTML);
		minLengthStatus.addStyleNames("u-password-stat", "u-password-minLen");
		minClassesStatus = new Label("", ContentMode.HTML);
		minClassesStatus.addStyleNames("u-password-stat", "u-password-minClass");
		sequencesStatus = new Label("", ContentMode.HTML);
		sequencesStatus.addStyleNames("u-password-stat", "u-password-seq");
		
		VerticalLayout root = new VerticalLayout();
		root.addStyleName(Styles.leftMarginSmall.toString());
		root.addStyleName(Styles.passwordQuality.toString());
		root.setSpacing(false);
		root.setMargin(false);
		root.addComponent(qualityMeter);
		root.setWidth(16, Unit.EM);
		if (config.getMinLength() > 1)
			root.addComponent(minLengthStatus);
		if (config.getMinClassesNum() > 1)
			root.addComponent(minClassesStatus);
		if (config.isDenySequences())
			root.addComponent(sequencesStatus);
		root.addComponent(mainInfo);
		setCompositionRoot(root);
		
		if (!context.isShowLabelInline())
			addStyleName(Styles.nonCompactTopMargin.toString());
		
		onNewPassword("");
	}

	public void onNewPassword(String password)
	{
		StrengthInfo measure = StrengthChecker.measure(password, config.getMinScore(), msg);
		int length = password.length();
		boolean trivialSequences = StrengthChecker.hasNoTrivialSequences(password);
		int classes = StrengthChecker.getCharacterClasses(password);
		
		qualityMeter.setValue((float)measure.scoreNormalized);
		if (config.getMinScore() > 0)
		{
			boolean isScoreOK = measure.score >= config.getMinScore();
			Images qualityIcon = isScoreOK ? Images.ok : Images.warn;
			qualityMeter.setIcon(qualityIcon.getResource());
			qualityMeter.setStyleName(Styles.iconError.toString(), !isScoreOK);
			if (isScoreOK)
			{
				qualityMeter.removeStyleName(Styles.redProgressBar.toString());
				qualityMeter.setStyleName(Styles.greenProgressBar.toString());
			}
			else
			{
				qualityMeter.removeStyleName(Styles.greenProgressBar.toString());
				qualityMeter.setStyleName(Styles.redProgressBar.toString());
			}
		}
		
		if (!measure.warning.isEmpty())
		{
			mainInfo.setValue(msg.getMessage("PasswordCredentialEditor.hint", 
					measure.warning));
		} else if (!measure.suggestions.isEmpty())
		{
			mainInfo.setValue(msg.getMessage("PasswordCredentialEditor.hint", 
					measure.suggestions.get(0)));
		} else 
		{
			mainInfo.setValue("");
		}
		
		styleStatusLabel(minLengthStatus, length >= config.getMinLength(), 
				msg.getMessage("PasswordCredentialEditor.minLengthStatus", 
						length, config.getMinLength()));

		styleStatusLabel(minClassesStatus, classes >= config.getMinClassesNum(), 
				msg.getMessage("PasswordCredentialEditor.minClassesStatus", 
						classes, config.getMinClassesNum()));

		styleStatusLabel(sequencesStatus, trivialSequences, 
				msg.getMessage("PasswordCredentialEditor.trivialSequences"));
	}
	
	private void styleStatusLabel(Label label, boolean status, String message)
	{
		String icon = status ? Images.ok.getHtml() : Images.warn.getHtml();
		label.setValue(icon + " " + message);
		label.setStyleName(Styles.iconError.toString(), !status);
	}
}
