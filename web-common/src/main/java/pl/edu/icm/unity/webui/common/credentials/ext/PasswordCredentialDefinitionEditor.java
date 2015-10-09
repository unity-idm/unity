/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.vaadin.risto.stepper.IntStepper;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.PasswordVerificator;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * {@link CredentialDefinition} editor and viewer for the {@link PasswordVerificator}.
 * @author K. Benedyczak
 */
public class PasswordCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private static final double MS_IN_MONTH = 3600000L*24L*30.41;
	private static final int MAX_MONTHS = 48;
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTplMan;
	private IntStepper minLength;
	private IntStepper minClasses;
	private CheckBox denySequences;
	private CheckBox limitMaxAge;
	private IntStepper maxAge;
	private IntStepper historySize;
	private IntStepper rehashNumber;
	private CredentialResetSettingsEditor resetSettings;
	
	public PasswordCredentialDefinitionEditor(UnityMessageSource msg, MessageTemplateManagement msgTplMan)
	{
		this.msg = msg;
		this.msgTplMan = msgTplMan;
	}


	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		PasswordCredential helper = new PasswordCredential();
		helper.setSerializedConfiguration(credentialDefinitionConfiguration);

		Label minLength = new Label();
		minLength.setCaption(msg.getMessage("PasswordDefinitionEditor.minLength"));
		Label minClasses = new Label();
		minClasses.setCaption(msg.getMessage("PasswordDefinitionEditor.minClasses"));
		Label denySequences = new Label();
		denySequences.setCaption(msg.getMessage("PasswordDefinitionEditor.denySequencesRo"));
		Label historySize = new Label();
		historySize.setCaption(msg.getMessage("PasswordDefinitionEditor.historySize"));
		Label maxAge = new Label();
		maxAge.setCaption(msg.getMessage("PasswordDefinitionEditor.maxAgeRo"));
		Label rehashNumber = new Label();
		rehashNumber.setCaption(msg.getMessage("PasswordDefinitionEditor.rehashNumber"));
		
		CredentialResetSettingsEditor viewer = new CredentialResetSettingsEditor(msg, msgTplMan,
				helper.getPasswordResetSettings());
		
		FormLayout form = new CompactFormLayout(minLength, minClasses, denySequences, historySize, maxAge,
				rehashNumber);
		viewer.addViewerToLayout(form);
		form.setMargin(true);
		
		minLength.setValue(String.valueOf(helper.getMinLength()));
		minClasses.setValue(String.valueOf(helper.getMinClassesNum()));
		denySequences.setValue(helper.isDenySequences() ? msg.getMessage("yes") : msg.getMessage("no"));
		denySequences.setReadOnly(true);
		historySize.setValue(String.valueOf(helper.getHistorySize()));
		rehashNumber.setValue(String.valueOf(helper.getRehashNumber()));
		
		long maxAgeMs = helper.getMaxAge();
		double maxAgeMonths = ((double)maxAgeMs)/MS_IN_MONTH;
		BigDecimal maxAgeMonthsRounded = new BigDecimal(maxAgeMonths, new MathContext(2, RoundingMode.HALF_UP)); 
		long maxAgeDays = maxAgeMs/(3600000L*24);
		if (maxAgeMs == PasswordCredential.MAX_AGE_UNDEF)
			maxAge.setValue(msg.getMessage("PasswordDefinitionEditor.maxAgeUnlimited"));
		else
			maxAge.setValue(msg.getMessage("PasswordDefinitionEditor.maxAgeValue", 
					maxAgeMonthsRounded.doubleValue(), maxAgeDays));
		
		return form;
	}
	
	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		minLength = new IntStepper(msg.getMessage("PasswordDefinitionEditor.minLength"));
		minLength.setMinValue(1);
		minLength.setMaxValue(30);
		minLength.setWidth(3, Unit.EM);
		minClasses = new IntStepper(msg.getMessage("PasswordDefinitionEditor.minClasses"));
		minClasses.setMinValue(1);
		minClasses.setMaxValue(4);
		minClasses.setWidth(3, Unit.EM);
		denySequences = new CheckBox(msg.getMessage("PasswordDefinitionEditor.denySequences"));
		historySize = new IntStepper(msg.getMessage("PasswordDefinitionEditor.historySize"));
		historySize.setMinValue(0);
		historySize.setMaxValue(50);
		historySize.setWidth(3, Unit.EM);
		limitMaxAge = new CheckBox(msg.getMessage("PasswordDefinitionEditor.limitMaxAge"));
		limitMaxAge.setImmediate(true);
		limitMaxAge.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				maxAge.setEnabled(limitMaxAge.getValue());
			}
		});
		maxAge = new IntStepper(msg.getMessage("PasswordDefinitionEditor.maxAge"));
		maxAge.setMinValue(1);
		maxAge.setMaxValue(MAX_MONTHS);
		maxAge.setWidth(3, Unit.EM);
		maxAge.setValue(24);
		rehashNumber = new IntStepper(msg.getMessage("PasswordDefinitionEditor.rehashNumber"));
		rehashNumber.setStepAmount(100);
		rehashNumber.setMinValue(0);
		rehashNumber.setMaxValue(100000);
		rehashNumber.setWidth(5, Unit.EM);

		FormLayout form = new CompactFormLayout(minLength, minClasses, denySequences, historySize, limitMaxAge,
				maxAge, rehashNumber);
		form.setSpacing(true);
		form.setMargin(true);
		PasswordCredential helper = new PasswordCredential();
		if (credentialDefinitionConfiguration != null)
			helper.setSerializedConfiguration(credentialDefinitionConfiguration);
		else
			helper.setRehashNumber(PasswordCredential.DEFAULT_REHASH_NUMBER);
		initUIState(helper);
		resetSettings = new CredentialResetSettingsEditor(msg, msgTplMan, helper.getPasswordResetSettings());
		resetSettings.addEditorToLayout(form);
				
		return form;
	}

	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		PasswordCredential helper = new PasswordCredential();
		helper.setDenySequences(denySequences.getValue());
		helper.setHistorySize((int)(double)historySize.getValue());
		if (limitMaxAge.getValue())
		{
			long maxAgeMs = (long)(double)maxAge.getValue();
			maxAgeMs *= MS_IN_MONTH;
			helper.setMaxAge(maxAgeMs);
		} else
			helper.setMaxAge(PasswordCredential.MAX_AGE_UNDEF);
		helper.setMinClassesNum((int)(double)minClasses.getValue());
		helper.setMinLength((int)(double)minLength.getValue());
		helper.setPasswordResetSettings(resetSettings.getValue());
		helper.setRehashNumber(rehashNumber.getValue());
		try
		{
			return helper.getSerializedConfiguration();
		} catch (InternalException e)
		{
			throw new IllegalCredentialException(e.getMessage(), e);
		}
	}

	private void initUIState(PasswordCredential helper)
	{
		minLength.setValue(helper.getMinLength());
		minClasses.setValue(helper.getMinClassesNum());
		denySequences.setValue(helper.isDenySequences());
		historySize.setValue(helper.getHistorySize());
		long maxAgeMonths = Math.round(helper.getMaxAge() / MS_IN_MONTH);
		if (maxAgeMonths > 40)
			maxAgeMonths = 40;
		if (maxAgeMonths < 1)
			maxAgeMonths = 1;
		if (helper.getMaxAge() != PasswordCredential.MAX_AGE_UNDEF)
		{
			limitMaxAge.setValue(true);
			maxAge.setEnabled(true);
			maxAge.setValue((int)maxAgeMonths);
		} else
		{
			limitMaxAge.setValue(false);
			maxAge.setEnabled(false);
		}
		rehashNumber.setValue(helper.getRehashNumber());
	}
}
