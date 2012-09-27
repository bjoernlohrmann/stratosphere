package eu.stratosphere.nephele.streaming.buffers;


public class BufferSizeHistory {

	private BufferSizeHistoryEntry[] entries;

	private int entriesInHistory;

	public BufferSizeHistory(int noOfHistoryEntries) {
		this.entries = new BufferSizeHistoryEntry[noOfHistoryEntries];
		this.entriesInHistory = 0;
	}

	public void addToHistory(long timestamp, int newBufferSize) {
		BufferSizeHistoryEntry newEntry = new BufferSizeHistoryEntry(Math.min(entriesInHistory, entries.length - 1),
			timestamp, newBufferSize);

		if (entriesInHistory < entries.length) {
			entries[entriesInHistory] = newEntry;
			entriesInHistory++;
		} else {
			System.arraycopy(entries, 1, entries, 0, entriesInHistory - 1);
			entries[entriesInHistory - 1] = newEntry;
		}
	}

	public BufferSizeHistoryEntry[] getEntries() {
		return entries;
	}

	public BufferSizeHistoryEntry getFirstEntry() {
		return entries[0];
	}

	public BufferSizeHistoryEntry getLastEntry() {
		if (entriesInHistory > 0) {
			return entries[entriesInHistory - 1];
		} else {
			return null;
		}
	}

	public boolean hasEntries() {
		return entriesInHistory > 0;
	}

	public int getNumberOfEntries() {
		return entriesInHistory;
	}
}
