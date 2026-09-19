/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Guards server-side fetches of remote-source-declared URIs (e.g. an OAuth federation member's or a
 * SAML entity's self-declared {@code logo_uri}) against SSRF: resolves the URI's host and rejects it
 * if any resolved address is a loopback, link-local, private, multicast, or otherwise non-public
 * destination. This is a blocklist, not an allowlist - by design, since a public-destination allowlist
 * would make arbitrary third-party logos unusable. It does not protect against DNS rebinding (the
 * address re-resolving to an internal one between this check and the actual connection).
 */
public class SsrfProtection
{
	public static void assertNoInternalDestination(URI uri) throws IOException
	{
		String scheme = uri.getScheme();
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
			throw new IOException("Scheme " + scheme + " is not allowed for this destination");

		String host = uri.getHost();
		if (host == null)
			throw new IOException("URI has no host: " + uri);

		InetAddress[] addresses;
		try
		{
			addresses = InetAddress.getAllByName(host);
		} catch (UnknownHostException e)
		{
			throw new IOException("Can not resolve host " + host, e);
		}
		for (InetAddress address : addresses)
			if (isInternal(address))
				throw new BlockedDestinationException("Destination " + address.getHostAddress() + " (resolved from "
						+ host + ") is not a permitted public network address");
	}

	/**
	 * Thrown specifically when a destination was resolved and rejected as internal/non-public - as
	 * opposed to other {@link IOException}s this method throws (unsupported scheme, unresolvable host),
	 * which are not themselves indicative of an SSRF attempt. Callers can catch this type to give SSRF
	 * blocks distinct, more visible logging than ordinary network failures.
	 */
	public static class BlockedDestinationException extends IOException
	{
		public BlockedDestinationException(String message)
		{
			super(message);
		}
	}

	private static boolean isInternal(InetAddress address)
	{
		byte[] bytes = address.getAddress();
		if (bytes.length == 16 && isIPv4Mapped(bytes))
		{
			try
			{
				return isInternal(InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16)));
			} catch (UnknownHostException e)
			{
				return true;
			}
		}

		if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
				|| address.isMulticastAddress() || address.isAnyLocalAddress())
			return true;

		if (bytes.length == 4)
			return isBlockedIPv4(bytes);
		if (bytes.length == 16)
			return isBlockedIPv6(bytes);
		return true;
	}

	private static boolean isIPv4Mapped(byte[] b)
	{
		for (int i = 0; i < 10; i++)
			if (b[i] != 0)
				return false;
		return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
	}

	private static boolean isBlockedIPv4(byte[] b)
	{
		int b0 = b[0] & 0xFF;
		int b1 = b[1] & 0xFF;
		int b2 = b[2] & 0xFF;
		if (b0 == 0)
			return true; // 0.0.0.0/8 - "this network"
		if (b0 == 100 && (b1 & 0xC0) == 64)
			return true; // 100.64.0.0/10 - CGNAT
		if (b0 == 192 && b1 == 0 && b2 == 0)
			return true; // 192.0.0.0/24 - IETF protocol assignments
		if (b0 == 192 && b1 == 0 && b2 == 2)
			return true; // 192.0.2.0/24 - TEST-NET-1
		if (b0 == 192 && b1 == 88 && b2 == 99)
			return true; // 192.88.99.0/24 - 6to4 relay anycast
		if (b0 == 198 && (b1 == 18 || b1 == 19))
			return true; // 198.18.0.0/15 - benchmarking
		if (b0 == 198 && b1 == 51 && b2 == 100)
			return true; // 198.51.100.0/24 - TEST-NET-2
		if (b0 == 203 && b1 == 0 && b2 == 113)
			return true; // 203.0.113.0/24 - TEST-NET-3
		if (b0 >= 240)
			return true; // 240.0.0.0/4 reserved, incl. 255.255.255.255 broadcast
		return false;
	}

	private static boolean isBlockedIPv6(byte[] b)
	{
		if ((b[0] & 0xFE) == 0xFC)
			return true; // fc00::/7 - unique local address
		if ((b[0] & 0xFF) == 0x20 && (b[1] & 0xFF) == 0x01 && (b[2] & 0xFF) == 0x0D && (b[3] & 0xFF) == 0xB8)
			return true; // 2001:db8::/32 - documentation
		return false;
	}

	private SsrfProtection()
	{
	}
}
