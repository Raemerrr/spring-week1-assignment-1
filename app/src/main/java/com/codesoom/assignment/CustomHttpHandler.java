package com.codesoom.assignment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomHttpHandler implements com.sun.net.httpserver.HttpHandler {

	private static final Map<Integer, TaskItem> TASK_MAP = new ConcurrentHashMap<>();
	private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(1);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		var method = exchange.getRequestMethod();
		int rCode;
		var pathVariableId = getPathVariableId(exchange);
		String response;
		switch (method) {
			case "GET" -> {
				if (pathVariableId == null) {
					var taskItems = handleGet();
					response = writeValueAsString(taskItems);
				} else {
					var taskItem = handleGet(pathVariableId);
					response = writeValueAsString(taskItem);
				}
				rCode = response == null ? 404 : 200;
			}
			case "POST" -> {
				var taskItem = handlePost(exchange);
				response = writeValueAsString(taskItem);
				rCode = 201;
			}
			case "PUT" -> {
				var taskItem = handlePut(exchange, pathVariableId);
				response = writeValueAsString(taskItem);
				rCode = response == null ? 404 : 200;
			}
			case "DELETE" -> {
				var taskItem = handleDelete(pathVariableId);
				response = writeValueAsString(taskItem);
				rCode = response == null ? 404 : 204;
			}
			default -> throw new IllegalAccessError();
		}
		sendResponse(exchange, response, rCode);
	}

	private TaskItem handleDelete(Integer id) {
		if (id == null) {
			return null;
		}
		if (TASK_MAP.containsKey(id)) {
			return TASK_MAP.remove(id);
		}
		return null;
	}

	private TaskItem handlePut(HttpExchange exchange, Integer id) {
		if (id == null) {
			return null;
		}
		var upsertTaskRequest = getRequestBody(exchange);
		if (upsertTaskRequest == null) {
			return null;
		}

		var taskItem = TASK_MAP.get(id);
		if (taskItem != null) {
			taskItem.setTitle(upsertTaskRequest.getTitle());
			return taskItem;
		}
		return null;
	}

	private TaskItem handlePost(HttpExchange exchange) {
		var upsertTaskRequest = getRequestBody(exchange);
		if (upsertTaskRequest == null) {
			return null;
		}
		var taskItem = new TaskItem(generateTaskId(), upsertTaskRequest.getTitle());
		TASK_MAP.put(taskItem.id, taskItem);
		return taskItem;
	}

	private Collection<TaskItem> handleGet() {
		return TASK_MAP.values();
	}

	private TaskItem handleGet(Integer id) {
		if (id == null) {
			return null;
		}
		return TASK_MAP.get(id);
	}

	private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		var responseBytes = (response == null || response.isBlank()) ? new byte[0] : response.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, responseBytes.length);

		if (responseBytes.length > 0) {
			var os = exchange.getResponseBody();
			os.write(responseBytes);
			os.flush();
			os.close();
		} else {
			exchange.getResponseBody().close();
		}
	}

	private Integer getPathVariableId(HttpExchange exchange) {
		var path = exchange.getRequestURI().getPath();
		var pathParts = path.split("/");
		var id = (pathParts.length == 3) ? pathParts[2] : null;
		try {
			if (id == null) {
				return null;
			}
			return Integer.valueOf(id);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private int generateTaskId() {
		return ATOMIC_INTEGER.getAndIncrement();
	}

	private UpsertTaskRequest getRequestBody(HttpExchange exchange) {
		var inputStream = exchange.getRequestBody();
		try {
			var requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			return OBJECT_MAPPER.readValue(requestBody, UpsertTaskRequest.class);
		} catch (IOException e) {
			return null;
		}
	}

	private String writeValueAsString(TaskItem taskItem) {
		if (taskItem == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(taskItem);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private String writeValueAsString(Collection<TaskItem> taskItems) {
		if (taskItems == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(taskItems);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	static class TaskItem {

		private int id;
		private String title;

		public TaskItem() {
		}

		public TaskItem(int id, String title) {
			this.id = id;
			this.title = title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public int getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}

	static class UpsertTaskRequest {

		private String title;

		public String getTitle() {
			return title;
		}
	}
}
