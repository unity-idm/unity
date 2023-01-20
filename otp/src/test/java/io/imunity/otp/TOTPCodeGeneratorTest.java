/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import io.imunity.otp.v8.TOTPCodeGenerator;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class TOTPCodeGeneratorTest
{
	private static final TestCaseSpec[] SHA1_CASES = 
	{
		new TestCaseSpec(59, "94287082"),
		new TestCaseSpec(1111111109, "07081804"),
		new TestCaseSpec(1111111111, "14050471"),
		new TestCaseSpec(1234567890, "89005924"),
		new TestCaseSpec(2000000000, "69279037"),
		new TestCaseSpec(20000000000l, "65353130")
	}; 

	private static final TestCaseSpec[] SHA256_CASES = 
	{
		new TestCaseSpec(59, 		"46119246"),
		new TestCaseSpec(1111111109, 	"68084774"),
		new TestCaseSpec(1111111111, 	"67062674"),
		new TestCaseSpec(1234567890, 	"91819424"),
		new TestCaseSpec(2000000000, 	"90698825"),
		new TestCaseSpec(20000000000l, 	"77737706")
	}; 

	private static final TestCaseSpec[] SHA512_CASES = 
	{
		new TestCaseSpec(59, 		"90693936"),
		new TestCaseSpec(1111111109, 	"25091201"),
		new TestCaseSpec(1111111111, 	"99943326"),
		new TestCaseSpec(1234567890, 	"93441116"),
		new TestCaseSpec(2000000000, 	"38618901"),
		new TestCaseSpec(20000000000l, 	"47863826")
	}; 

	@ParameterizedTest
	@MethodSource("getSHA1TestParams")
	public void shouldProduceRFCValue(TestCaseSpec caseSpec)
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                String code = TOTPCodeGenerator.generateTOTP(seed, caseSpec.time, new OTPGenerationParams(8, HashFunction.SHA1, 30));
                
                assertThat(code).isEqualTo(caseSpec.code);
	}
	
	@ParameterizedTest
	@MethodSource("getSHA256TestParams")
	public void shouldProduceRFCValueSHA256(TestCaseSpec caseSpec)
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930313233343536373839303132");
		
                String code = TOTPCodeGenerator.generateTOTP(seed, caseSpec.time, new OTPGenerationParams(8, HashFunction.SHA256, 30));
                
                assertThat(code).isEqualTo(caseSpec.code);
	}

	@ParameterizedTest
	@MethodSource("getSHA512TestParams")
	public void shouldProduceRFCValueSHA512(TestCaseSpec caseSpec)
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930" +
			         "3132333435363738393031323334353637383930" +
			         "3132333435363738393031323334353637383930" +
			         "31323334");
		
                String code = TOTPCodeGenerator.generateTOTP(seed, caseSpec.time, new OTPGenerationParams(8, HashFunction.SHA512, 30));
                
                assertThat(code).isEqualTo(caseSpec.code);
	}
	
	@ParameterizedTest
	@EnumSource(HashFunction.class)
	public void shouldGenerateCodeForRandomKey(HashFunction hash)
	{
		String seed = TOTPKeyGenerator.generateRandomBase32EncodedKey(hash);
		
                String code = TOTPCodeGenerator.generateTOTP(seed, 1l, new OTPGenerationParams(8, hash, 30));
                
                assertThat(code).isNotNull().hasSize(8);
	}
	
	@Test
	public void shouldGenerateURI()
	{
		byte[] key = { 'H', 'e', 'l', 'l', 'o', '!', (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
		
		String url = TOTPKeyGenerator.generateTOTPURI(new Base32().encodeToString(key), 
				"john.doe@email.com", "ACME&Sons", 
				new OTPGenerationParams(6, HashFunction.SHA1, 30), Optional.of("https://host.com/image.jpg"));
                
                assertThat(url).isEqualTo("otpauth://totp/ACME%26Sons:john.doe%40email.com?"
                		+ "secret=JBSWY3DPEHPK3PXP&"
                		+ "issuer=ACME%26Sons&"
                		+ "algorithm=SHA1&"
                		+ "digits=6&period=30&"
                		+ "image=https%3A%2F%2Fhost.com%2Fimage.jpg");
	}
	
	private static Stream<TestCaseSpec> getSHA1TestParams()
	{
		return Arrays.stream(SHA1_CASES);
	}
	
	private static Stream<TestCaseSpec> getSHA256TestParams()
	{
		return Arrays.stream(SHA256_CASES);
	}
	
	private static Stream<TestCaseSpec> getSHA512TestParams()
	{
		return Arrays.stream(SHA512_CASES);
	}
	
	static String hexToBase32(String hexValue)
	{
		try
		{
			return new Base32().encodeToString(Hex.decodeHex(hexValue));
		} catch (DecoderException e)
		{
			throw new IllegalStateException("Can not decode hex value", e);
		}
	}
	
	static class TestCaseSpec
	{
		final long time;
		final String code;
		
		TestCaseSpec(long time, String code)
		{
			this.time = time;
			this.code = code;
		}
	}
}
