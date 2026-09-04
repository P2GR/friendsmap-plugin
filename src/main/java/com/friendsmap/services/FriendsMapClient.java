/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.friendsmap.model.HeartbeatPayload;
import com.friendsmap.model.HeartbeatResponse;

/**
 * HTTP client for the FriendsMap backend. Java 11 built-in HttpClient + Gson
 * (Gson is provided by the RuneLite client at runtime).
 *
 * <p>Server URL is hardcoded. Token is managed internally by the plugin:
 * first contact calls /register and persists the returned token; heartbeats
 * send {@code Authorization: Bearer <token>} and receive visible friends in
 * the response body (pull model — the server never pushes).</p>
 */
public class FriendsMapClient
{
	public static final String BASE_URL = "https://map.mss54.com";

	private final HttpClient httpClient;
	private final Gson gson = new GsonBuilder().serializeNulls().create();

	public FriendsMapClient()
	{
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();
	}

	/** Probe the backend health endpoint. Never throws. */
	public HealthProbe probe()
	{
		try
		{
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + "/api/v1/health"))
				.timeout(Duration.ofSeconds(3))
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			return new HealthProbe(response.statusCode() == 200, response.statusCode(), response.body());
		}
		catch (Exception e)
		{
			return new HealthProbe(false, -1, e.getMessage());
		}
	}

	/**
	 * Register (idempotent server-side) and return the backend-issued token.
	 * Returns null on any failure.
	 */
	public String register(String username, int world)
	{
		try
		{
			RegisterRequest body = new RegisterRequest();
			body.username = username;
			body.world = world;

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + "/api/v1/register"))
				.timeout(Duration.ofSeconds(3))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200)
			{
				return null;
			}
			RegisterResponse registerResponse = gson.fromJson(response.body(), RegisterResponse.class);
			return registerResponse == null ? null : registerResponse.token;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Send heartbeat; visible friends come back in the HTTP response.
	 */
	public HeartbeatResult heartbeat(HeartbeatPayload payload, String token)
	{
		try
		{
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + "/api/v1/heartbeat"))
				.timeout(Duration.ofSeconds(3))
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200)
			{
				return HeartbeatResult.failure(response.statusCode(), response.body());
			}
			HeartbeatResponse heartbeatResponse = gson.fromJson(response.body(), HeartbeatResponse.class);
			return HeartbeatResult.success(response.statusCode(), response.body(), heartbeatResponse);
		}
		catch (Exception e)
		{
			return HeartbeatResult.error(e.getMessage());
		}
	}

	private static final class RegisterRequest
	{
		private String username;
		private int world;
	}

	private static final class RegisterResponse
	{
		private String accountId;
		private String token;
	}

	public static final class HealthProbe
	{
		private final boolean reachable;
		private final int statusCode;
		private final String body;

		private HealthProbe(boolean reachable, int statusCode, String body)
		{
			this.reachable = reachable;
			this.statusCode = statusCode;
			this.body = body;
		}

		public boolean isReachable()
		{
			return reachable;
		}

		public int getStatusCode()
		{
			return statusCode;
		}

		public String getBody()
		{
			return body;
		}
	}

	public static final class HeartbeatResult
	{
		private final boolean success;
		private final int statusCode;
		private final String rawBody;
		private final HeartbeatResponse response;

		private HeartbeatResult(boolean success, int statusCode, String rawBody, HeartbeatResponse response)
		{
			this.success = success;
			this.statusCode = statusCode;
			this.rawBody = rawBody;
			this.response = response;
		}

		static HeartbeatResult success(int statusCode, String rawBody, HeartbeatResponse response)
		{
			return new HeartbeatResult(true, statusCode, rawBody, response);
		}

		static HeartbeatResult failure(int statusCode, String rawBody)
		{
			return new HeartbeatResult(false, statusCode, rawBody, null);
		}

		static HeartbeatResult error(String message)
		{
			return new HeartbeatResult(false, -1, message, null);
		}

		public boolean isSuccess()
		{
			return success;
		}

		public int getStatusCode()
		{
			return statusCode;
		}

		public String getRawBody()
		{
			return rawBody;
		}

		public HeartbeatResponse getResponse()
		{
			return response;
		}
	}
}
