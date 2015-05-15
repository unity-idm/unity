/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;

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

/**
 * {@link X500Identity} editor
 * @author K. Benedyczak
 */
public class X500IdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private boolean required;
	
	public X500IdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(boolean required, boolean adminMode)
	{
		this.required = required;
		field = new TextField();
		field.setWidth(100, Unit.PERCENTAGE);
		Upload upload = new Upload();
		upload.setCaption(msg.getMessage("X500IdentityEditor.certUploadCaption"));
		CertUploader uploader = new CertUploader(); 
		upload.setReceiver(uploader);
		upload.addSucceededListener(uploader);
		
		FormLayout wrapper = new CompactFormLayout(upload);
		wrapper.setMargin(false);
		field.setCaption(new X500Identity().getHumanFriendlyName(msg) + ":");
		field.setRequired(required);
		return new ComponentsContainer(field, wrapper);
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		String dn = field.getValue();
		if (dn.trim().equals(""))
		{
			if (!required)
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
		
		public OutputStream receiveUpload(String filename, String mimeType) 
		{
			fos = new LimitedOuputStream(102400, new ByteArrayOutputStream(102400));
			return fos;
		}

		public void uploadSucceeded(SucceededEvent event) 
		{
			if (fos.isOverflow())
			{
				NotificationPopup.showError(msg, msg.getMessage("X500IdentityEditor.uploadFailed"),
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
	public void setDefaultValue(String value)
	{
		field.setValue(value);	
	}
}
