package com.photosphere.notification;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscriber {
	private static final Logger LOG = LoggerFactory.getLogger(Subscriber.class);

	public static final BiConsumer<String, byte[]> LOGGING_ON_INITIALIZED = (path, data) -> {
		if (LOG.isDebugEnabled())
			LOG.debug("onInitialized:{}", path);
	};
	public static final BiConsumer<String, byte[]> LOGGING_ON_ADDED = (path, data) -> {
		if (LOG.isDebugEnabled())
			LOG.debug("onAdded:{}", path);
	};
	public static final BiConsumer<String, byte[]> LOGGING_ON_UPDATED = (path, data) -> {
		if (LOG.isDebugEnabled())
			LOG.debug("onUpdated:{}", path);
	};
	public static final Consumer<String> LOGGING_ON_REMOVED = (path) -> {
		if (LOG.isDebugEnabled())
			LOG.debug("onRemoved:{}", path);
	};
	public static final BiConsumer<String, Exception> LOGGING_ON_ERRORED = (path, e) -> {
		if (LOG.isWarnEnabled())
			LOG.warn("onErrored:" + path, e);
	};
	public static final BiConsumer<String, byte[]> NOOP_ON_INITIALIZED = (path, data) -> {
	};
	public static final BiConsumer<String, byte[]> NOOP_ON_ADDED = (path, data) -> {
	};
	public static final BiConsumer<String, byte[]> NOOP_ON_UPDATED = (path, data) -> {
	};
	public static final Consumer<String> NOOP_ON_REMOVED = (path) -> {
	};
	public static final BiConsumer<String, Exception> NOOP_ON_ERRORED = (path, e) -> {
	};

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String connectString = null;
		private String watchPath = null;
		private BiConsumer<String, byte[]> onInitialized = NOOP_ON_INITIALIZED;
		private BiConsumer<String, byte[]> onAdded = NOOP_ON_ADDED;
		private BiConsumer<String, byte[]> onUpdated = NOOP_ON_UPDATED;
		private Consumer<String> onRemoved = NOOP_ON_REMOVED;
		private BiConsumer<String, Exception> onErrored = LOGGING_ON_ERRORED;

		private Builder() {

		}

		public Builder connectString(String connectString) {
			this.connectString = connectString;
			return this;
		}

		public Builder watchPath(String watchPath) {
			this.watchPath = watchPath;
			return this;
		}

		public Builder onInitialized(BiConsumer<String, byte[]> consumer) {
			onInitialized = consumer;
			return this;
		}

		public Builder onAdded(BiConsumer<String, byte[]> consumer) {
			onAdded = consumer;
			return this;
		}

		public Builder onUpdated(BiConsumer<String, byte[]> consumer) {
			onUpdated = consumer;
			return this;
		}

		public Builder onRemoved(Consumer<String> consumer) {
			onRemoved = consumer;
			return this;
		}

		public Builder onErrored(BiConsumer<String, Exception> consumer) {
			onErrored = consumer;
			return this;
		}

		public Subscriber build() {
			return new Subscriber(connectString, watchPath, onInitialized, onAdded, onUpdated, onRemoved, onErrored);
		}
	}

	private Subscriber( //
			String connectString, //
			String watchPath, //
			BiConsumer<String, byte[]> onInitialized, //
			BiConsumer<String, byte[]> onAdded, //
			BiConsumer<String, byte[]> onUpdated, //
			Consumer<String> onRemoved, //
			BiConsumer<String, Exception> onErrored //
	) {
		CuratorFramework client = ZkConn.connect(connectString);
		try {
			ZkUtil.createPath(client, watchPath);
		} catch (Exception ignore) {
		}
		int depth = watchPath.split(ZKPaths.PATH_SEPARATOR).length + 1;
		final AtomicBoolean initialized = new AtomicBoolean(false);
		TreeCache _cache = TreeCache.newBuilder(client, watchPath) //
				.disableZkWatches(false) //
				.setCacheData(false) //
				.setCreateParentNodes(true) //
				.setMaxDepth(depth) //
				.build();
		_cache.getListenable().addListener((c, event) -> {
			LOG.debug("event:{}", event);
			switch (event.getType()) {
			case NODE_ADDED:
				if (initialized.get()) {
					if (onAdded != null && onAdded != NOOP_ON_ADDED) {
						String path = event.getData().getPath();
						if (path.split(ZKPaths.PATH_SEPARATOR).length == depth) {
							onAdded.accept(path, event.getData().getData());
						}
					}
				} else {
					if (onInitialized != null && onInitialized != NOOP_ON_INITIALIZED) {
						String path = event.getData().getPath();
						if (path.split(ZKPaths.PATH_SEPARATOR).length == depth) {
							onInitialized.accept(path, event.getData().getData());
						}
					}
				}
				break;
			case NODE_UPDATED:
				if (onUpdated != null && onUpdated != NOOP_ON_UPDATED) {
					String path = event.getData().getPath();
					if (path.split(ZKPaths.PATH_SEPARATOR).length == depth) {
						onUpdated.accept(path, event.getData().getData());
					}
				}
				break;
			case NODE_REMOVED:
				if (onRemoved != null && onRemoved != NOOP_ON_REMOVED) {
					String path = event.getData().getPath();
					if (path.split(ZKPaths.PATH_SEPARATOR).length == depth) {
						onRemoved.accept(path);
					}
				}
				break;
			case INITIALIZED:
				initialized.set(true);
				break;
			default:
				return;
			}
		});
		try {
			_cache.start();
		} catch (Exception e) {
			if (onErrored != null) {
				onErrored.accept(watchPath, e);
			}
			LOG.error("could not start curator tree cache.", e);
		}
	}
}
