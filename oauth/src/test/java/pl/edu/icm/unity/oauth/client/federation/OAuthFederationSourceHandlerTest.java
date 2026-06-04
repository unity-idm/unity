/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@ExtendWith(MockitoExtension.class)
class OAuthFederationSourceHandlerTest
{
	private static final Duration SHORT_INTERVAL = Duration.ofSeconds(60);
	private static final OAuthFederationConfig CONFIG = new OAuthFederationConfig(
			new EntityID("https://anchor.example.com"), URI.create("https://anchor.example.com/list"), new JWKSet(),
			SHORT_INTERVAL, null, ServerHostnameCheckingMode.FAIL);

	@Mock
	ExecutorsService executorsService;
	@Mock
	ScheduledExecutorService scheduler;
	@Mock
	OAuthFederationLoader loader;

	OAuthFederationSourceHandler handler;
	Runnable capturedRefreshTask;

	@BeforeEach
	void setUp()
	{
		when(executorsService.getScheduledService()).thenReturn(scheduler);
		handler = new OAuthFederationSourceHandler(executorsService, loader);
	}

	private void setupScheduler()
	{
		when(scheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
				.thenAnswer(inv ->
				{
					capturedRefreshTask = inv.getArgument(0);
					return mock(ScheduledFuture.class);
				});
	}

	@Test
	void shouldStartSchedulerOnFirstConsumer()
	{
		setupScheduler();

		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (chains, id) ->
		{
		});

		verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(5L), eq(5L), eq(TimeUnit.SECONDS));
	}

	@Test
	void shouldNotStartSchedulerTwice()
	{
		setupScheduler();

		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (chains, id) ->
		{
		});
		handler.addConsumer("c2", SHORT_INTERVAL, CONFIG, (chains, id) ->
		{
		});

		verify(scheduler, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
	}

	@Test
	void shouldReturnTrueWhenLastConsumerRemoved()
	{
		setupScheduler();
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (chains, id) ->
		{
		});

		boolean empty = handler.removeConsumer("c1");

		assertThat(empty).isTrue();
	}

	@Test
	void shouldReturnFalseWhenOtherConsumersRemain()
	{
		setupScheduler();
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (chains, id) ->
		{
		});
		handler.addConsumer("c2", SHORT_INTERVAL, CONFIG, (chains, id) ->
		{
		});

		boolean empty = handler.removeConsumer("c1");

		assertThat(empty).isFalse();
	}

	@Test
	void shouldDeliverTrustChainsToConsumerOnRefresh() throws Exception
	{
		setupScheduler();
		TrustChain chain = buildChain();
		when(loader.loadAll(CONFIG)).thenReturn(List.of(chain));
		List<String> receivedIds = new ArrayList<>();
		List<List<TrustChain>> receivedChains = new ArrayList<>();
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (c, id) ->
		{
			receivedChains.add(c);
			receivedIds.add(id);
		});

		capturedRefreshTask.run();

		assertThat(receivedIds).containsExactly("c1");
		assertThat(receivedChains.get(0)).containsExactly(chain);
	}

	@Test
	void shouldDeliverToAllConsumersOnRefresh()
	{
		setupScheduler();
		when(loader.loadAll(CONFIG)).thenReturn(List.of());
		List<String> receivedIds = new ArrayList<>();
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (c, id) -> receivedIds.add(id));
		handler.addConsumer("c2", SHORT_INTERVAL, CONFIG, (c, id) -> receivedIds.add(id));

		capturedRefreshTask.run();

		assertThat(receivedIds).containsExactlyInAnyOrder("c1", "c2");
	}

	@Test
	void shouldSkipRefreshWhenIntervalNotElapsedAndNoCacheExpiry()
	{
		setupScheduler();
		when(loader.hasExpiredEntries()).thenReturn(false);
		when(loader.loadAll(CONFIG)).thenReturn(List.of());
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (c, id) ->
		{
		});

		capturedRefreshTask.run(); // first run — lastRefresh = EPOCH, interval always elapsed
		capturedRefreshTask.run(); // second run — interval not elapsed, no expired entries

		verify(loader, times(1)).loadAll(any());
	}

	@Test
	void shouldRefreshWhenCacheHasExpiredEntries()
	{
		setupScheduler();
		when(loader.hasExpiredEntries()).thenReturn(false)
				.thenReturn(true);
		when(loader.loadAll(CONFIG)).thenReturn(List.of());
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (c, id) ->
		{
		});

		capturedRefreshTask.run(); // first run
		capturedRefreshTask.run(); // second run — expired entries detected

		verify(loader, times(2)).loadAll(any());
	}

	@Test
	void shouldContinueWithOtherConsumersWhenOneThrows()
	{
		setupScheduler();
		when(loader.loadAll(CONFIG)).thenReturn(List.of());
		List<String> receivedIds = new ArrayList<>();
		handler.addConsumer("c1", SHORT_INTERVAL, CONFIG, (c, id) ->
		{
			throw new RuntimeException("consumer failure");
		});
		handler.addConsumer("c2", SHORT_INTERVAL, CONFIG, (c, id) -> receivedIds.add(id));

		capturedRefreshTask.run();

		assertThat(receivedIds).containsExactly("c2");
	}

	@Test
	void shouldGenerateUniqueConsumerIds()
	{
		String id1 = OAuthFederationSourceHandler.generateConsumerId();
		String id2 = OAuthFederationSourceHandler.generateConsumerId();

		assertThat(id1).isNotEqualTo(id2);
		assertThat(id1).isNotBlank();
	}

	// --- helper ---

	private TrustChain buildChain() throws Exception
	{
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("test")
				.generate();
		JWKSet jwks = new JWKSet(key.toPublicJWK());
		EntityID leafId = new EntityID("https://idp.example.com");
		EntityID anchorId = new EntityID("https://anchor.example.com");
		Date now = new Date();
		Date exp = Date.from(Instant.now()
				.plusSeconds(3600));

		EntityStatementClaimsSet leafClaims = new EntityStatementClaimsSet(leafId, leafId, now, exp, jwks);
		EntityStatement leafStatement = EntityStatement.sign(leafClaims, key);

		EntityStatementClaimsSet anchorClaims = new EntityStatementClaimsSet(anchorId, leafId, now, exp, jwks);
		EntityStatement anchorStatement = EntityStatement.sign(anchorClaims, key);

		return new TrustChain(leafStatement, List.of(anchorStatement));
	}
}
