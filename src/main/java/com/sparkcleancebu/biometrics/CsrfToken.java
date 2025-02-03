package com.sparkcleancebu.biometrics;

import java.net.http.HttpResponse;
import java.util.Optional;

import com.sparkcleancebu.http_helper.HttpClientHelper;

public class CsrfToken {
	private static String token = null;

	public static String getToken() {
		return token;
	}

	public static void resetToken() {
		token = null;
	}

	public static String acquire() {
		try {
			if (token == null || token.isEmpty()) {
				HttpResponse<String> response = HttpClientHelper.get("sanctum/csrf-cookie");

				if (response.statusCode() == 204) {
					Optional<String> xsrfToken = response.headers().firstValue("Set-Cookie");

					if (xsrfToken.isPresent()) {
						// Extract CSRF token value
						token = xsrfToken.get().split(";")[0].split("=")[1];
						useToken();

						System.out.println("CSRF Token is acquired");
					} else {
						throw new Exception("Header not found.");
					}
				} else {
					throw new Exception("Unexpected status code received: " + response.statusCode());
				}
			}

			useToken();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return token;
	}

	private static void useToken() {
		HttpClientHelper.registeredHeaders.update("X-XSRF-TOKEN", token);
	}
}