/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.password;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;
import pl.edu.icm.unity.webui.common.Styles;


public class PasswordQualityComponent extends VerticalLayout
{
	private final MessageSource msg;
	private final PasswordCredential config;
	private PasswordProgressBar qualityMeter;

	public PasswordQualityComponent(MessageSource msg, PasswordCredential config,
	                                CredentialEditorContext context)
	{
		super();
		this.msg = msg;
		this.config = config;
		
		initComponent(context);
	}

	private void initComponent(CredentialEditorContext context)
	{
		qualityMeter = new PasswordProgressBar(config.getMinLength() > 1, config.getMinClassesNum() > 1, config.isDenySequences());
		qualityMeter.setTitle(msg.getMessage("PasswordCredentialEditor.qualityMeter"));
		qualityMeter.setWidth(15, Unit.EM);
		qualityMeter.addClassName("u-password-quality");
			
		VerticalLayout root = new VerticalLayout();
		root.setSpacing(false);
		root.setMargin(false);

		root.add(qualityMeter);
		root.setWidth(16, Unit.EM);

		add(root);
		if (!context.isShowLabelInline())
			addClassName(Styles.nonCompactTopMargin.toString());
		
		onNewPassword("");
	}

	public void onNewPassword(String password)
	{
		StrengthInfo measure = StrengthChecker.measure(password, config.getMinScore(), msg);
		int length = password.length();
		boolean trivialSequences = StrengthChecker.hasNoTrivialSequences(password);
		int classes = StrengthChecker.getCharacterClasses(password);
		
		qualityMeter.setValue(measure.scoreNormalized);
		if (config.getMinScore() > 0)
		{
			boolean isScoreOK = measure.score >= config.getMinScore();
			qualityMeter.setTitleIcon(isScoreOK);
			if (isScoreOK)
				qualityMeter.setColorToGreen();
			else
				qualityMeter.setColorToRed();
		}
		
		if (!measure.warning.isEmpty())
		{
			qualityMeter.setHint(msg.getMessage("PasswordCredentialEditor.hint", measure.warning));
		} else if (!measure.suggestions.isEmpty())
		{
			qualityMeter.setHint(msg.getMessage("PasswordCredentialEditor.hint", measure.suggestions.get(0)));
		} else 
		{
			qualityMeter.setHint("");
		}

		qualityMeter.setMinLengthStatus(
				msg.getMessage("PasswordCredentialEditor.minLengthStatus", length, config.getMinLength()),
				length >= config.getMinLength()
		);

		qualityMeter.setMinClassesStatus(
				msg.getMessage("PasswordCredentialEditor.minClassesStatus", classes, config.getMinClassesNum()),
				classes >= config.getMinClassesNum()
		);

		qualityMeter.setSequencesStatus(msg.getMessage("PasswordCredentialEditor.trivialSequences"), trivialSequences);
	}

}