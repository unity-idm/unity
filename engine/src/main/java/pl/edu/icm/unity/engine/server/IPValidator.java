/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.net.InetAddresses;


/**
 * Checks validity of IP address. Configurable with whitelist of address ranges or can just confirm syntax of address
 * if no whitelist is set.
 */
class IPValidator
{
	private List<CIDRAddressSpec> whitelist;
	
	IPValidator(List<String> whitelist)
	{
		this.whitelist = whitelist.stream()
				.map(cidr -> new CIDRAddressSpec(cidr))
				.collect(Collectors.toList());
	}

	void validateIPAddress(String ip)
	{
		InetAddress parsedIP = InetAddresses.forString(ip);
	
		if (whitelist.isEmpty())
			return;
		for (CIDRAddressSpec allowed: whitelist)
			if (allowed.matches(parsedIP))
				return;
		throw new AddressNotAllowedException(ip);
	}



	static class CIDRAddressSpec
	{
		private final int maskLength;
		private final byte[] netAddr;
		private byte[] maskBytes;
		
		CIDRAddressSpec(String cidrAddressSpec)
		{
			if (cidrAddressSpec.indexOf('/') > 0)
			{
				String[] addressAndMask = cidrAddressSpec.split("/");
				cidrAddressSpec = addressAndMask[0];
				maskLength = Integer.parseInt(addressAndMask[1]);
			} else
			{
				maskLength = 0;
			}
			InetAddress networkAddress = InetAddresses.forString(cidrAddressSpec);
			netAddr = networkAddress.getAddress();
			maskBytes = getMaskInBytes(maskLength);
		}
		
		private static byte[] getMaskInBytes(int maskLength)
		{
			int oddBits = maskLength % 8;
			int nMaskBytes = maskLength / 8 + (oddBits == 0 ? 0 : 1);
			byte[] maskBytes = new byte[nMaskBytes];

			Arrays.fill(maskBytes, 0, oddBits == 0 ? maskBytes.length : maskBytes.length - 1, (byte) 0xFF);

			if (oddBits != 0)
			{
				int finalByte = (1 << oddBits) - 1;
				finalByte <<= 8 - oddBits;
				maskBytes[maskBytes.length - 1] = (byte) finalByte;
			}
			return maskBytes;
		}

		public boolean matches(InetAddress ipAddress)
		{
			byte[] addressBytes = ipAddress.getAddress();
			if (netAddr.length != addressBytes.length)
				return false;

			if (maskLength == 0)
				return Arrays.equals(netAddr, addressBytes);

			for (int i = 0; i < maskBytes.length; i++)
			{
				if ((addressBytes[i] & maskBytes[i]) != (netAddr[i] & maskBytes[i]))
					return false;
			}

			return true;
		}
	}

	public static class AddressNotAllowedException extends IllegalArgumentException
	{
		public AddressNotAllowedException(String ip)
		{
			super("Adress " + ip + " is not allowed");
		}
	}
	
}
