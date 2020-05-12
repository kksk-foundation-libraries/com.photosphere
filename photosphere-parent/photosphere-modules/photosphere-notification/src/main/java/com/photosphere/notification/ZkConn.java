package com.photosphere.notification;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

class ZkConn {
	private static final ZkConn INSTANCE = new ZkConn();

	private ZkConn() {
	}

	private final ConcurrentMap<String, CuratorFramework> clients = new ConcurrentHashMap<>();

	public static CuratorFramework connect(String connectString) {
		return INSTANCE._connect(connectString);
	}

	private CuratorFramework _connect(String connectString) {
		return clients.computeIfAbsent(connectString, _connStr -> {
			CuratorFramework client = CuratorFrameworkFactory //
					.builder() //
					.connectString(_connStr) //
					.retryPolicy(new ExponentialBackoffRetry(100, 3)) //
					.build();
			client.start();
			return client;
		});
	}
}
