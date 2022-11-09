/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.sms;

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.HttpResponseHandler;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.notifications.MessageTemplateParams;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;
import pl.edu.icm.unity.types.basic.MessageType;


/**
 * Sends SMSes via Clickatell gateway. 
 * 
 * See https://www.clickatell.com/developers/api-documentation/rest-api-send-message/
 * 
 * @author K. Benedyczak
 */
public class ClickatellChannel implements NotificationChannelInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_NOTIFY, ClickatellChannel.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private SMSServiceProperties config;
	private ExecutorsService execService;
	
	public ClickatellChannel(SMSServiceProperties config, ExecutorsService execService)
	{
		this.config = config;
		this.execService = execService;
	}

	@Override
	public String getFacilityId()
	{
		return SMSFacility.NAME;
	}

	@Override
	public boolean providesMessageTemplatingFunctionality()
	{
		return false;
	}
	
	@Override
	public Future<NotificationStatus> sendNotification(String recipientAddress, Message message)
	{
		NotificationStatus retStatus = new NotificationStatus();
		return execService.getExecutionService().submit(() ->
		{
			try
			{
				sendSMS(recipientAddress, message);
			} catch (Exception e)
			{
				log.error("SMS notification failed", e);
				retStatus.setProblem(e);
			}
		}, retStatus);
	}
	
	private void sendSMS(String recipientAddress, Message message) throws IOException, ParseException
	{
		if (message.getType() != MessageType.PLAIN)
			throw new ConfigurationException("Refusing to send non-PLAN message over SMS channel");
		ObjectNode request = createRequest(recipientAddress, message);
		String requestEntity = JsonUtil.serialize(request);
		log.info("Sending SMS to {} over Clickatell", recipientAddress);
		sendMessage(requestEntity);
	}
	
	private void sendMessage(String body) throws IOException, ParseException
	{
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://platform.clickatell.com/messages");

		httpPost.setEntity(new StringEntity(body));
		httpPost.addHeader("Authorization", config.getValue(SMSServiceProperties.CLICKATELL_API_KEY));
		httpPost.setHeader("Content-type", ContentType.APPLICATION_JSON.getMimeType());
		httpPost.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

		log.debug("Will send SMS over Clickatell service, request:\n {}", body);
		ClassicHttpResponse response = client.execute(httpPost, new HttpResponseHandler());
		if (response.getCode() >= 300)
		{
			throw new IOException("Communication with Clickatell service failed, error: " + 
					new StatusLine(response).toString() + ", received contents: " +
					EntityUtils.toString(response.getEntity()));
		}
		log.info("SMS to {} sent successfully");
		client.close();
	}
	
	private ObjectNode createRequest(String recipientAddress, Message message)
	{
		ObjectNode request = MAPPER.createObjectNode();
		String subject = message.getSubject();
		subject = subject.isEmpty() ? "" : subject + ": ";
		request.put("content", subject + message.getBody());
		ArrayNode to = request.withArray("to");
		to.add(recipientAddress);
		request.put("charset", config.getEnumValue(
				SMSServiceProperties.CLICKATELL_CHARSET, 
				SMSServiceProperties.Charset.class).toString());
		return request;
	}
	

	@Override
	public Future<NotificationStatus> sendExternalTemplateMessage(String recipientAddress,
			MessageTemplateParams templateParams)
	{
		throw new UnsupportedOperationException();
	}
}
