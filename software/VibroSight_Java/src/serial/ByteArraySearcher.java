package serial;

/*
 * Class to search for delimiter
 */

public class ByteArraySearcher {
	byte[] pattern;

	public ByteArraySearcher(byte[] buffer) {
		this.pattern = buffer.clone();
	}

	public int searchStack(byte[] myStack, int offset, int length) {
		myLoop: for(int i=offset; i<offset + length - pattern.length + 1; i++) {
			for(int j=0; j<pattern.length; j++) {
				if(myStack[i+j] != pattern[j]) {
					continue myLoop;
				}
			}
			return i;
		}
		return -1;
	}
}
