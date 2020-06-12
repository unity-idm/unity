/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import fi.jasoft.qrcode.QRCode;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

class OTPEditorComponent extends CustomComponent implements Component.Focusable, ComponentWithLabel
{
	private static final Map<HashFunction, Integer> QR_BASE_SIZES = ImmutableMap.of(HashFunction.SHA1, 41, 
			HashFunction.SHA256, 49, HashFunction.SHA512, 53);
	
	private final MessageSource msg;
	private final OTPCredentialDefinition config;
	private final CredentialEditorContext context;
	
	private String secret;
	private int tabIndex;
	
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
		setCompositionRoot(main);
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
		if (!context.isRequired() && !verificationComponent.isVerified())
			return null;
		
		if (context.isRequired() && !verificationComponent.isVerified())
		{
			verificationComponent.setComponentError(new UserError(msg.getMessage("OTPEditorComponent.verificationRequired")));
			throw new MissingCredentialException(msg.getMessage("OTPEditorComponent.verificationRequired"));
		}
		
		OTPCredential credential = new OTPCredential(secret, config.otpParams);
		return JsonUtil.toJsonString(credential);
	}

	void setCredentialError(EngineException error)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setLabel(String label)
	{
		// TODO Auto-generated method stub
		
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
		
		QRCodeComponent()
		{
			VerticalLayout main  = new VerticalLayout();
			main.setMargin(false);
			main.setSpacing(false);
			Label info = new Label(msg.getMessage("OTPEditorComponent.qrCodeInfo"));
			QRCode qr = new QRCode();
			int size = (QR_BASE_SIZES.get(config.otpParams.hashFunction) + 8) * BASE_SIZE_ZOOM;
			qr.setWidth(size, Unit.PIXELS);
			qr.setHeight(size, Unit.PIXELS);			
			//TODO user
			String uri = TOTPKeyGenerator.generateTOTPURI(secret, config.issuerName + " user", 
					config.issuerName, config.otpParams);
			qr.setValue(uri);
			
			main.addComponents(info, qr);
			setCompositionRoot(main);
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
		
		public VerificationComponent()
		{
			code = new TextField(); 
			code.setPlaceholder(msg.getMessage("WebOTPRetrieval.code", 
					config.otpParams.codeLength));
			code.addValueChangeListener(v -> code.setComponentError(null));
			Button verify = new Button(msg.getMessage("OTPEditorComponent.verifyButton"));
			verify.addClickListener(e -> this.verify());
			validationLayout = new HorizontalLayout();
			validationLayout.addComponents(code, verify);

			confirmed = new Label(msg.getMessage("OTPEditorComponent.codeVerified"));
			confirmed.addStyleName(Styles.success.name());
			confirmed.setVisible(false);
			VerticalLayout main = new VerticalLayout(validationLayout, confirmed);
			main.setMargin(new MarginInfo(true, false, false, false));
			main.setSpacing(false);
			setCompositionRoot(main);
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
			} else
			{
				code.setComponentError(new UserError(msg.getMessage("OTPEditorComponent.invalidCode")));
			}
		}
		
		boolean isVerified()
		{
			return validated;
		}
	}
}
