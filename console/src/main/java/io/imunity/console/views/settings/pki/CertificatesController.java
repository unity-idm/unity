/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.pki;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;

import java.util.List;

@Component
class CertificatesController
{
	private final PKIManagement pkiMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	CertificatesController(PKIManagement pkiMan, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.pkiMan = pkiMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	List<CertificateEntry> getCertificates()
	
	{
		try
		{
			return pkiMan.getPersistedCertificates().stream()
					.map(CertificateEntry::new)
					.toList();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CertificatesController.getAllError"), e.getMessage());
		}
		return List.of();
	}


	CertificateEntry getCertificate(String name)
	{
		try
		{
			return new CertificateEntry(pkiMan.getCertificate(name));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CertificatesController.getError"), e.getMessage());
		}
		return null;
	}

	void addCertificate(CertificateEntry certificate)
	{
		try
		{
			pkiMan.addPersistedCertificate(new NamedCertificate(certificate.getName(), certificate.getValue()));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CertificatesController.addError", certificate.getName()), e.getMessage());
		}
	}

	void updateCertificate(CertificateEntry certificate)
	{
		try
		{
			pkiMan.updateCertificate(new NamedCertificate(certificate.getName(), certificate.getValue()));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CertificatesController.updateError", certificate.getName()), e.getMessage());
		}
	}

	void removeCertificate(CertificateEntry certificate)
	{
		try
		{
			pkiMan.removeCertificate(certificate.getName());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CertificatesController.removeError", certificate.getName()), e.getMessage());
		}
	}
}
