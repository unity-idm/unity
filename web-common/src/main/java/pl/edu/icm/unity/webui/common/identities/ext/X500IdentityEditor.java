/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;

/**
 * {@link X500Identity} editor
 * @author K. Benedyczak
 */
public class X500IdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private IdentityEditorContext context;
	
	public X500IdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		this.context = context;
		field = new TextField();
		field.setWidth(80, Unit.PERCENTAGE);
		Upload upload = new Upload();
		upload.setCaption(msg.getMessage("X500IdentityEditor.certUploadCaption"));
		CertUploader uploader = new CertUploader(); 
		upload.setReceiver(uploader);
		upload.addSucceededListener(uploader);
		
		FormLayout wrapper = new CompactFormLayout(upload);
		wrapper.setMargin(false);
		setLabel(new X500Identity().getHumanFriendlyName(msg));
		field.setRequiredIndicatorVisible(context.isRequired());
		return new ComponentsContainer(field, wrapper);
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		String dn = field.getValue();
		if (dn.trim().equals(""))
		{
			if (!context.isRequired())
				return null;
			String err = msg.getMessage("X500IdentityEditor.dnEmpty");
			field.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		try
		{
			X500NameUtils.getX500Principal(dn);
			field.setComponentError(null);
		} catch (Exception e)
		{
			field.setComponentError(new UserError(msg.getMessage("X500IdentityEditor.dnError", 
					e.getMessage())));
			throw new IllegalIdentityValueException(e.getMessage());
		}
		return new IdentityParam(X500Identity.ID, dn);
	}

	private class CertUploader implements Receiver, SucceededListener {
		private LimitedOuputStream fos;
		
		@Override
		public OutputStream receiveUpload(String filename, String mimeType) 
		{
			fos = new LimitedOuputStream(102400, new ByteArrayOutputStream(102400));
			return fos;
		}

		@Override
		public void uploadSucceeded(SucceededEvent event) 
		{
			if (fos.isOverflow())
			{
				NotificationPopup.showError(msg.getMessage("X500IdentityEditor.uploadFailed"),
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
				NotificationPopup.showError(msg, msg.getMessage("X500IdentityEditor.uploadInvalid"),
						e);
				fos = null;
			}
		}
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		field.setValue(value.getValue());	
	}
	

	@Override
	public void setLabel(String value)
	{
		if (context.isShowLabelInline())
			field.setPlaceholder(value);
		else
			field.setCaption(value + ":");
	}
}
