/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v23;

import com.google.common.collect.ImmutableMap;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.otp.*;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import org.vaadin.barcodes.Barcode;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

import java.util.Map;

class OTPEditorComponent extends VerticalLayout
{
	private static final Map<HashFunction, Integer> QR_BASE_SIZES = ImmutableMap.of(HashFunction.SHA1, 41,
			HashFunction.SHA256, 49, HashFunction.SHA512, 53);
	
	private final MessageSource msg;
	private final OTPCredentialDefinition config;
	private final CredentialEditorContext context;
	
	private String secret;
	private int tabIndex;
	
	private Label credentialName;
	private QRCodeComponent qrCodeComponent;
	private TextCodeComponent textCodeComponent;
	private VerificationComponent verificationComponent;
	private Button switchCodeComponent;
	
	
	OTPEditorComponent(MessageSource msg, CredentialEditorContext context, OTPCredentialDefinition config)
	{
		this.msg = msg;
		this.context = context;
		this.config = config;
		secret = TOTPKeyGenerator.generateRandomBase32EncodedKey(config.otpParams.hashFunction);

		credentialName = new Label();
		credentialName.setVisible(false);
		
		qrCodeComponent = new QRCodeComponent();
		
		textCodeComponent = new TextCodeComponent();
		textCodeComponent.setVisible(false);
		
		switchCodeComponent = new Button(msg.getMessage("OTPEditorComponent.switchModeToText"));
		switchCodeComponent.addClassName("u-highlightedLink");
		switchCodeComponent.addClickListener(e -> switchCodeComponent());
		
		verificationComponent = new VerificationComponent();
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.add(qrCodeComponent, textCodeComponent, switchCodeComponent, verificationComponent);
		VerticalLayout mainWithSpacing = new VerticalLayout(credentialName, main);
		mainWithSpacing.setSpacing(true);
		mainWithSpacing.setMargin(false);
		add(mainWithSpacing);
		
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
		switchCodeComponent.setText(msg.getMessage(messageKey));
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
		private Barcode qr;
		private TextField user;
		
		QRCodeComponent()
		{
			VerticalLayout main  = new VerticalLayout();
			main.setMargin(false);
			main.setSpacing(true);
			Label info = new Label(msg.getMessage("OTPEditorComponent.qrCodeInfo"));
			user = new TextField(msg.getMessage("OTPEditorComponent.user"));
			user.setValue(config.issuerName + " user");
			user.setVisible(false);
			
			Button customizeUser = new Button(msg.getMessage("OTPEditorComponent.customizeUser"));
			customizeUser.addClassName("u-highlightedLink");
			customizeUser.addClickListener(e -> {customizeUser.setVisible(false); user.setVisible(true);});
			
			int size = (QR_BASE_SIZES.get(config.otpParams.hashFunction) + 8) * BASE_SIZE_ZOOM;
			qr = new Barcode("", Barcode.Type.qrcode, size+"px", size+"px");
			updateQR();
			user.addValueChangeListener(v ->
			{
				if(qr != null)
					main.remove(qr);
				updateQR();
				addComponentAtIndex(3, qr);
			});
			main.add(user, info, qr, customizeUser);
			main.setAlignItems(Alignment.CENTER);
			add(main);
		}
		
		private void updateQR()
		{
			int size = (QR_BASE_SIZES.get(config.otpParams.hashFunction) + 8) * BASE_SIZE_ZOOM;
			String uri = TOTPKeyGenerator.generateTOTPURI(secret, user.getValue(),
					config.issuerName, config.otpParams, config.logoURI);
			qr = new Barcode(uri, Barcode.Type.qrcode, size+"px", size+"px");
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
			Label info = new Label(msg.getMessage("OTPEditorComponent.textCodeInfo"));
			Label code = new Label(formatSecret(secret));
			code.addClassName("u-textMonospace");
			code.setText(msg.getMessage("OTPEditorComponent.textCode"));
			code.setWidth(100, Unit.PERCENTAGE);
			Label type = new Label(msg.getMessage("OTPEditorComponent.textCodeType"));
			type.setText(msg.getMessage("OTPEditorComponent.textCodeTypeCaption"));
			Label length = new Label(String.valueOf(config.otpParams.codeLength));
			length.setText(msg.getMessage("OTPEditorComponent.textCodeLength"));
			Label algorithm = new Label(config.otpParams.hashFunction.toString());
			algorithm.setText(msg.getMessage("OTPEditorComponent.textCodeAlgorithm"));
			FormLayout codeInfoLayout  = new FormLayout();
			codeInfoLayout.add(code, type, length, algorithm);
			
			VerticalLayout main = new VerticalLayout(info, codeInfoLayout);
			main.setMargin(false);
			add(main);
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
		private TextField code;
		private boolean validated;
		private HorizontalLayout validationLayout;
		private Label confirmed;
		private VerticalLayout main;
		
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
			verify.getElement().setProperty("title" ,msg.getMessage("OTPEditorComponent.verifyButton"));
			verify.addClickListener(e -> this.verify());
			verify.addClassName("u-otp-verification-button");
			validationLayout = new HorizontalLayout();
			validationLayout.add(code, verify);

			confirmed = new Label(msg.getMessage("OTPEditorComponent.codeVerified"));
			confirmed.setVisible(false);
			main = new VerticalLayout(validationLayout, confirmed);
			main.setMargin(true);
			main.setSpacing(false);
			add(main);
		}
		
		@Override
		public void setWidth(float width, Unit unit)
		{
			super.setWidth(width, unit);
			if (main != null)
				main.setWidth(width, unit);
			if (validationLayout != null)
				validationLayout.setWidth(width, unit);
			if (confirmed != null)
				confirmed.setWidth(100, Unit.PERCENTAGE);

		}
		
		@Override
		public void focus()
		{
			code.focus();
		}

		public void setError(String error)
		{
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
				code.setErrorMessage(null);
			} else
			{
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
