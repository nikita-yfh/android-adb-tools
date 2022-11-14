package ru.nikita.adb;

import java.lang.RuntimeException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class SparseHeader {
	public SparseHeader(RandomAccessFile file) throws IOException {
		file.seek(0);

		magic = Integer.reverseBytes(file.readInt());
		majorVersion = Short.reverseBytes(file.readShort());
		minorVersion = Short.reverseBytes(file.readShort());
		fileHeaderSize = Short.reverseBytes(file.readShort());
		chunkHeaderSize = Short.reverseBytes(file.readShort());
		blockSize = Integer.reverseBytes(file.readInt());
		totalBlocks = Integer.reverseBytes(file.readInt());
		totalChunks = Integer.reverseBytes(file.readInt());
		imageChecksum = Integer.reverseBytes(file.readInt());

		if(magic != MAGIC)
			throw new RuntimeException("Sparse header magic does not match");
		if(majorVersion != MAJOR_VER)
			throw new RuntimeException("Sparse header major version does not match");
		if(fileHeaderSize < SPARSE_HEADER_LEN)
			throw new RuntimeException("Invalid sparse header size");
		if(chunkHeaderSize < CHUNK_HEADER_LEN)
			throw new RuntimeException("Invalid chunk header size");

		if(fileHeaderSize > SPARSE_HEADER_LEN)
			file.skipBytes(fileHeaderSize - SPARSE_HEADER_LEN);
	}

	public SparseHeader(int blockSize, long len, int chunks) {
		this.magic = MAGIC;
		this.majorVersion = MAJOR_VER;
		this.minorVersion = MINOR_VER;
		this.fileHeaderSize = SPARSE_HEADER_LEN;
		this.chunkHeaderSize = CHUNK_HEADER_LEN;
		this.blockSize = blockSize;
		this.totalBlocks = (int) (len + blockSize - 1) / blockSize;
		this.totalChunks = chunks;
		this.imageChecksum = 0;
	}

	public void write(ByteBuffer bytes) {
		bytes.putInt(magic);
		bytes.putShort(majorVersion);
		bytes.putShort(minorVersion);
		bytes.putShort(fileHeaderSize);
		bytes.putShort(chunkHeaderSize);
		bytes.putInt(blockSize);
		bytes.putInt(totalBlocks);
		bytes.putInt(totalChunks);
		bytes.putInt(imageChecksum);
	}

	public long getLen() {
		return totalBlocks * blockSize;
	}
	public int getBlockSize() {
		return blockSize;
	}
	public int getTotalBlocks() {
		return totalBlocks;
	}
	public int getTotalChunks() {
		return totalChunks;
	}
	public int getChunkHeaderSize() {
		return chunkHeaderSize;
	}

	public static int getOverhead() {
		return SPARSE_HEADER_LEN + 2 * CHUNK_HEADER_LEN + 4;
	}

	private int magic; // 0xed26ff3a
	private short majorVersion; // (0x1) - reject images with higher major versions
	private short minorVersion; // (0x0) - allow imager with higher monor versions
	private short fileHeaderSize; // 28 bytes for first revision of the file format
	private short chunkHeaderSize; // 12 bytes for first revision of the file format
	private int blockSize; // block size in bytes, must me a multiple of 4 (4096)
	private int totalBlocks; // total blocks in the not-sparse output image
	private int totalChunks; // total chunks in the sparse input image
	private int imageChecksum; // CRC32 checksum of the original data, counting "don't care"

	public final static int MAGIC				= 0xed26ff3a;
	public final static int MAJOR_VER			= 1;
	public final static int MINOR_VER			= 0;
	public final static int SPARSE_HEADER_LEN	= 28;
	public final static int CHUNK_HEADER_LEN	= 12;
}

