package com.sparkcleancebu.biometrics;

import java.security.SecureRandom;

public class RandomString {
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private static String randomize(int len) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < len; i++) {
			if (i > 0 && i % 4 == 0) {
				sb.append('-'); // Add hyphen every 4 characters
			}
			sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
		}

		return sb.toString();
	}

	public static String generate() {
		return randomize(32);
	}

	public static String generate(int len) {
		return randomize(len);
	}
}
