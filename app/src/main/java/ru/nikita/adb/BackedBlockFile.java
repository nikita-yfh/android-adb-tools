package ru.nikita.adb;

import java.io.RandomAccessFile;

public class BackedBlockFile extends BackedBlock {
	public BackedBlockFile(int block, int len, RandomAccessFile file, long offset) {
		super(block, len);
		this.file = file;
		this.offset = offset;
	}

	@Override
	public int countSize(int blockSize) {
		int alignedLen = blockSize * ((len + blockSize - 1) / blockSize);
		return SparseHeader.CHUNK_HEADER_LEN + alignedLen;
	}

	@Override
	public BackedBlock _split(int newLen, int newBlock) {
		long newOffset = offset + len;
		return new BackedBlockFile(newLen, newBlock, file, newOffset);
	}

	private RandomAccessFile file;
	private long offset;
}
