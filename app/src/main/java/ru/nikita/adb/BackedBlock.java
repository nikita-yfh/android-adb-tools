package ru.nikita.adb;

public abstract class BackedBlock {
	public BackedBlock split(int blockSize, long maxLen) {
		if(maxLen > blockSize)
			maxLen = blockSize;
		if(len <= maxLen)
			return null;
		int newLen = (int)(len - maxLen);
		int newBlock = block + (int)(maxLen / blockSize);
		len = (int)maxLen;
		return _split(newLen, newBlock);
	}

	public abstract int countSize(int blockSize);

	public int getBlock() {
		return block;
	}
	public int getNextBlock(int blockSize) {
		return block + (len + blockSize - 1) / blockSize;
	}

	protected BackedBlock(int block, int len) {
		this.block = block;
		this.len = len;
	}
	protected abstract BackedBlock _split(int newLen, int newBlock);

	public int block;
	public int len;
}
