package com.codesoom.assignment;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {

	public String getGreeting() {
		return "Hello World!";
	}

	public static void main(String[] args) throws IOException {
		var server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/", new CustomHttpHandler());
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		server.start();
	}
}
