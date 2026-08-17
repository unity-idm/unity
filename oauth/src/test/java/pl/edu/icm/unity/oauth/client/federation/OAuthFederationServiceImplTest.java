/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@ExtendWith(MockitoExtension.class)
class OAuthFederationServiceImplTest
{
	private static final String TRUST_ANCHOR = "https://anchor.example.com";
	private static final OAuthFederationTrustConfig CONFIG = new OAuthFederationTrustConfig(
			new EntityID(TRUST_ANCHOR), new JWKSet(),
			Duration.ofSeconds(60), null, ServerHostnameCheckingMode.FAIL, null);

	@Mock
	ExecutorsService executorsService;
	@Mock
	ScheduledExecutorService scheduler;
	@SuppressWarnings("rawtypes")
	@Mock
	ScheduledFuture scheduledFuture;

	OAuthFederationServiceImpl service;

	@BeforeEach
	void setUp()
	{
		service = new OAuthFederationServiceImpl(executorsService);
	}

	@SuppressWarnings("unchecked")
	private void setupScheduler()
	{
		when(executorsService.getScheduledService()).thenReturn(scheduler);
		when(scheduler.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any(TimeUnit.class)))
				.thenReturn(scheduledFuture);
	}

	@Test
	void shouldCancelTaskAndRemoveHandlerWhenLastConsumerUnregistered()
	{
		setupScheduler();
		String consumerId = service.preregisterConsumer();
		service.registerConsumer(consumerId, Duration.ofSeconds(60), CONFIG, (chains, id) -> {});

		service.unregisterConsumer(consumerId);

		verify(scheduledFuture).cancel(false);
		// Re-registering the same trust anchor must create a fresh handler (old one removed from map)
		String newConsumerId = service.preregisterConsumer();
		service.registerConsumer(newConsumerId, Duration.ofSeconds(60), CONFIG, (chains, id) -> {});
		verify(scheduler, org.mockito.Mockito.times(2))
				.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any(TimeUnit.class));
	}

	@Test
	void shouldNotCancelTaskWhenOtherConsumersRemain()
	{
		setupScheduler();
		String consumer1 = service.preregisterConsumer();
		String consumer2 = service.preregisterConsumer();
		service.registerConsumer(consumer1, Duration.ofSeconds(60), CONFIG, (chains, id) -> {});
		service.registerConsumer(consumer2, Duration.ofSeconds(60), CONFIG, (chains, id) -> {});

		service.unregisterConsumer(consumer1);

		verify(scheduledFuture, never()).cancel(false);
	}

	@Test
	void shouldHandleUnregisterForUnknownConsumer()
	{
		service.unregisterConsumer("unknown-id");
		service.unregisterConsumer(null);
	}

	@Test
	void shouldUseSeparateHandlerForDifferentTruststore()
	{
		setupScheduler();
		OAuthFederationTrustConfig configA = new OAuthFederationTrustConfig(
				new EntityID(TRUST_ANCHOR), new JWKSet(),
				Duration.ofSeconds(60), null, ServerHostnameCheckingMode.FAIL, "truststoreA");
		OAuthFederationTrustConfig configB = new OAuthFederationTrustConfig(
				new EntityID(TRUST_ANCHOR), new JWKSet(),
				Duration.ofSeconds(60), null, ServerHostnameCheckingMode.FAIL, "truststoreB");

		String consumerA = service.preregisterConsumer();
		String consumerB = service.preregisterConsumer();
		service.registerConsumer(consumerA, Duration.ofSeconds(60), configA, (chains, id) -> {});
		service.registerConsumer(consumerB, Duration.ofSeconds(60), configB, (chains, id) -> {});

		verify(scheduler, org.mockito.Mockito.times(2))
				.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any(TimeUnit.class));
	}
}
