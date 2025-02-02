package com.sparkcleancebu.http_helper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientHelper {	
	private static String baseUrl = "";
	public static HttpHeaders registeredHeaders = new HttpHeaders();
	
	public static void setBaseUrl(String url) {
		baseUrl = url;
	}
	
	public static String getBaseUrl() {
		return baseUrl;
	}
	
	public static URI createUrl(String url) {
		return URI.create(baseUrl + url);
	}
	
	public static HttpResponse<String> get(String url, HttpHeaders headers) throws Exception {
		HttpHeaders compiledHeaders = mergeHeaders(registeredHeaders, headers);
		return sendGet(url, compiledHeaders);
	}

	public static HttpResponse<String> get(String url) throws Exception {
		return sendGet(url, registeredHeaders);
	}
	
	public static HttpResponse<String> post(String url, FormData formData, HttpHeaders headers) throws Exception {
		HttpHeaders compiledHeaders = mergeHeaders(registeredHeaders, headers);
		return sendPost(url, formData, compiledHeaders);
	}
	
	public static HttpResponse<String> post(String url, FormData formData) throws Exception {
		return sendPost(url, formData, registeredHeaders);
	}
	
	private static HttpResponse<String> sendPost(String url, FormData formData, HttpHeaders headers) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(createUrl(url));
		
		if(headers.size() > 0) {
			for(HttpHeaders.Header h : headers) {
				requestBuilder.header(h.getName(), h.getValue());
			}
		}
		
		requestBuilder.header("Content-Type", "multipart/form-data; boundary=" + FormData.separator);
		
		try {
			requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(formData.build().toByteArray()));

			HttpRequest request = requestBuilder.build();
			
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			   
			return response;
		} catch (Exception e) {
			throw e;
		}
	}
	
	private static HttpResponse<String> sendGet(String url, HttpHeaders headers) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(createUrl(url));
		
		if(headers.size() > 0) {
			for(HttpHeaders.Header h : headers) {
				requestBuilder.header(h.getName(), h.getValue());
			}
		}
		
		requestBuilder.header("Content-Type", "application/json");
		requestBuilder.GET();

		HttpRequest request = requestBuilder.build();
	
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        } catch (Exception e) {
        	throw e;
        }
	}

	public static HttpHeaders mergeHeaders(HttpHeaders h1, HttpHeaders h2) {
		HttpHeaders merged = new HttpHeaders();

		// Add all headers from h1 to merged
		for (HttpHeaders.Header header : h1) {
			merged.add(header.getName(), header.getValue());
		}

		// Add or update headers from h2 to merged
		for (HttpHeaders.Header header : h2) {
			merged.update(header.getName(), header.getValue());
		}
		
		return merged;
	}
}