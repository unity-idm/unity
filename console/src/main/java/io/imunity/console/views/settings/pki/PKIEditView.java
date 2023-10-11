/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.pki;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Optional;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.BreadCrumbParameter.BREAD_CRUMB_SEPARATOR;

@PermitAll
@Route(value = "/pki/edit", layout = ConsoleMenu.class)
public class PKIEditView extends ConsoleViewComponent
{
	public static final int MAX_FILE_SIZE = 50000000;
	private final CertificatesController controller;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;

	private boolean edit;
	private TextArea value;
	private Binder<CertificateEntry> binder;
	private FormLayout certDetails;
	private BreadCrumbParameter breadCrumbParameter;


	PKIEditView(MessageSource msg, CertificatesController controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String certName)
	{
		getContent().removeAll();

		CertificateEntry certificateEntry;
		if(certName == null)
		{
			certificateEntry = new CertificateEntry();
			breadCrumbParameter = new BreadCrumbParameter(
					null, msg.getMessage("TrustedCertificates.navCaption") + BREAD_CRUMB_SEPARATOR + msg.getMessage("new")
			);
			edit = false;
		}
		else
		{
			certificateEntry = controller.getCertificate(certName);
			breadCrumbParameter = new BreadCrumbParameter(
					certName, msg.getMessage("TrustedCertificates.navCaption") + BREAD_CRUMB_SEPARATOR + certName
			);
			edit = true;
		}
		initUI(certificateEntry);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(CertificateEntry certificateEntry)
	{
		TextField name = new TextField();
		name.setWidthFull();
		name.setReadOnly(edit);
		name.setWidth("var(--vaadin-text-field-big)");
		name.setPlaceholder(msg.getMessage("Certificate.defaultName"));

		MemoryBuffer memoryBuffer = new MemoryBuffer();
		Upload upload = new Upload(memoryBuffer);
		upload.setMaxFileSize(MAX_FILE_SIZE);
		upload.getStyle().set("margin", "var(--base-margin) 0");
		upload.addSucceededListener(e ->
		{
			try
			{
				value.setValue(new String(memoryBuffer.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
			} catch (IOException ex)
			{
				notificationPresenter.showError(msg.getMessage("error"), ex.getMessage());
			}
		});
		upload.setUploadButton(new Button(msg.getMessage("CertificateEditor.uploadButtonCaption")));

		value = new TextArea();
		value.setWidthFull();
		value.setClassName("monospace");
		value.setHeight("var(--vaadin-text-field-medium)");
		value.addValueChangeListener(e -> refreshDetails());

		binder = new Binder<>(CertificateEntry.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(CertificateEntry::getName, CertificateEntry::setName);
		binder.forField(value)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator(this::validateCert, msg.getMessage("CertificateEditor.invalidCertFormat"))
				.withConverter(this::getCertFromString, this::getCertAsString)
				.bind(CertificateEntry::getValue, CertificateEntry::setValue);

		FormLayout editorLayout = new FormLayout();
		editorLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		editorLayout.addFormItem(name, msg.getMessage("Certificate.name"));
		editorLayout.addFormItem(upload, "");
		editorLayout.addFormItem(value, msg.getMessage("Certificate.value"));

		certDetails = new FormLayout();
		certDetails.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		Details details = new Details(new Span(msg.getMessage("CertificateEditor.certficateDetails")), certDetails);
		details.setOpened(true);
		editorLayout.add(details);
		binder.setBean(certificateEntry);

		getContent().add(new VerticalLayout(editorLayout, createActionLayout(msg, edit, PKIView.class, this::onConfirm)));
	}

	private void onConfirm()
	{
		binder.validate();
		if(binder.isValid())
		{
			CertificateEntry bean = binder.getBean();
			if(edit)
				controller.updateCertificate(bean);
			else
				controller.addCertificate(bean);
			UI.getCurrent().navigate(PKIView.class);
		}
	}

	private void refreshDetails()
	{
		certDetails.removeAll();
		X509Certificate cert;
		try
		{
			cert = getCertFromString(value.getValue());
		} catch (Exception e)
		{
			certDetails.add(new Span(msg.getMessage("CertificateEditor.invalidCertFormat")));
			return;
		}

		certDetails.addFormItem(getMonospaceLabel(cert.getSubjectX500Principal().toString()), msg.getMessage("Certificate.subject"));
		certDetails.addFormItem(getMonospaceLabel(cert.getIssuerX500Principal().toString()), msg.getMessage("Certificate.issuer"));
		certDetails.addFormItem(getMonospaceLabel(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(cert.getNotBefore())), msg.getMessage("Certificate.validFrom"));
		certDetails.addFormItem(getMonospaceLabel(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(cert.getNotAfter())), msg.getMessage("Certificate.validTo"));
		certDetails.addFormItem(getMonospaceLabel(cert.getSigAlgName()), msg.getMessage("Certificate.signatureAlgorithm"));
		certDetails.addFormItem(getMonospaceLabel(cert.getPublicKey().getAlgorithm()), msg.getMessage("Certificate.publicKey"));
	}

	private static Span getMonospaceLabel(String value)
	{
		Span label = new Span(value);
		label.setClassName("monospace");
		return label;
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
			CertificateUtils.saveCertificate(out, cert, CertificateUtils.Encoding.PEM);
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
			return CertificateUtils.loadCertificate(in, CertificateUtils.Encoding.PEM);

		} catch (IOException e)
		{
			throw new InternalException(msg.getMessage("CertificateEditor.invalidCertFormat"), e);
		}
	}
}
