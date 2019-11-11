/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.perfromance;

public class PerformanceTestConfig
{
	public final String unityBaseURL;
	public final String restUserName;
	public final String restUserPasswd;

	public PerformanceTestConfig(String unityURL, String restUserName, String restUserPasswd)
	{
		this.unityBaseURL = unityURL;
		this.restUserName = restUserName;
		this.restUserPasswd = restUserPasswd;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String unityURL;
		private String restUserName;
		private String restUserPasswd;

		private Builder()
		{
		}

		public Builder withUnityURL(String unityURL)
		{
			this.unityURL = unityURL;
			return this;
		}

		public Builder withRestUserName(String restUserName)
		{
			this.restUserName = restUserName;
			return this;
		}

		public Builder withRestUserPasswd(String restUserPasswd)
		{
			this.restUserPasswd = restUserPasswd;
			return this;
		}

		public PerformanceTestConfig build()
		{
			return new PerformanceTestConfig(unityURL, restUserName, restUserPasswd);
		}
	}
}
