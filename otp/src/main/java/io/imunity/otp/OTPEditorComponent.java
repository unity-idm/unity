/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.google.common.collect.ImmutableMap;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.forms.components.QRCodeFactory;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

import java.util.Map;

import static io.imunity.vaadin.elements.CssClassNames.POINTER;

class OTPEditorComponent extends VerticalLayout
{
	private static final Map<HashFunction, Integer> QR_BASE_SIZES = ImmutableMap.of(HashFunction.SHA1, 41,
			HashFunction.SHA256, 49, HashFunction.SHA512, 53);
	
	private final MessageSource msg;
	private final OTPCredentialDefinition config;
	private final CredentialEditorContext context;
	
	private final String secret;

	private final Span credentialName;
	private final QRCodeComponent qrCodeComponent;
	private final TextCodeComponent textCodeComponent;
	private final VerificationComponent verificationComponent;
	private final Div switchCodeComponent;
	
	
	OTPEditorComponent(MessageSource msg, CredentialEditorContext context, OTPCredentialDefinition config)
	{
		this.msg = msg;
		this.context = context;
		this.config = config;
		secret = TOTPKeyGenerator.generateRandomBase32EncodedKey(config.otpParams.hashFunction);

		credentialName = new Span();
		credentialName.setVisible(false);
		
		qrCodeComponent = new QRCodeComponent();
		
		textCodeComponent = new TextCodeComponent();
		textCodeComponent.setVisible(false);

		Span switchCodeLabel = new Span(msg.getMessage("OTPEditorComponent.switchModeToText"));
		switchCodeLabel.addClassName(POINTER.getName());
		switchCodeComponent = new Div(switchCodeLabel);
		switchCodeComponent.addClassName(CssClassNames.UNDERLINE.getName());
		switchCodeComponent.addClickListener(e -> switchCodeComponent());
		
		verificationComponent = new VerificationComponent();
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setPadding(false);
		main.setSpacing(false);
		main.add(qrCodeComponent, textCodeComponent, switchCodeComponent, verificationComponent);

		setSpacing(true);
		setMargin(false);
		setPadding(false);
		add(credentialName, main);
		
		if (context.isCustomWidth())
		{
			setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			qrCodeComponent.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			textCodeComponent.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			verificationComponent.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		}
	}

	private void switchCodeComponent()
	{
		boolean state = textCodeComponent.isVisible();
		textCodeComponent.setVisible(!state);
		qrCodeComponent.setVisible(state);
		String messageKey = state ? "OTPEditorComponent.switchModeToText" : "OTPEditorComponent.switchModeToQR";
		switchCodeComponent.removeAll();
		Span label = new Span(msg.getMessage(messageKey));
		label.addClassName(POINTER.getName());
		switchCodeComponent.add(label);
	}
	
	String getValue() throws MissingCredentialException
	{
		if (!context.isRequired() && !verificationComponent.getVerificationStatus())
			return null;
		
		if (context.isRequired() && !verificationComponent.getVerificationStatus())
			throw new MissingCredentialException(msg.getMessage("OTPEditorComponent.verificationRequired"));
		
		verificationComponent.setError(null);
		OTPCredential credential = new OTPCredential(secret, config.otpParams);
		return JsonUtil.toJsonString(credential);
	}

	public void setLabel(String label)
	{
		credentialName.setText(label);
		credentialName.setVisible(!label.isEmpty());
	}
	
	public void focus()
	{
		verificationComponent.focus();
	}
	
	
	private class QRCodeComponent extends VerticalLayout
	{
		private static final int BASE_SIZE_ZOOM = 3;
		private final TextField user;
		private Image qr;

		QRCodeComponent()
		{
			setMargin(false);
			setPadding(false);

			Span info = new Span(msg.getMessage("OTPEditorComponent.qrCodeInfo"));
			user = new TextField(msg.getMessage("OTPEditorComponent.user"));
			user.setValue(config.issuerName + " user");
			user.setVisible(false);
			user.getStyle().set("padding", "0");

			Span customUserLabel = new Span(msg.getMessage("OTPEditorComponent.customizeUser"));
			customUserLabel.addClassName(POINTER.getName());
			Div customizeUser = new Div(customUserLabel);
			customizeUser.addClassName(CssClassNames.UNDERLINE.getName());
			customizeUser.addClickListener(e -> {customizeUser.setVisible(false); user.setVisible(true);});
			
			qr = new Image();
			qr.getStyle().set("align-self", "center");
			updateQR();
			user.addValueChangeListener(v ->
			{
				if(qr != null)
					remove(qr);
				updateQR();
				addComponentAtIndex(3, qr);
			});
			add(user, info, qr, customizeUser);
		}
		
		private void updateQR()
		{
			int size = (QR_BASE_SIZES.get(config.otpParams.hashFunction) + 8) * BASE_SIZE_ZOOM;
			String uri = TOTPKeyGenerator.generateTOTPURI(secret, user.getValue(),
					config.issuerName, config.otpParams, config.logoURI);
			qr = QRCodeFactory.createQRCode(uri, size);
			qr.getStyle().set("align-self", "center");
		}
		
		void setReadOnly()
		{
			user.setReadOnly(true);
		}
		
		@Override
		public void setWidth(float width, Unit unit)
		{
			super.setWidth(width, unit);
			if (user != null)
				user.setWidth(width, unit);
		}
	}
	
	private class TextCodeComponent extends VerticalLayout
	{
		TextCodeComponent()
		{
			Span info = new Span(msg.getMessage("OTPEditorComponent.textCodeInfo"));
			Span code = new Span(formatSecret(secret));
			code.addClassName("u-textMonospace");
			code.setWidthFull();

			Span type = new Span(msg.getMessage("OTPEditorComponent.textCodeType"));
			Span length = new Span(String.valueOf(config.otpParams.codeLength));
			Span algorithm = new Span(config.otpParams.hashFunction.toString());

			FormLayout codeInfoLayout  = new FormLayout();
			codeInfoLayout.addFormItem(code, msg.getMessage("OTPEditorComponent.textCode"));
			codeInfoLayout.addFormItem(type, msg.getMessage("OTPEditorComponent.textCodeTypeCaption"));
			codeInfoLayout.addFormItem(length, msg.getMessage("OTPEditorComponent.textCodeLength"));
			codeInfoLayout.addFormItem(algorithm, msg.getMessage("OTPEditorComponent.textCodeAlgorithm"));

			setMargin(false);
			setPadding(false);
			add(info, codeInfoLayout);
		}
		
		private String formatSecret(String secret)
		{
			StringBuilder sb = new StringBuilder();
			char[] charArray = secret.toCharArray();
			for (int i=0; i<charArray.length; i++)
			{
				sb.append(charArray[i]);
				if (((i+1) % 4 == 0))
					sb.append(' ');
			}
			return sb.toString();
		}
	}
	
	private class VerificationComponent extends VerticalLayout implements Focusable<VerificationComponent>
	{
		private final TextField code;
		private boolean validated;
		private final HorizontalLayout validationLayout;
		private final Span confirmed;
		public VerificationComponent()
		{
			code = new TextField(); 
			code.setPlaceholder(msg.getMessage("OTPRetrieval.code", 
					config.otpParams.codeLength));
			code.addValueChangeListener(v -> code.setErrorMessage(null));
			code.addClassName("u-otp-verification-code");
			code.setWidthFull();
			Button verify = new Button();
			verify.setIcon(VaadinIcon.CHECK.create());
			verify.setTooltipText(msg.getMessage("OTPEditorComponent.verifyButton"));
			verify.addClickListener(e -> this.verify());
			verify.addClassName("u-otp-verification-button");
			validationLayout = new HorizontalLayout();
			validationLayout.setMargin(false);
			validationLayout.setPadding(false);
			validationLayout.setSpacing(false);
			validationLayout.add(code, verify);
			validationLayout.setSpacing(true);

			confirmed = new Span(msg.getMessage("OTPEditorComponent.codeVerified"));
			confirmed.setVisible(false);

			setMargin(false);
			setPadding(false);
			setSpacing(false);
			add(validationLayout, confirmed);
		}
		
		@Override
		public void setWidth(float width, Unit unit)
		{
			super.setWidth(width, unit);
			validationLayout.setWidth(width, unit);
			confirmed.setWidth(100, Unit.PERCENTAGE);
		}
		
		@Override
		public void focus()
		{
			code.focus();
		}

		public void setError(String error)
		{
			code.setInvalid(error != null);
			code.setErrorMessage(error);
		}

		private void verify()
		{
			validated = TOTPCodeVerificator.verifyCode(code.getValue(), secret, config.otpParams,
					config.allowedTimeDriftSteps);
			if (validated)
			{
				validationLayout.setVisible(false);
				confirmed.setVisible(true);
				qrCodeComponent.setReadOnly();
				code.setInvalid(false);
				code.setErrorMessage(null);
			} else
			{
				code.setInvalid(true);
				code.setErrorMessage(msg.getMessage("OTPEditorComponent.invalidCode"));
			}
		}
		
		boolean getVerificationStatus()
		{
			if (validated)
				return true;
			verify();
			return validated;
		}
	}
}
