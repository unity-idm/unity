/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Password editor. This component contains a single masked text field, displays password strength
 * and fulfillment of credential settings. It is not maintaining any additional widgets
 * as password re-typing field.
 * 
 * @author K. Benedyczak
 */
public class PasswordEditComponent
{
	private final UnityMessageSource msg;
	private final PasswordCredential config;
	
	private PasswordField password;
	private ProgressBar qualityMeter;
	private Label mainInfo;
	private Label minLengthStatus;
	private Label minClassesStatus;
	private Label sequencesStatus;
	private ComponentsContainer root;
	
	public PasswordEditComponent(UnityMessageSource msg, PasswordCredential config)
	{
		this.msg = msg;
		this.config = config;
		root = new ComponentsContainer();
		
		password = new PasswordField(msg.getMessage("PasswordCredentialEditor.password"));
		password.setValueChangeMode(ValueChangeMode.LAZY);
		password.addValueChangeListener(event -> onNewPassword(event.getValue()));
		password.addStyleName("u-password-setup");
		qualityMeter = new ProgressBar();
		qualityMeter.setCaption(msg.getMessage("PasswordCredentialEditor.qualityMeter"));
		qualityMeter.setWidth(210, Unit.PIXELS);
		qualityMeter.addStyleName("u-password-quality");
		mainInfo = new Label("", ContentMode.HTML);
		mainInfo.setWidth(100, Unit.PERCENTAGE);
		mainInfo.addStyleName(Styles.emphasized.toString());
		mainInfo.addStyleName("u-password-hint");
		VerticalLayout qualityMeterLayout = new VerticalLayout(qualityMeter, mainInfo);
		qualityMeterLayout.setMargin(false);
		qualityMeterLayout.setSpacing(false);
		minLengthStatus = new Label("", ContentMode.HTML);
		minLengthStatus.addStyleNames("u-password-stat", "u-password-minLen");
		minClassesStatus = new Label("", ContentMode.HTML);
		minClassesStatus.addStyleNames("u-password-stat", "u-password-minClass");
		sequencesStatus = new Label("", ContentMode.HTML);
		sequencesStatus.addStyleNames("u-password-stat", "u-password-seq");
		
		root.add(password, qualityMeterLayout);
		if (config.getMinLength() > 1)
			root.add(minLengthStatus);
		if (config.getMinClassesNum() > 1)
			root.add(minClassesStatus);
		if (config.isDenySequences())
			root.add(sequencesStatus);
		
		onNewPassword("");
	}
	
	public ComponentsContainer getAsContainer()
	{
		return root;
	}
	
	public String getPassword()
	{
		return password.getValue();
	}
	
	public void focus()
	{
		password.focus();
	}
	
	public void clear()
	{
		password.setValue("");
	}
	
	public void setRequiredIndicatorVisible(boolean visible)
	{
		password.setRequiredIndicatorVisible(visible);
	}

	public void setComponentError(ErrorMessage componentError)
	{
		password.setComponentError(componentError);
	}
	
	public boolean isValid()
	{
		String password = this.password.getValue();
		StrengthInfo measure = StrengthChecker.measure(password, config.getMinScore(), msg);
		if (measure.score < config.getMinScore())
			return false;
		if (password.length() < config.getMinLength())
			return false;
		if (StrengthChecker.getCharacterClasses(password) < config.getMinClassesNum())
			return false;
		if (!StrengthChecker.hasNoTrivialSequences(password))
			return false;
		return true;
	}
	
	private void onNewPassword(String password)
	{
		StrengthInfo measure = StrengthChecker.measure(password, config.getMinScore(), msg);
		int length = password.length();
		boolean trivialSequences = StrengthChecker.hasNoTrivialSequences(password);
		int classes = StrengthChecker.getCharacterClasses(password);
		
		qualityMeter.setValue((float)measure.scoreNormalized);
		if (config.getMinScore() > 0)
		{
			Images qualityIcon = measure.score >= config.getMinScore() ? 
					Images.ok : Images.warn;
			qualityMeter.setIcon(qualityIcon.getResource());
			qualityMeter.setStyleName(Styles.iconError.toString(), 
					measure.score < config.getMinScore());
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
