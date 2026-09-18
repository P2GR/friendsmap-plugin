/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.friendsmap.model.HeartbeatPayload;
import com.friendsmap.model.HeartbeatResponse;

/**
 * HTTP client for the FriendsMap backend. Uses the OkHttpClient and Gson
 * provided by the RuneLite client.
 *
 * <p>Server URL is hardcoded. Token is managed internally by the plugin:
 * first contact calls /register and persists the returned token; heartbeats
 * send {@code Authorization: Bearer <token>} and receive visible friends in
 * the response body (pull model — the server never pushes).</p>
 *
 * <p>All methods are blocking and must not be called on the client thread or
 * the EDT — RuneLite's OkHttpClient enforces this with an interceptor.</p>
 */
public class FriendsMapClient
{
	public static final String BASE_URL = "https://map.mss54.com";

	private static final MediaType JSON = MediaType.parse("application/json");
	private static final long TIMEOUT_SECONDS = 3;

	private final OkHttpClient httpClient;
	private final Gson gson;

	@Inject
	public FriendsMapClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.gson = gson.newBuilder().serializeNulls().create();
		// Derived from RuneLite's client so its interceptor chain (client-thread
		// guard, blocked-domain check) is retained and the connection pool and
		// dispatcher are shared.
		this.httpClient = okHttpClient.newBuilder()
			.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();
	}

	/** Read and close the response body; never returns null. */
	private static String bodyString(Response response) throws IOException
	{
		try (ResponseBody body = response.body())
		{
			return body == null ? "" : body.string();
		}
	}

	/** Probe the backend health endpoint. Never throws. */
	public HealthProbe probe()
	{
		Request request = new Request.Builder()
			.url(BASE_URL + "/api/v1/health")
			.get()
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			return new HealthProbe(response.code() == 200, response.code(), bodyString(response));
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
		RegisterRequest body = new RegisterRequest();
		body.username = username;
		body.world = world;

		Request request = new Request.Builder()
			.url(BASE_URL + "/api/v1/register")
			.header("Content-Type", "application/json")
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			String rawBody = bodyString(response);
			if (response.code() != 200)
			{
				return null;
			}
			RegisterResponse registerResponse = gson.fromJson(rawBody, RegisterResponse.class);
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
		Request request = new Request.Builder()
			.url(BASE_URL + "/api/v1/heartbeat")
			.header("Authorization", "Bearer " + token)
			.header("Content-Type", "application/json")
			.post(RequestBody.create(JSON, gson.toJson(payload)))
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			String rawBody = bodyString(response);
			if (response.code() != 200)
			{
				return HeartbeatResult.failure(response.code(), rawBody);
			}
			HeartbeatResponse heartbeatResponse = gson.fromJson(rawBody, HeartbeatResponse.class);
			return HeartbeatResult.success(response.code(), rawBody, heartbeatResponse);
		}
		catch (Exception e)
		{
			return HeartbeatResult.error(e.getMessage());
		}
	}

	/**
	 * POST /api/v1/register request body. The fields are read only by Gson
	 * reflection, which the compiler cannot see, hence the suppression.
	 */
	@SuppressWarnings("unused")
	private static final class RegisterRequest
	{
		private String username;
		private int world;
	}

	/**
	 * POST /api/v1/register response body. {@code token} is the only field the
	 * plugin consumes; {@code accountId} is kept because it is part of the
	 * server contract (Gson ignores fields that are absent from the JSON).
	 */
	@SuppressWarnings("unused")
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
