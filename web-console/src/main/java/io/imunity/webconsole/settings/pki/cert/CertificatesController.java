/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki.cert;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.Certificate;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all trusted certificates views
 * 
 * @author P.Piernik
 *
 */
@Component
public class CertificatesController
{
	private PKIManagement pkiMan;
	private UnityMessageSource msg;

	CertificatesController(PKIManagement pkiMan, UnityMessageSource msg)
	{
		this.pkiMan = pkiMan;
		this.msg = msg;
	}

	List<Certificate> getCertificates() throws ControllerException
	
	{
		try
		{
			return pkiMan.getPersistedCertificates();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CertificatesController.getAllError"),
					e.getMessage(), e);
		}
	}
	
	
	Certificate getCertificate(String name) throws ControllerException
	{
		try
		{
			return pkiMan.getPersistedCertificate(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CertificatesController.getError", name),
					e.getMessage(), e);
		}
	}

	void addCertificate(Certificate certificate) throws ControllerException
	{
		try
		{
			pkiMan.addPersistedCertificate(certificate);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CertificatesController.addError", certificate.name),
					e.getMessage(), e);
		}
	}

	void updateCertificate(Certificate certificate) throws ControllerException
	{
		try
		{
			pkiMan.updatePersistedCertificate(certificate);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CertificatesController.updateError", certificate.name),
					e.getMessage(), e);
		}
	}

	void removeCertificate(Certificate certificate) throws ControllerException
	{
		try
		{
			pkiMan.removePersistedCertificate(certificate.name);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CertificatesController.removeError", certificate.name),
					e.getMessage(), e);
		}
	}
}
