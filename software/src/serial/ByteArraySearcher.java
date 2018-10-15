package serial;

/*
 * Helper functions to search for delimiter
 */
public class ByteArraySearcher {
	byte[] pattern;

	public ByteArraySearcher(byte[] ptr) {
		this.pattern = ptr.clone();
	}

	public int searchHayStack(byte[] haystack, int offset, int length) {
		oloop: for(int i=offset; i<offset + length - pattern.length + 1; i++) {
			for(int j=0; j<pattern.length; j++) {
				if(haystack[i+j] != pattern[j]) {
					continue oloop;
				}
			}
			return i;
		}
		return -1;
	}
}
