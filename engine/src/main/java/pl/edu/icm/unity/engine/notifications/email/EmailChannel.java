/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.email;

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

import com.sun.mail.util.MailSSLSocketFactory;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.notifications.MessageTemplateParams;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;

class EmailChannel implements NotificationChannelInstance
{
	private final Session session;
	private final ExecutorsService executorsService;
	
	
	EmailChannel(String configuration, ExecutorsService executorsService, PKIManagement pkiManagement)
	{
		this.executorsService = executorsService;
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
		String smtpUser = props.getProperty(EmailFacility.CFG_USER);
		String smtpPassword = props.getProperty(EmailFacility.CFG_PASSWD);
		Authenticator smtpAuthn = (smtpUser != null && smtpPassword != null) ? 
				new SimpleAuthenticator(smtpUser, smtpPassword) : null;
		String trustAll = props.getProperty(EmailFacility.CFG_TRUST_ALL);
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
			SSLSocketFactory factory = new SocketFactoryCreator2(validator, 
					new HostnameMismatchCallbackImpl(ServerHostnameCheckingMode.FAIL)).getSocketFactory();
			props.put("mail.smtp.ssl.socketFactory", factory);
		}
		session = Session.getInstance(props, smtpAuthn);
	}
	
	@Override
	public Future<NotificationStatus> sendNotification(final String recipientAddress, 
			final MessageTemplate.Message message)
	{
		NotificationStatus retStatus = new NotificationStatus();
		return executorsService.getExecutionService().submit(() -> 
		{
			try
			{
				sendEmail(message, recipientAddress);
			} catch (Exception e)
			{
				EmailFacility.log.error("E-mail notification failed", e);
				retStatus.setProblem(e);
			}
		}, retStatus);
	}
	
	private void sendEmail(MessageTemplate.Message message, String to) throws MessagingException
	{
		EmailFacility.log.info("Sending e-mail message to '" + to +"' with subject: " + message.getSubject());
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom();
		msg.setRecipients(Message.RecipientType.TO, to);
		msg.setSubject(message.getSubject());
		msg.setSentDate(new Date());
		msg.setContent(message.getBody(), getContentType(message));
		Transport.send(msg);
	}

	private String getContentType(MessageTemplate.Message message)
	{
		switch (message.getType())
		{
		case HTML:
			return "text/html; charset=utf-8";
		case PLAIN:
			return "text/plain; charset=utf-8";
		}
		throw new IllegalStateException("BUG: missing conversion of type " 
				+ message.getType() + " to mail content type");
	}

	@Override
	public String getFacilityId()
	{
		return EmailFacility.NAME;
	}
	
	@Override
	public boolean providesMessageTemplatingFunctionality()
	{
		return false;
	}
	
	static class SimpleAuthenticator extends Authenticator
	{
		private String user;
		private String password;
		
		public SimpleAuthenticator(String user, String password)
		{
			this.user = user;
			this.password = password;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() 
		{
			return new PasswordAuthentication(user, password);
		}
	}

	@Override
	public Future<NotificationStatus> sendExternalTemplateMessage(String recipientAddress,
			MessageTemplateParams templateParams)
	{
		throw new UnsupportedOperationException();
	}
}