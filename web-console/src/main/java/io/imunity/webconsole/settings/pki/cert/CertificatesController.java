/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki.cert;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
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

	List<NamedCertificate> getCertificates() throws ControllerException
	
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
	
	
	NamedCertificate getCertificate(String name) throws ControllerException
	{
		try
		{
			return pkiMan.getCertificate(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CertificatesController.getError", name),
					e.getMessage(), e);
		}
	}

	void addCertificate(NamedCertificate certificate) throws ControllerException
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

	void updateCertificate(NamedCertificate certificate) throws ControllerException
	{
		try
		{
			pkiMan.updateCertificate(certificate);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CertificatesController.updateError", certificate.name),
					e.getMessage(), e);
		}
	}

	void removeCertificate(NamedCertificate certificate) throws ControllerException
	{
		try
		{
			pkiMan.removeCertificate(certificate.name);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CertificatesController.removeError", certificate.name),
					e.getMessage(), e);
		}
	}
}
