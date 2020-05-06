package com.photosphere.colfer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface ColferObject {
	byte[] marshal(OutputStream out, byte[] buf) throws IOException;

	int unmarshal(byte[] buf, int offset);

	default byte[] marshal() {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			marshal(out, null);
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	default <T extends AbstractColferObject> T unmarshal(byte[] buf) {
		unmarshal(buf, 0);
		return (T) this;
	}
}
