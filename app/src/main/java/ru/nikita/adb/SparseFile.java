package ru.nikita.adb;

import java.lang.String;
import java.lang.RuntimeException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.ListIterator;
import ru.nikita.adb.SparseHeader;
import ru.nikita.adb.BackedBlock;
import ru.nikita.adb.BackedBlockFile;
import ru.nikita.adb.BackedBlockFill;

public class SparseFile {
	public SparseFile(String path) throws FileNotFoundException, IOException {
		RandomAccessFile file = new RandomAccessFile(path, "r");
		SparseHeader sparseHeader = new SparseHeader(file);
		create(sparseHeader.getBlockSize(), sparseHeader.getLen());

		int currentBlock = 0;
		for(int i = 0; i < sparseHeader.getTotalChunks(); i++) {
			ChunkHeader chunkHeader = new ChunkHeader(file, sparseHeader.getChunkHeaderSize());

			currentBlock += chunkHeader.processChunk(file, sparseHeader.getChunkHeaderSize(), currentBlock);
		}
		if(sparseHeader.getTotalBlocks() != currentBlock)
			throw new RuntimeException("Sparse block count does not match");
	}

	public byte[] write() throws IOException {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(array);

		new SparseHeader(blockSize, len, countChunks()).write(stream);

		// int lastBlock = 0;
		// for(BackedBlock block : backedBlocks) {
		// 	if(block.getBlock() > lastBlock) {
		// 		int blocks = block.getBlock() - lastBlock;
		// 		chunks++;
		// 	}
		// 	chunks++;
		// 	lastBlock = block.getNextBlock(blockSize);
		// }
		// if(len - lastBlock * blockSize > 0);

		
		return array.toByteArray();
	}

	public int countChunks() {
		int chunks = 0;
		int lastBlock = 0;
		for(BackedBlock block : backedBlocks) {
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
		for(BackedBlock block : backedBlocks) {
			if(block.getBlock() > lastBlock)
				count += SparseHeader.CHUNK_HEADER_LEN;
			lastBlock = block.getNextBlock(blockSize);
			count += block.countSize(blockSize);
		}
		if(lastBlock < (len + blockSize - 1) / blockSize)
			count += SparseHeader.CHUNK_HEADER_LEN;
		return count;
	}

	public SparseFile[] resparse(int maxLen) {
		ArrayList<SparseFile> list = new ArrayList<SparseFile>();
		BackedBlock block = null;
		do {
			SparseFile s = new SparseFile(blockSize, len);
			block = moveChunksUpToLen(s, maxLen);
			list.add(s);
		} while(block != null);
		return list.toArray(new SparseFile[0]);
	}

	private BackedBlock moveChunksUpToLen(SparseFile to, long len) {
		len -= SparseHeader.getOverhead();
		long fileLen = 0;
		int lastBlock = 0;
		ListIterator<BackedBlock> i = backedBlocks.listIterator();
		while(i.hasNext()) {
			BackedBlock block = i.next();
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
					to.backedBlocks.add(i.previous());
					i.remove();
				}
				return block;
			}
			fileLen += count;
		}
		to.backedBlocks = backedBlocks;
		return null;
	}


	private SparseFile(int blockSize, long len) {
		create(blockSize, len);
	}
	private void create(int blockSize, long len) {
		this.blockSize = blockSize;
		this.len = len;
		this.backedBlocks = new ArrayList<BackedBlock>();
	}

	private class ChunkHeader {
		public ChunkHeader(RandomAccessFile file, int chunkHeaderSize) throws IOException {
			type = Short.reverseBytes(file.readShort());
			file.skipBytes(2);
			blocks = Integer.reverseBytes(file.readInt());
			totalSize = Integer.reverseBytes(file.readInt());

			if(chunkHeaderSize > CHUNK_HEADER_LEN)
				file.skipBytes(chunkHeaderSize - CHUNK_HEADER_LEN);

		}

		public int processChunk(RandomAccessFile file, int chunkHeaderSize, int currentBlock) throws IOException {
			int dataSize = totalSize - chunkHeaderSize;
			long offset = file.getFilePointer();
			int len = getLen();
			switch(type) {
			case TYPE_RAW:
				if(dataSize % blockSize != 0 || dataSize / blockSize != blocks)
					throw new RuntimeException(String.format("Invalid data block at %d", offset));
				backedBlocks.add(new BackedBlockFile(currentBlock, len, file, offset));
				file.skipBytes(len);
				return blocks;
			case TYPE_FILL:
				if(dataSize != 4) // sizeof(int)
					throw new RuntimeException(String.format("Invalid fill block at %d", offset));
				int fillValue = file.readInt();
				backedBlocks.add(new BackedBlockFill(currentBlock, len, fillValue));
				return blocks;
			case TYPE_DONT_CARE:
				if(dataSize != 0)
					throw new RuntimeException(String.format("Invalid skip block at %d", offset));
				return blocks;
			case TYPE_CRC32:
				if(dataSize != 4) // sizeof(int)
					throw new RuntimeException(String.format("Invalid crc block at %d", offset));
				return 0;
			default:
				throw new RuntimeException(String.format("Unknown block %04X at %d", type, offset));
			}
		}

		private int getLen() {
			return blocks * blockSize;
		}

		public short type; // 0xcac1 -> raw; 0xcac2 -> fill; 0xcac3 -> don't care
		public int blocks; // in blocks in output image
		public int totalSize; // in bytes of chunk input file including chunk header and data

		private final static short TYPE_RAW			= -13631;
		private final static short TYPE_FILL		= -13630;
		private final static short TYPE_DONT_CARE	= -13629;
		private final static short TYPE_CRC32		= -13628;
		private final static int CHUNK_HEADER_LEN	= 12;
	}

	private int blockSize;
	private long len;
	private ArrayList<BackedBlock> backedBlocks;
}


