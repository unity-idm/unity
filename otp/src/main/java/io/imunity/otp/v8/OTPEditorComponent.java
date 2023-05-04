/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8;

import com.google.common.collect.ImmutableMap;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import fi.jasoft.qrcode.QRCode;
import io.imunity.otp.*;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

import java.util.Map;

class OTPEditorComponent extends CustomComponent implements Component.Focusable, ComponentWithLabel
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
		switchCodeComponent.addStyleName(Styles.vButtonLink.toString());
		switchCodeComponent.addStyleName("u-highlightedLink");
		switchCodeComponent.addClickListener(e -> switchCodeComponent());
		
		verificationComponent = new VerificationComponent();
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponents(qrCodeComponent, textCodeComponent, switchCodeComponent, verificationComponent);
		VerticalLayout mainWithSpacing = new VerticalLayout(credentialName, main);
		mainWithSpacing.setSpacing(true);
		mainWithSpacing.setMargin(false);
		setCompositionRoot(mainWithSpacing);
		
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
		switchCodeComponent.setCaption(msg.getMessage(messageKey));
	}
	
	String getValue() throws MissingCredentialException
	{
		if (!context.isRequired() && !verificationComponent.getVerificationStatus())
			return null;
		
		if (context.isRequired() && !verificationComponent.getVerificationStatus())
			throw new MissingCredentialException(msg.getMessage("OTPEditorComponent.verificationRequired"));
		
		verificationComponent.setComponentError(null);
		OTPCredential credential = new OTPCredential(secret, config.otpParams);
		return JsonUtil.toJsonString(credential);
	}

	@Override
	public void setLabel(String label)
	{
		credentialName.setValue(label);
		credentialName.setVisible(!label.isEmpty());
	}

	@Override
	public int getTabIndex()
	{
		return tabIndex;
	}

	@Override
	public void setTabIndex(int tabIndex)
	{
		this.tabIndex = tabIndex;
	}
	
	@Override
	public void focus()
	{
		verificationComponent.focus();
	}
	
	
	private class QRCodeComponent extends CustomComponent
	{
		private static final int BASE_SIZE_ZOOM = 3;
		private QRCode qr;
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
			customizeUser.addStyleName(Styles.vButtonLink.toString());
			customizeUser.addStyleName("u-highlightedLink");
			customizeUser.addClickListener(e -> {customizeUser.setVisible(false); user.setVisible(true);});
			
			qr = new QRCode();
			int size = (QR_BASE_SIZES.get(config.otpParams.hashFunction) + 8) * BASE_SIZE_ZOOM;
			qr.setWidth(size, Unit.PIXELS);
			qr.setHeight(size, Unit.PIXELS);			
			updateQR();
			user.addValueChangeListener(v -> updateQR());
			main.addComponents(user, info, qr, customizeUser);
			main.setComponentAlignment(qr, Alignment.MIDDLE_CENTER);
			setCompositionRoot(main);
		}
		
		private void updateQR()
		{
			String uri = TOTPKeyGenerator.generateTOTPURI(secret, user.getValue(), 
					config.issuerName, config.otpParams, config.logoURI);
			qr.setValue(uri);
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
	
	private class TextCodeComponent extends CustomComponent
	{
		TextCodeComponent()
		{
			Label info = new Label(msg.getMessage("OTPEditorComponent.textCodeInfo"));
			Label code = new Label(formatSecret(secret));
			code.addStyleName("u-textMonospace");
			code.setCaption(msg.getMessage("OTPEditorComponent.textCode"));
			code.setWidth(100, Unit.PERCENTAGE);
			Label type = new Label(msg.getMessage("OTPEditorComponent.textCodeType"));
			type.setCaption(msg.getMessage("OTPEditorComponent.textCodeTypeCaption"));
			Label length = new Label(String.valueOf(config.otpParams.codeLength));
			length.setCaption(msg.getMessage("OTPEditorComponent.textCodeLength"));
			Label algorithm = new Label(config.otpParams.hashFunction.toString());
			algorithm.setCaption(msg.getMessage("OTPEditorComponent.textCodeAlgorithm"));
			FormLayout codeInfoLayout  = new CompactFormLayout();
			codeInfoLayout.setMargin(new MarginInfo(false, false, false, false));
			codeInfoLayout.addComponents(code, type, length, algorithm);
			
			VerticalLayout main = new VerticalLayout(info, codeInfoLayout);
			main.setMargin(false);
			setCompositionRoot(main);
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
	
	private class VerificationComponent extends CustomComponent
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
			code.addValueChangeListener(v -> code.setComponentError(null));
			code.addStyleName("u-otp-verification-code");
			code.setWidthFull();
			Button verify = new Button();
			verify.setIcon(Images.check.getResource());
			verify.setDescription(msg.getMessage("OTPEditorComponent.verifyButton"));
			verify.addClickListener(e -> this.verify());
			verify.addStyleName("u-otp-verification-button");
			verify.setWidthUndefined();
			validationLayout = new HorizontalLayout();
			validationLayout.addComponents(code, verify);
			validationLayout.setExpandRatio(code, 1.0f);
			
			confirmed = new Label(msg.getMessage("OTPEditorComponent.codeVerified"));
			confirmed.addStyleName(Styles.success.name());
			confirmed.setVisible(false);
			main = new VerticalLayout(validationLayout, confirmed);
			main.setMargin(new MarginInfo(true, false, false, false));
			main.setSpacing(false);
			setCompositionRoot(main);
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
		
		private void verify()
		{
			validated = TOTPCodeVerificator.verifyCode(code.getValue(), secret, config.otpParams,
					config.allowedTimeDriftSteps);
			if (validated)
			{
				validationLayout.setVisible(false);
				confirmed.setVisible(true);
				qrCodeComponent.setReadOnly();
				code.setComponentError(null);
			} else
			{
				code.setComponentError(new UserError(msg.getMessage("OTPEditorComponent.invalidCode")));
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
