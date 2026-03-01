/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.x500;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.identity.X500Identity;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;

public class X500IdentityEditor implements IdentityEditor
{
	private static final int MAX_CERT_SIZE = 102400;

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
		{
			field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		} else
		{
			field.setWidthFull();
		}

		Upload upload = new Upload(new InMemoryUploadHandler((metadata, bytes) ->
		{
			if (bytes.length > MAX_CERT_SIZE)
			{
				notificationPresenter.showError(
					msg.getMessage("X500IdentityEditor.uploadFailed"),
					msg.getMessage("X500IdentityEditor.certSizeTooBig")
				);
				return;
			}

			try
			{
				ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
				X509Certificate loaded = CertificateUtils.loadCertificate(bis, Encoding.PEM);
				field.setValue(X500NameUtils.getReadableForm(loaded.getSubjectX500Principal()));
			} catch (Exception e)
			{
				notificationPresenter.showError(msg.getMessage("X500IdentityEditor.uploadInvalid"), e.getMessage());
			}
		}));
		upload.setUploadButton(new Button(msg.getMessage("X500IdentityEditor.certUploadCaption")));
		upload.setMaxFileSize(MAX_CERT_SIZE);

		if (context.isRequired())
		{
			field.setRequiredIndicatorVisible(true);
			field.setTooltipText(msg.getMessage("fieldRequired"));
		}
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
		{
			return ValidationResult.ok();
		}
		try
		{
			X500NameUtils.getX500Principal(value);
			return ValidationResult.ok();
		} catch (Exception e)
		{
			return ValidationResult.error(msg.getMessage("X500IdentityEditor.dnError", e.getMessage()));
		}
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		binder.ensureValidityCatched(() -> new IllegalIdentityValueException(""));
		String dn = field.getValue().trim();
		if (dn.isEmpty())
		{
			return null;
		}
		return new IdentityParam(X500Identity.ID, dn);
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
		{
			field.setPlaceholder(value);
		} else
		{
			field.setLabel(value + ":");
		}
	}
}
