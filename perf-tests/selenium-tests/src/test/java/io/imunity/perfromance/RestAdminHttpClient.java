/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.perfromance;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestAdminHttpClient
{
	private final String baseUrl;
	public static final MediaType TEXT_PLAIN = MediaType.get("text/plain");

	public RestAdminHttpClient(String baseUrl)
	{
		this.baseUrl = baseUrl;
	}

	public void invalidateSession(String userName)
	{
		OkHttpClient client = new OkHttpClient.Builder()
				.authenticator(getDefaultAuthn())
				.build();

		RequestBody body = RequestBody.create(TEXT_PLAIN, userName);
		String url = baseUrl + "/restadm/v1/triggerEvent/invalidate_session";
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();

		try (Response response = client.newCall(request).execute())
		{
			response.isSuccessful();
		} catch (IOException e)
		{
			throw new IllegalStateException("Failed to invalidate session for " + userName, e);
		}
	}

	private Authenticator getDefaultAuthn()
	{
		return (route, response) -> {
			String credential = Credentials.basic("a", "a");
			return response.request()
					.newBuilder()
					.header("Authorization", credential)
					.build();
		};
	}
}
