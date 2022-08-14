package ru.nikita.adb;

import ru.nikita.adb.SparseHeader;

public class BackedBlockFill extends BackedBlock {
	public BackedBlockFill(int block, int len, int value) {
		super(block, len);
		this.value = value;
	}
	
	@Override
	public int countSize(int blockSize) {
		return SparseHeader.CHUNK_HEADER_LEN + 4;
	}

	@Override
	public BackedBlock _split(int newLen, int newBlock) {
		return new BackedBlockFill(newLen, newBlock, value);
	}

	private int value;
}

