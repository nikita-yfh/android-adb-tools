package ru.nikita.adb;

import java.lang.String;
import java.lang.RuntimeException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.ListIterator;
import ru.nikita.adb.SparseHeader;
import ru.nikita.adb.Block;

public class SparseFile {
	public SparseFile(RandomAccessFile file) throws IOException {
		SparseHeader sparseHeader = new SparseHeader(file);
		create(sparseHeader.getBlockSize(), sparseHeader.getLen());

		int currentBlock = 0;
		for(int i = 0; i < sparseHeader.getTotalChunks(); i++)
			currentBlock += Block.addBlock(blockList, file, sparseHeader.getChunkHeaderSize(), blockSize, currentBlock);

		if(sparseHeader.getTotalBlocks() != currentBlock)
			throw new RuntimeException("Sparse block count does not match");
	}

	public byte[] getBytes() throws IOException {
		ByteBuffer bytes = ByteBuffer.allocate((int) countLen());
		bytes.order(ByteOrder.LITTLE_ENDIAN);

		new SparseHeader(blockSize, len, countChunks()).write(bytes);

		int lastBlock = 0;
		for(Block block : blockList) {
			if(block.getBlock() > lastBlock) {
				int blockCount = block.getBlock() - lastBlock;
				Block.writeSkipChunk(bytes, blockCount);
			}
			block.write(bytes, blockSize);
			lastBlock = block.getNextBlock(blockSize);
		}
		long pad = len - lastBlock * blockSize;
		if(pad > 0)
			Block.writeSkipChunk(bytes, (int) (pad / blockSize));
		return bytes.array();
	}

	public int countChunks() {
		int chunks = 0;
		int lastBlock = 0;
		for(Block block : blockList) {
			if(block.getBlock() > lastBlock)
				chunks++;
			chunks++;
			lastBlock = block.getNextBlock(blockSize);
		}
		if(lastBlock < (len + blockSize - 1) / blockSize)
			chunks++;
		return chunks;
	}


	public long countLen() {
		int chunks = countChunks();
		long count = SparseHeader.SPARSE_HEADER_LEN;
		int lastBlock = 0;
		for(Block block : blockList) {
			if(block.getBlock() > lastBlock)
				count += SparseHeader.CHUNK_HEADER_LEN;
			lastBlock = block.getNextBlock(blockSize);
			count += block.countSize(blockSize);
		}
		if(lastBlock < (len + blockSize - 1) / blockSize)
			count += SparseHeader.CHUNK_HEADER_LEN;
		return count;
	}

	public SparseFile[] resparse(long maxLen) {
		ArrayList<SparseFile> list = new ArrayList<SparseFile>();
		Block block = null;
		do {
			SparseFile s = new SparseFile(blockSize, len);
			block = moveChunksUpToLen(s, maxLen);
			list.add(s);
		} while(block != null);
		return list.toArray(new SparseFile[0]);
	}

	private Block moveChunksUpToLen(SparseFile to, long len) {
		len -= SparseHeader.getOverhead();
		long fileLen = 0;
		int lastBlock = 0;
		ListIterator<Block> i = blockList.listIterator();
		while(i.hasNext()) {
			Block block = i.next();
			long count = 0;
			if(block.getBlock() > lastBlock)
				count += SparseHeader.CHUNK_HEADER_LEN;
			lastBlock = block.getNextBlock(to.blockSize);
			count += block.countSize(to.blockSize);
			if(fileLen + count > len) {
				fileLen += SparseHeader.CHUNK_HEADER_LEN;
				if(!i.hasPrevious() || (len - fileLen > (len / 8)))
					i.add(block.split(blockSize, len - fileLen));
				i.previous();
				while(i.hasPrevious()) {
					to.blockList.add(i.previous());
					i.remove();
				}
				return block;
			}
			fileLen += count;
		}
		to.blockList = blockList;
		return null;
	}


	private SparseFile(int blockSize, long len) {
		create(blockSize, len);
	}
	private void create(int blockSize, long len) {
		this.blockSize = blockSize;
		this.len = len;
		this.blockList = new ArrayList<Block>();
	}

	private long len;
	private int blockSize;
	private ArrayList<Block> blockList;
}

