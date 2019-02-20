/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki.cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import com.vaadin.data.Binder;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import io.imunity.webadmin.utils.FileUploder;
import io.imunity.webconsole.WebConsoleConstans;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;

/**
 * Certificate editor with details panel
 * 
 * @author P.Piernik
 *
 */
class CertificateEditor extends CustomComponent
{
	private TextField name;
	private TextArea value;
	private Binder<CertificateForBinder> binder;
	private UnityMessageSource msg;
	private FileUploder uploader;
	private FormLayout certDetails;

	CertificateEditor(UnityMessageSource msg, UnityServerConfiguration serverConfig, NamedCertificate toEdit)
	{
		this.msg = msg;

		name = new TextField(msg.getMessage("Certificate.name"));
		name.setWidth(100, Unit.PERCENTAGE);

		Label fileUploaded = new Label();
		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		Upload upload = new Upload();

		uploader = new FileUploder(upload, progress, fileUploaded, msg,
				serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true), () -> {
					reloadValueFromFile();
				});
		uploader.register();
		upload.setCaption("");
		upload.setButtonCaption(msg.getMessage("CertificateEditor.uploadButtonCaption"));

		value = new TextArea(msg.getMessage("Certificate.value"));
		value.setWidth(100, Unit.PERCENTAGE);
		value.setHeight(30, Unit.EM);
		value.addValueChangeListener(e -> refreshDetails());

		binder = new Binder<>(CertificateForBinder.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.forField(value)
				.withValidator(s -> validateCert(s),
						msg.getMessage("CertificateEditor.invalidCertFormat"))
				.withConverter(s -> getCertFromString(s), c -> getCertAsString(c))

				.asRequired(msg.getMessage("fieldRequired")).bind("value");

		FormLayout editorLayout = new FormLayout();
		editorLayout.addComponents(name, upload, progress, value);
		editorLayout.setWidth(WebConsoleConstans.MEDIUM_EDITOR_WIDTH,
				WebConsoleConstans.MEDIUM_EDITOR_WIDTH_UNIT);
		editorLayout.setMargin(new MarginInfo(false, true));

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editorLayout);

		certDetails = new FormLayout();
		CollapsibleLayout detailsLayout = new CollapsibleLayout(
				msg.getMessage("CertificateEditor.certficateDetails"), certDetails);
		main.addComponent(detailsLayout);

		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
		binder.setBean(new CertificateForBinder(toEdit.name, toEdit.value));

	}

	private void reloadValueFromFile()
	{
		File file = uploader.getFile();
		if (file != null)
		{
			try
			{
				value.setValue(FileUtils.readFileToString(file));

			} catch (IOException e)
			{
				value.setComponentError(
						new UserError(msg.getMessage("CertificateEditor.invalidCertFormat")));
			}

		}
		uploader.unblock();
	}

	private void refreshDetails()
	{
		certDetails.removeAllComponents();
		X509Certificate cert;
		try
		{
			cert = getCertFromString(value.getValue());
		} catch (Exception e)
		{
			certDetails.addComponent(new Label(msg.getMessage("CertificateEditor.invalidCertFormat")));
			return;
		}

		Label subject = new Label();
		subject.setCaption(msg.getMessage("Certificate.subject"));
		subject.setValue(cert.getSubjectDN().toString());

		Label issuer = new Label();
		issuer.setCaption(msg.getMessage("Certificate.issuer"));
		issuer.setValue(cert.getIssuerDN().toString());

		Label validFrom = new Label();
		validFrom.setCaption(msg.getMessage("Certificate.validFrom"));
		validFrom.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(cert.getNotBefore()));

		Label validTo = new Label();
		validTo.setCaption(msg.getMessage("Certificate.validTo"));
		validTo.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(cert.getNotAfter()));

		Label alg = new Label();
		alg.setCaption(msg.getMessage("Certificate.signatureAlgorithm"));
		alg.setValue(cert.getSigAlgName());

		Label key = new Label();
		key.setCaption(msg.getMessage("Certificate.publicKey"));
		key.setValue(cert.getPublicKey().getAlgorithm());

		certDetails.addComponents(subject, issuer, validFrom, validTo, alg, key);
	}

	private boolean validateCert(String s)
	{
		try
		{
			getCertFromString(s);
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}

	private String getCertAsString(X509Certificate cert)
	{
		if (cert == null)
			return "";

		OutputStream out = new ByteArrayOutputStream();
		try
		{
			CertificateUtils.saveCertificate(out, cert, Encoding.PEM);
		} catch (IOException e)
		{
			throw new InternalException(msg.getMessage("CertificateEditor.invalidCertFormat"), e);
		}
		return out.toString();
	}

	private X509Certificate getCertFromString(String cert)
	{
		InputStream in = new ByteArrayInputStream(cert.getBytes());
		try
		{
			return CertificateUtils.loadCertificate(in, Encoding.PEM);

		} catch (IOException e)
		{
			throw new InternalException(msg.getMessage("CertificateEditor.invalidCertFormat"), e);
		}
	}

	void editMode()
	{
		name.setReadOnly(true);
	}

	boolean hasErrors()
	{
		return binder.validate().hasErrors();
	}

	NamedCertificate getCertificate()
	{
		uploader.clear();
		CertificateForBinder cert = binder.getBean();
		return new NamedCertificate(cert.getName(), cert.getValue());
	}

	public class CertificateForBinder
	{
		private String name;
		private X509Certificate value;

		public CertificateForBinder(String name, X509Certificate value)
		{
			this.name = name;
			this.value = value;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public X509Certificate getValue()
		{
			return value;
		}

		public void setValue(X509Certificate value)
		{
			this.value = value;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, value);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NamedCertificate other = (NamedCertificate) obj;

			return Objects.equals(name, other.name) && Objects.equals(value, other.value);
		}

	}
}
