/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.identities.x500;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.forms.registration.SingleStringFieldBinder;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorContext;
import io.imunity.vaadin23.shared.endpoint.components.StringBindingValue;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;

public class X500IdentityEditor implements IdentityEditor
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private TextField field;
	private IdentityEditorContext context;
	private SingleStringFieldBinder binder;
	
	public X500IdentityEditor(MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		binder = new SingleStringFieldBinder(msg);
		this.context = context;
		field = new TextField();
		if (context.isCustomWidth())
			field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		else
			field.setWidthFull();
		Upload upload = new Upload();
		upload.setUploadButton(new Button(msg.getMessage("X500IdentityEditor.certUploadCaption")));
		CertUploader uploader = new CertUploader();
		upload.setReceiver(uploader);
		upload.addSucceededListener(uploader);
		
		setLabel(new X500Identity().getHumanFriendlyName(msg));
		
		binder.forField(field, context.isRequired())
			.withValidator(this::validate)
			.bind("value");
		binder.setBean(new StringBindingValue(""));
		
		return new ComponentsContainer(field, upload);
	}

	private ValidationResult validate(String value, ValueContext context)
	{
		if (value.isEmpty())
			return ValidationResult.ok(); //fall back
		try
		{
			X500NameUtils.getX500Principal(value);
			return ValidationResult.ok();
		} catch (Exception e)
		{
			return ValidationResult.error(msg.getMessage("X500IdentityEditor.dnError", 
					e.getMessage()));
		}
	}
	
	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		binder.ensureValidityCatched(() -> new IllegalIdentityValueException(""));
		String dn = field.getValue().trim();
		if (dn.isEmpty())
			return null;
		return new IdentityParam(X500Identity.ID, dn);
	}

	private class CertUploader implements Receiver, ComponentEventListener<SucceededEvent>
	{
		private LimitedOuputStream fos;
		
		@Override
		public OutputStream receiveUpload(String filename, String mimeType) 
		{
			fos = new LimitedOuputStream(102400, new ByteArrayOutputStream(102400));
			return fos;
		}

		@Override
		public void onComponentEvent(SucceededEvent event)
		{
			if (fos.isOverflow())
			{
				notificationPresenter.showError(msg.getMessage("X500IdentityEditor.uploadFailed"),
						msg.getMessage("X500IdentityEditor.certSizeTooBig"));
				fos = null;
				return;
			}
			try
			{
				byte[] uploaded = ((ByteArrayOutputStream)fos.getWrappedStream()).toByteArray();
				ByteArrayInputStream bis = new ByteArrayInputStream(uploaded);
				X509Certificate loaded = CertificateUtils.loadCertificate(bis, Encoding.PEM);
				field.setValue(X500NameUtils.getReadableForm(loaded.getSubjectX500Principal()));
			} catch (Exception e)
			{
				notificationPresenter.showError(msg.getMessage("X500IdentityEditor.uploadInvalid"), e.getMessage());
				fos = null;
			}
		}
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		binder.setBean(new StringBindingValue(value.getValue()));
	}
	

	@Override
	public void setLabel(String value)
	{
		if (context.isShowLabelInline())
			field.setPlaceholder(value);
		else
			field.setLabel(value + ":");
	}
}
