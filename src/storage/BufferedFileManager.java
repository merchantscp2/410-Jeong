package storage;

/**
 * A {@code BufferedFileManager} manages a storage space using the slotted page format and buffering.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 */
public class BufferedFileManager extends FileManager {

	// TODO complete this class (5 points)

	/**
	 * Constructs a {@code BufferedFileManager}.
	 * 
	 * @param slottedPageSize
	 *            the size (in bytes) of {@code SlottedPage}s
	 * @param bufferSize
	 *            the number of {@code SlottedPage}s that the buffer can maintain
	 */
	public BufferedFileManager(int slottedPageSize, int bufferSize) {
		super(slottedPageSize);
	}

}
