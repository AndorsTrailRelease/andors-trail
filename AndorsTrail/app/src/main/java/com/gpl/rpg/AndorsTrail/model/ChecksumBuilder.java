package com.gpl.rpg.AndorsTrail.model;

import android.os.Build;

import com.gpl.rpg.AndorsTrail.util.L;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumBuilder {

	public static final int CHECKSUM_LENGTH = 32;// 256 bits (depends on the hash algorithm)
	public static final String CHECKSUM_ALGORITHM = "SHA-256"; //Should be available in all Android versions
	private ByteBuffer buffer;
	private final MessageDigest digest;

	private ChecksumBuilder(int initialCapacity, ByteOrder byteOrder) {
		buffer = ByteBuffer.allocate(initialCapacity);
		buffer.order(byteOrder);
		try {
			digest = MessageDigest.getInstance(CHECKSUM_ALGORITHM); // Or SHA-512 for even stronger hash
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Hash algorithm not found", e);
		}
	}

	private ChecksumBuilder(int initialCapacity) {
		this(initialCapacity, ByteOrder.BIG_ENDIAN); // Default to big-endian
	}

	public ChecksumBuilder() {
		this(1024*10); // A reasonable default initial capacity
	}

	// --- Methods for adding different data types ---

	public ChecksumBuilder add(String value) {
		if (value != null) {
			byte[] bytes;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				bytes = value.getBytes(StandardCharsets.UTF_8);
			} else {
				bytes = value.getBytes();
			}
			add(bytes);
		} else {
			add(-1); // Use -1 to represent a null string
		}
		return this;
	}

	public ChecksumBuilder add(byte[] bytes) {
		add(bytes.length);						  // Add length prefix
		ensureCapacity(bytes.length + 4); // +4 for length prefix (int)
		buffer.put(bytes);
		return this;
	}

	public ChecksumBuilder add(boolean value) {
		ensureCapacity(1);
		buffer.put(value ? (byte) 1 : (byte) 0);
		return this;
	}

	public ChecksumBuilder add(long value) {
		ensureCapacity(8);
		buffer.putLong(value);
		return this;
	}

	public ChecksumBuilder add(int value) {
		ensureCapacity(4);
		buffer.putInt(value);
		return this;
	}

	public ChecksumBuilder add(float value) {
		ensureCapacity(8);
		buffer.putFloat(value);
		return this;
	}

	public ChecksumBuilder add(double value) {
		ensureCapacity(4);
		buffer.putDouble(value);
		return this;
	}

	// --- Method to finalize and get the checksum ---

	public byte[] build() throws DigestException {
		buffer.flip(); // Prepare for reading
		digest.update(buffer);// Only use the actually used part of the buffer
		buffer.flip(); // Prepare for further writing
		return digest.digest();
	}


	// --- Utility method to ensure sufficient capacity ---
	private void ensureCapacity(int required) {
		if (buffer.remaining() < required) {
			int newCapacity = Math.max(buffer.capacity() * 2, buffer.capacity() + required);
			ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
			newBuffer.order(buffer.order());
			buffer.flip(); // Prepare for reading
			newBuffer.put(buffer); // Copy existing data
			buffer = newBuffer;    // Assign the new buffer to the field
		}
	}

}
