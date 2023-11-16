/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;


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
		qualityMeter.setWidth("17em");
		qualityMeter.addClassName("u-password-quality");

		add(qualityMeter);

		if(context.getCustomWidth() != null && context.getCustomWidthUnit() != null)
		{
			getStyle().set("position", "absolute");
			getStyle().set("margin-left", context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
		}

		if (context.isShowLabelInline())
			setClassName("u-no-padding-top");

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
