/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.confirmations.MobileNumberConfirmationDialog;

/**
 * Mobile number field with verify button and confirmation info label. For admin also confirm checkbox is visible
 * @author P.Piernik
 *
 */
public class MobileNumberFieldWithVerifyButton extends CustomField<VerifiableMobileNumber>
{
	private VerifiableMobileNumberAttributeSyntax syntax;
	private UnityMessageSource msg;
	private IdentityFormatter formatter;
	private MobileNumberConfirmationManager mobileConfirmationMan;
	private MobileNumberConfirmationConfiguration confirmationConfiguration;
	
	private VerifiableMobileNumber value;
	private ConfirmationInfo confirmationInfo;
	private String label;
	private AbstractTextField valueField;
	private CheckBox adminConfirmCheckbox;
	private Button verifyButton;
	private boolean required;
	private boolean adminMode;
	private Label confirmationInfoLabel;

	
	public MobileNumberFieldWithVerifyButton(VerifiableMobileNumber value, String label,
			MobileNumberConfirmationManager mobileConfirmationMan,
			VerifiableMobileNumberAttributeSyntax syntax, UnityMessageSource msg,
			IdentityFormatter formatter, boolean required, boolean adminMode,
			MobileNumberConfirmationConfiguration confirmationConfiguration)
	{
		this.value = value;
		this.label = label;
		this.mobileConfirmationMan = mobileConfirmationMan;
		this.syntax = syntax;
		this.required = required;
		this.adminMode = adminMode;
		this.msg = msg;
		this.formatter = formatter;
		this.confirmationConfiguration = confirmationConfiguration;

	}

	public String getVerifiableMobileValue() throws IllegalAttributeValueException
	{
		
		if (!required && valueField.getValue().isEmpty())
			return null;
		
		try
		{
			VerifiableMobileNumber mobile = new VerifiableMobileNumber(valueField.getValue());
			if (adminMode)
			{
				mobile.setConfirmationInfo(
						new ConfirmationInfo(adminConfirmCheckbox.getValue()));
			} else
			{
				mobile.setConfirmationInfo(confirmationInfo);
			}
			syntax.validate(mobile);
			valueField.setComponentError(null);
			return syntax.convertToString(mobile);
		} catch (IllegalAttributeValueException e)
		{
			valueField.setComponentError(new UserError(e.getMessage()));
			throw e;
		} catch (Exception e)
		{
			valueField.setComponentError(new UserError(e.getMessage()));
			throw new IllegalAttributeValueException(e.getMessage(), e);
		}		
	}
	
	
	@Override
	public VerifiableMobileNumber getValue()
	{

		try
		{
			return syntax.convertFromString(getVerifiableMobileValue());
		} catch (IllegalAttributeValueException e)
		{

			return null;
		}

	}

	@Override
	protected Component initContent()
	{
		setRequiredIndicatorVisible(required);
		confirmationInfo = value == null ? new ConfirmationInfo()
				: value.getConfirmationInfo();
		confirmationInfoLabel = new Label();			
		HorizontalLayout fieldWithButton = new HorizontalLayout();
		fieldWithButton.setMargin(false);
		fieldWithButton.setSpacing(false);
		valueField = new TextField();	
		if (label != null)
			valueField.setId("MobileNumberValueEditor."+label);
		if (value != null)
			valueField.setValue(value.getValue());
		
		fieldWithButton.addComponent(valueField);
		
		verifyButton = new Button();
		verifyButton.setIcon(Images.verify.getResource());
		verifyButton.setStyleName(Styles.vButtonLink.toString());
		verifyButton.addStyleName(Styles.vButtonBorderless.toString());
		verifyButton.addStyleName(Styles.link.toString());
		verifyButton.addStyleName(Styles.largeIcon.toString());
		
		verifyButton.setDescription(msg.getMessage("VerifiableMobileNumberField.verify"));
		verifyButton.setVisible(!confirmationInfo.isConfirmed());
		if (confirmationConfiguration != null)
			fieldWithButton.addComponent(verifyButton);
		
		adminConfirmCheckbox = new CheckBox(msg.getMessage(
				"VerifiableMobileNumberField.confirmedCheckbox"));
		adminConfirmCheckbox.setValue(confirmationInfo.isConfirmed());
		adminConfirmCheckbox.addValueChangeListener(e -> {
			verifyButton.setVisible(!e.getValue());
		});

		verifyButton.addClickListener(e -> {
			
			String value;
			try
			{
				value = getVerifiableMobileValue();
			} catch (IllegalAttributeValueException e1)
			{
				return; 
			}

			MobileNumberConfirmationDialog confirmationDialog = new MobileNumberConfirmationDialog(
					syntax.convertFromString(value).getValue(),
					confirmationInfo, msg, mobileConfirmationMan,
					confirmationConfiguration,
					new MobileNumberConfirmationDialog.Callback()
					{
						@Override
						public void onConfirm()
						{
							updateConfirmationLabelAndButtons();
						}
					});
			confirmationDialog.show();
		});
			
		valueField.addValueChangeListener(e -> {
			
			if (value != null && e.getValue().equals(value.getValue()))
			{
				confirmationInfo = value.getConfirmationInfo();
			} else
			{
				confirmationInfo = new ConfirmationInfo();
			}
			updateConfirmationLabelAndButtons();
		});
		

		
		if (adminMode)
		{
			adminConfirmCheckbox.setVisible(true);
			confirmationInfoLabel.setVisible(false);
			
		} else
		{
			adminConfirmCheckbox.setVisible(false);
			confirmationInfoLabel.setVisible(true);
		}
		
		
		updateConfirmationLabelAndButtons();
		
		VerticalLayout ret = new VerticalLayout(fieldWithButton, adminConfirmCheckbox, confirmationInfoLabel);
		ret.setMargin(false);
		return ret;
	}

	
	private void updateConfirmationLabelAndButtons()
	{
		confirmationInfoLabel.setValue(formatter.getConfirmationStatusString(
				confirmationInfo));
		verifyButton.setVisible(!confirmationInfo.isConfirmed());
		adminConfirmCheckbox.setValue(confirmationInfo.isConfirmed());	
	}

	@Override
	protected void doSetValue(VerifiableMobileNumber value)
	{
		valueField.setValue(value.getValue());
	}
}
