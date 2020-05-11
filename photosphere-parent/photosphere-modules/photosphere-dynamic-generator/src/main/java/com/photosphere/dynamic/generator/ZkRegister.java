package com.photosphere.dynamic.generator;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkRegister implements AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(ZkRegister.class);
	private final String watchPath;
	private final String dataPath;
	private final CuratorFramework client;

	public ZkRegister(String connString, String watchPath, String dataPath) throws Exception {
		this.watchPath = watchPath;
		this.dataPath = dataPath;
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(100, 3);

		client = CuratorFrameworkFactory.newClient(connString, retryPolicy);
		client.start();
		String[] arr;
		arr = dataPath.split(ZKPaths.PATH_SEPARATOR);
		for (int i = 2; i <= arr.length; i++) {
			String path = String.join(ZKPaths.PATH_SEPARATOR, Arrays.copyOf(arr, i));
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				client.create().forPath(path);
			}
		}
		arr = watchPath.split(ZKPaths.PATH_SEPARATOR);
		for (int i = 2; i <= arr.length; i++) {
			String path = String.join(ZKPaths.PATH_SEPARATOR, Arrays.copyOf(arr, i));
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				client.create().forPath(path);
			}
		}
		LOG.debug("watchPath:{}, dataPath:{}", watchPath, dataPath);
	}

	@Override
	public void close() throws Exception {
		client.close();
	}

	public void register(String alias, String fqcn, String source) throws Exception {
		register(alias, fqcn.getBytes(), source.getBytes());
	}

	public void register(String alias, String fqcn, File sourceFile) throws Exception {
		register(alias, fqcn.getBytes(), Files.readAllBytes(sourceFile.toPath()));
	}

	private void register(String alias, byte[] fqcn, byte[] source) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("creating alias:{}", alias);
			LOG.debug("creating path:{}", ZKPaths.makePath(dataPath, alias));
		}
		client.create().forPath(ZKPaths.makePath(dataPath, alias));
		client.create().forPath(ZKPaths.makePath(dataPath, alias, "fqcn"), fqcn);
		client.create().forPath(ZKPaths.makePath(dataPath, alias, "source"), source);
		byte[] data = new byte[Long.BYTES];
		putLong(data, 0, System.currentTimeMillis());
		if (LOG.isDebugEnabled()) {
			LOG.debug("creating path:{}", ZKPaths.makePath(watchPath, alias));
		}
		client.create().forPath(ZKPaths.makePath(watchPath, alias), data);
		if (LOG.isDebugEnabled()) {
			LOG.debug("created alias:{}", alias);
		}
	}

	static void putLong(byte[] b, int off, long val) {
		b[off + 7] = (byte) (val);
		b[off + 6] = (byte) (val >>> 8);
		b[off + 5] = (byte) (val >>> 16);
		b[off + 4] = (byte) (val >>> 24);
		b[off + 3] = (byte) (val >>> 32);
		b[off + 2] = (byte) (val >>> 40);
		b[off + 1] = (byte) (val >>> 48);
		b[off] = (byte) (val >>> 56);
	}
}
