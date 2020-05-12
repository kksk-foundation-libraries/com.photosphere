package com.photosphere.notification;

import java.util.Arrays;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ZkUtil {
	private static final Logger LOG = LoggerFactory.getLogger(ZkUtil.class);

	private ZkUtil() {
	}

	public static void createPath(CuratorFramework client, String path) throws Exception {
		String[] pathes = path.split(ZKPaths.PATH_SEPARATOR);
		for (int i = 2; i < pathes.length; i++) {
			String subPath = String.join(ZKPaths.PATH_SEPARATOR, Arrays.copyOf(pathes, i));
			Stat stat = client.checkExists().forPath(subPath);
			if (stat == null) {
				LOG.debug("creating...:{}", subPath);
				client.create().forPath(subPath);
			}
		}
		{
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				LOG.debug("creating...:{}", path);
				client.create().forPath(path);
			}
		}
	}
}
