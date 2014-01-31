/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.mail.util.MailSSLSocketFactory;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.notifications.NotificationFacility;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;

/**
 * Email notification facility.
 * @author K. Benedyczak
 */
@Component
public class EmailFacility implements NotificationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EmailFacility.class);
	public static final String NAME = "email";
	
	private static final String CFG_USER = "mailx.smtp.auth.username"; 
	private static final String CFG_PASSWD = "mailx.smtp.auth.password";
	private static final String CFG_TRUST_ALL = "mailx.smtp.trustAll";
	
	private ExecutorsService executorsService;
	private PKIManagement pkiManagement;
	
	@Autowired
	public EmailFacility(ExecutorsService executorsService, PKIManagement pkiManagement)
	{
		this.executorsService = executorsService;
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Sends notifications by e-mail";
	}

	@Override
	public void validateConfiguration(String configuration) throws WrongArgumentException
	{
		// TODO create properties helper and validate more throughly?
		Properties props = new Properties();
		try
		{
			props.load(new StringReader(configuration));
		} catch (IOException e)
		{
			throw new WrongArgumentException("Email configuration is invalid: " +
					"not a valid properties syntax was used", e);
		}
	}

	@Override
	public NotificationChannelInstance getChannel(String configuration)
	{
		return new EmailChannel(configuration);
	}

	@Override
	public String getRecipientAddressMetadataKey()
	{
		return ContactEmailMetadataProvider.NAME;
	}
	
	private class EmailChannel implements NotificationChannelInstance
	{
		private Session session;
		
		public EmailChannel(String configuration)
		{
			Properties props = new Properties();
			try
			{
				props.load(new StringReader(configuration));
			} catch (IOException e)
			{
				//really shouldn't happen
				throw new IllegalStateException("Bug: can't load email properties " +
						"for the channel instance", e);
			}
			String smtpUser = props.getProperty(CFG_USER);
			String smtpPassword = props.getProperty(CFG_PASSWD);
			Authenticator smtpAuthn = (smtpUser != null && smtpPassword != null) ? 
					new SimpleAuthenticator(smtpUser, smtpPassword) : null;
			String trustAll = props.getProperty(CFG_TRUST_ALL);
			if (trustAll != null && "true".equalsIgnoreCase(trustAll))
			{
				MailSSLSocketFactory trustAllSF;
				try
				{
					trustAllSF = new MailSSLSocketFactory();
				} catch (GeneralSecurityException e)
				{
					//really shouldn't happen
					throw new IllegalStateException("Can't init trust-all SSL socket factory", e);
				}
				trustAllSF.setTrustAllHosts(true);
				props.put("mail.smtp.ssl.socketFactory", trustAllSF);
			} else
			{
				X509CertChainValidator validator = pkiManagement.getMainAuthnAndTrust().getValidator();
				SSLSocketFactory factory = SocketFactoryCreator.getSocketFactory(null, validator);
				props.put("mail.smtp.ssl.socketFactory", factory);
			}
			session = Session.getInstance(props, smtpAuthn);
		}
		
		@Override
		public Future<NotificationStatus> sendNotification(final String recipientAddress, 
				final String msgSubject, final String message)
		{
			final NotificationStatus retStatus = new NotificationStatus();
			return executorsService.getService().submit(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						sendEmail(msgSubject, message, recipientAddress);
					} catch (Exception e)
					{
						log.error("E-mail notification failed", e);
						retStatus.setProblem(e);
					}
				}
			}, retStatus);
		}
		
		private void sendEmail(String subject, String body, String to) throws MessagingException
		{
			log.debug("Sending e-mail message to '" + to +"' with subject: " + subject);
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom();
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(subject);
			msg.setSentDate(new Date());
			msg.setText(body);
			Transport.send(msg);
		}

		@Override
		public String getFacilityId()
		{
			return NAME;
		}
	}
	
	private static class SimpleAuthenticator extends Authenticator
	{
		private String user;
		private String password;
		
		public SimpleAuthenticator(String user, String password)
		{
			this.user = user;
			this.password = password;
		}

		protected PasswordAuthentication getPasswordAuthentication() 
		{
			return new PasswordAuthentication(user, password);
		}
	}
}
