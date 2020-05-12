package com.photosphere.notification;

import org.apache.curator.framework.CuratorFramework;

public class Publisher {
	private final CuratorFramework client;

	public Publisher(String connectString) {
		client = ZkConn.connect(connectString);

	}

	public void publish(String path, byte[] data) throws Exception {
		if (data != null) {
			ZkUtil.createPath(client, path);
			client.setData().forPath(path, data);
		} else {
			client.delete().forPath(path);
		}
	}
}
