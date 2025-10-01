package pl.edu.icm.unity.oauth.rp.local;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RESTLocalBearerTokenRetrievalTest
{
	private final TestableRESTLocalBearerTokenRetrieval retrieval = new TestableRESTLocalBearerTokenRetrieval();

	@Test
	void shouldExtractBearerTokenWhenAuthorizationHeaderContainsOnlyBearerSegment()
	{
		Message message = mock(Message.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		retrieval.setCurrentMessage(message);
		when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);
		when(request.getHeader("Authorization")).thenReturn("Bearer tokenValue");

		BearerAccessToken token = retrieval.publicGetTokenCredential();

		assertThat(token).isNotNull();
		assertThat(token.getValue()).isEqualTo("tokenValue");
	}

	@Test
	void shouldExtractBearerTokenWhenAuthorizationHeaderContainsMultipleSegments()
	{
		Message message = mock(Message.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		retrieval.setCurrentMessage(message);
		when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);
		when(request.getHeader("Authorization")).thenReturn("Basic abc,Bearer tokenValue");

		BearerAccessToken token = retrieval.publicGetTokenCredential();

		assertThat(token).isNotNull();
		assertThat(token.getValue()).isEqualTo("tokenValue");
	}

	@Test
	void shouldReturnNullWhenBearerSegmentMissing()
	{
		Message message = mock(Message.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		retrieval.setCurrentMessage(message);
		when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);
		when(request.getHeader("Authorization")).thenReturn("Basic abc");

		BearerAccessToken token = retrieval.publicGetTokenCredential();

		assertThat(token).isNull();
	}

	@Test
	void shouldReturnNullWhenBearerSegmentMalformed()
	{
		Message message = mock(Message.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		retrieval.setCurrentMessage(message);
		when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(request);
		when(request.getHeader("Authorization")).thenReturn("Bearer ");

		BearerAccessToken token = retrieval.publicGetTokenCredential();

		assertThat(token).isNull();
	}

	private static class TestableRESTLocalBearerTokenRetrieval extends RESTLocalBearerTokenRetrieval
	{
		private Message currentMessage;

		void setCurrentMessage(Message message)
		{
			this.currentMessage = message;
		}

		BearerAccessToken publicGetTokenCredential()
		{
			return super.getTokenCredential(null);
		}

		@Override
		Message getCurrentMessage()
		{
			return currentMessage;
		}
	}
}
