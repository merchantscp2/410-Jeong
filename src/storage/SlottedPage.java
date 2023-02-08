package storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A {@code SlottedPage} can store objects of possibly different sizes in a byte array.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 */
public class SlottedPage implements Iterable<Object> {

	/**
	 * The ID of this {@code SlottedPage}.
	 */
	int pageID;

	/**
	 * A byte array for storing the header of this {@code SlottedPage} and objects.
	 */
	byte[] data;

	/**
	 * Constructs a {@code SlottedPage}.
	 * 
	 * @param pageID
	 *            the ID of the {@code SlottedPage}
	 * @param size
	 *            the size (in bytes) of the {@code SlottedPage}
	 */
	public SlottedPage(int pageID, int size) {
		data = new byte[size];
		this.pageID = pageID;
		setEntryCount(0);
		setStartOfDataStorage(data.length - Integer.BYTES);
	}

	@Override
	public String toString() {
		String s = "";
		for (Object o : this) {
			if (s.length() > 0)
				s += ", ";
			s += o;
		}
		return "(page ID: " + pageID + ", objects: [" + s + "])";
	}

	/**
	 * Returns the ID of this {@code SlottedPage}.
	 * 
	 * @return the ID of this {@code SlottedPage}
	 */
	public int pageID() {
		return pageID;
	}

	/**
	 * Returns the byte array of this {@code SlottedPage}.
	 * 
	 * @return the byte array of this {@code SlottedPage}
	 */
	public byte[] data() {
		return data;
	}

	/**
	 * Returns the number of entries in this {@code SlottedPage}.
	 * 
	 * @return the number of entries in this {@code SlottedPage}
	 */
	public int entryCount() {
		return readInt(0);
	}

	/**
	 * Adds the specified object in this {@code SlottedPage}.
	 * 
	 * @param o
	 *            an object to add
	 * @return the index for the object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws OverflowException
	 *             if this {@code SlottedPage} cannot accommodate the specified object
	 */
	public int add(Object o) throws IOException, OverflowException {
		// First let's check if it will fit :D
		
		// Save the object
		int saved_loc = save(o);

		// Going to be zero initially
		int current_index = readInt(0); 

		// increment the counter by 1
		writeInt(0, current_index+1);
		
		// save the location to the header. This will be counter index
		saveLocation(current_index, saved_loc);

		return current_index;
		// TODO complete this method (20 points)
		// throw new UnsupportedOperationException();
	}

	/**
	 * Returns the object at the specified index in this {@code SlottedPage} ({@code null} if that object was removed
	 * from this {@code SlottedPage}).
	 * 
	 * @param index
	 *            an index
	 * @return the object at the specified index in this {@code SlottedPage}; {@code null} if that object was removed
	 *         from this {@code SlottedPage}
	 * @throws IndexOutOfBoundsException
	 *             if an invalid index is given
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Object get(int index) throws IndexOutOfBoundsException, IOException {
		// TODO complete this method (20 points)
		// Get the location of the data from the index
		int data_loc = getLocation(index);
		// Check for a removed item and if so, return null
		if(data_loc == -1) {
			return null;
		}
		// Create a new object
		Object o;
		try {
			// Read the object from the data
		 	o = toObject(data, data_loc);
		} catch (Exception e) { // IF it can't be found, throw a new except
			throw new IndexOutOfBoundsException();
		}
		// If it was read, return it
		return o;
		//throw new UnsupportedOperationException();
	}

	/**
	 * Puts the specified object at the specified index in this {@code SlottedPage}.
	 * 
	 * @param index
	 *            an index
	 * @param o
	 *            an object to add
	 * @return the object stored previously at the specified location in this {@code SlottedPage}; {@code null} if no
	 *         such object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws OverflowException
	 *             if this {@code SlottedPage} cannot accommodate the specified object
	 * @throws IndexOutOfBoundsException
	 *             if an invalid index is used
	 */
	public Object put(int index, Object o) throws IOException, OverflowException, IndexOutOfBoundsException {
		if (index == entryCount()) {
			add(o);
			return null;
		}
		Object old = get(index);
		byte[] b = toByteArray(o);
		if (old != null && b.length <= toByteArray(old).length)
			System.arraycopy(b, 0, data, getLocation(index), b.length);
		else
			saveLocation(index, save(o));
		return old;
	}

	/**
	 * Removes the object at the specified index from this {@code SlottedPage}.
	 * 
	 * @param index
	 *            an index within this {@code SlottedPage}
	 * @return the object stored previously at the specified location in this {@code SlottedPage}; {@code null} if no
	 *         such object
	 * @throws IndexOutOfBoundsException
	 *             if an invalid index is used
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Object remove(int index) throws IndexOutOfBoundsException, IOException {
		// We're lazy, so we'll just set anything deleted to -1

		// If the object is 'deleted', return null
		if(getLocation(index) == -1) {
			return null;
		}
		
		// Cache the object
		Object o = get(index);

		// Then we'll set the header entry to -1
		saveLocation(index, -1);

		// and return the object
		return o;

		//throw new UnsupportedOperationException();
	}

	/**
	 * Returns an iterator over all objects stored in this {@code SlottedPage}.
	 */
	@Override
	public Iterator<Object> iterator() {
		
		class SlottedPageIterator<T> implements Iterator {
			SlottedPage sp;
			// store the current index and the count
			int current = 0, count;

			public SlottedPageIterator(SlottedPage sp) {
				this.sp = sp;
				count = sp.entryCount();
			}

			@Override
			public boolean hasNext() {
				// check if there are more still to go
				return (count > 0);
			}

			@Override
			public Object next() {
				Object o = null; // initialize to null
				while(o == null) { // if the object is null (first, or was removed)
					try{
						o = get(current++); // fetch the next one
						count --; // mark it as removed
					} catch(Exception e) {
						e.printStackTrace();
						throw new RuntimeException();
					}
				}
				return o;
			}
		}
		return new SlottedPageIterator<Object>(this);
		//throw new UnsupportedOperationException();
	}

	private static void p(String s) { System.out.println(s);}

	protected int get_next_valid_entry_addr(int current_index) {
		// Initialize the next address var
		int next_address = -1;
		do { // If we reach the end of the indexes
			if(current_index == this.entryCount()) {
				return this.startOfDataStorage(); // The last item will
			}
			try {
				next_address = getLocation(current_index++);
			} catch(Exception e) {}
		} while(next_address == -1);
		return next_address;
		// If its, not, we'll loop through the next indexes and find the next not deleted one
		// int of = 1;
		// int next_address = getLocation(current_index - of);
		// while(next_address == -1)
		// {
		// 	try{
		// 		next_address = getLocation(current_index - (of++));
		// 	} catch(Exception e) {
		// 		// FIXME except here
		// 	}
		// }
		// p("Next Address is: " + next_address);
		// return next_address;
	}
	protected int get_previous_valid_entry_addr(int current_index) {
		int previous_address = -1;
		do { // If we reach the end of the indexes
			if(current_index == 0) {
				return data.length - Integer.BYTES; // The last item will
			}
			try {
				previous_address = getLocation(current_index--);
			} catch(Exception e) {}
		} while(previous_address == -1);
		return previous_address;
		// if(current_index == this.entryCount()) // if its the last index, 
		// 	return this.startOfDataStorage();

		// int of = 1;
		// int next_address = getLocation(current_index + of);
		// while(next_address == -1)
		// {
		// 	try{
		// 		next_address = getLocation(current_index + (of++));
		// 	} catch(Exception e) {
		// 		// FIXME except here
		// 	}
		// }
		// p("Next Address is: " + next_address);
		// return next_address;
	}
	/**
     * Shift array bytes to the right to fill in removed data
     * @param arr The array of data
     * @param read_pointer Further most left address of bytes to start reading
     * @param write_pointer Further most righ address of bytes to start filling in
     * @param count How many to do. Must be >= read_pointer
     */
    public static void shift_array_bytes_right(byte[] arr, int read_pointer, int write_pointer, int count) throws IOException{
        if(count > read_pointer)
           throw new IOException("Count must be greater than the read pointer");
        for(int i = 0; i < count; i++) {
            arr[write_pointer] = arr[read_pointer];
            write_pointer--;
            read_pointer--;
        }
    }
	public int get_obj_byte_length(Object o) {
		int len;
        try {
            // This is mad ugly, but we want to read the previous object to get its length
            // Then subtract the start of the next object. This is how much we can compact
            // p("Previous item is " + (String)get(current_index-1));
            len = ((toByteArray(o).length));
            
        } catch (IOException e) {
			// TODO Auto-generated catch block
			len = -1;
		}
		return len;
	}
	private ArrayList<Integer> removed_indexes = new ArrayList<>();
	/**
	 * Reorganizes this {@code SlottedPage} to maximize its free space.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IndexOutOfBoundsException
	 */
	protected void compact() throws IOException {
		// Okay, so lets say we have the following: 
		// 5 | 979 | 983 | 990 | 992 | 996 | ...... | Some | String | Is | Here | Yay!
		// Then we remove String (1)
		// 5 | 979 | -1 | 990 | 992 | 996 | ...... | Some | String | Is | Here | Yay!
		// But now, we need to add something super long.. one more than the free space which is
		// (979 - (sizeof(int) * 6) aka ((headerAddr(addrCount())) - (sizeof(int) * addrCount()+1)

		// We want to move everything to the right. 
		// Ie, move everything to the left of the -1 to the right, until you hit the address of the next entry
		
		// BUT he said not to change the item count.. which is strange
		// because we want to still index by the same keys. So here's what we'll do
		// We need to move everything to the right (that's fine), we need to keep 
		// the same amount of stuff, but what we DO need to do is update the addresses
		// In each of the indexes! 

		// First, we'll go through and look for each null entry 
		// And we'll compute the total size we'll save (subtract the previous and the next) - CHECK FOR EDGE CASES

		
		
		for(int current_index = 0; current_index < entryCount(); current_index++) {
			// 5 | 979 | -1 | 990 | 992 | 996 | ...... | Some | String | Is | Here | Yay!
			// When null_index = 1
			// Okay, so the 1, aka String is the first thing we need to remove (shift right) and update Some's address

			// We can get the size of the item this way (note that if the previous/next ones are -1 too, we need to add them to the mess)
			if(getLocation(current_index) == -1 && !removed_indexes.contains(current_index))
			{
				p("Found deleted entry to compact at index " + current_index);
				// Okay. At this -1, we want to shift everything from the left to the byte before the next one

				// int next_object_start = get_next_valid_entry_addr(current_index, true);
				// // Now we want to find out how large the removed object was
				// int removed_obj_size;
				// try {
				// 	// This is mad ugly, but we want to read the previous object to get its length
				// 	// Then subtract the start of the next object. This is how much we can compact
				// 	// p("Previous item is " + (String)get(current_index-1));
				// 	// removed_obj_size = ((next_object_start - (getLocation(current_index-1) + toByteArray(get(current_index-2)).length)));
				// 	int next_addr_start = getLocation(current_index-1);
				// 	p("NEXT ADDR:::: " + next_addr_start);
				// 	int prev_addr = getLocation(current_index+1);
				// 	p("PREV ADDR:::::" + prev_addr);
				// 	int prev_obj_size = toByteArray(get(current_index+1)).length;
				// 	p("PREV OBJ SIZE::::" + prev_obj_size);
				// 	int prev_obj_end = getLocation(current_index+1) - prev_obj_size;
				// 	p("PREV OBJ END:::::" + prev_obj_end);
				// 	removed_obj_size = next_addr_start + prev_obj_end;
				// } catch(Exception e) { // If soemething happens, do this for now
				// 	removed_obj_size = 0;
				// }
				// p("THe size of the removed object is: " + removed_obj_size + " bytes");
				 
				
				

				// This is the address we want to start copying bytes TO. This will be 1 before the start of the next object
				// int write_pointer = next_object_start;
				// p("The address to start writing at is: " + write_pointer);
				// We want to stop writing when we get to the start of the free space which signifies the end of the data start
				//int stop_writing = headerSize() + freeSpaceSize(); // This takes us straight to the beginning of OBJECTS
				//int stop_writing = startOfDataStorage();
				//int stop_writing = startOfDataStorage() - removed_obj_size;
				//p("The address to stop writing at is: " + stop_writing);
				// While the start addr is less than the stop addr
				// int read_pointer = write_pointer - removed_obj_size;

				// Want to shift starting from where the beginning of the next one is

				//shift_array_bytes_right(data, (next_object_start-1) - removed_obj_size, startOfDataStorage() - removed_obj_size - 3);
				// p("Start of data storage: " + startOfDataStorage());
				// byte[] teststr = new byte[removed_obj_size];
				// System.arraycopy(data, write_pointer, teststr, 0, removed_obj_size);
				// Object oo;
				// try {
				// int removed_address = next_object_start - removed_obj_size;
				//  oo = new ObjectInputStream(new ByteArrayInputStream(teststr, 0, teststr.length-1)).readObject();
				// } catch (Exception e) {
				// 	// TODO: handle exception
				// 	oo = null;
				// }
				// p("This is what we're saying is the object:: " + oo);
				// while(read_pointer != startOfDataStorage()) {
				// 	// Copy the byte at the start index - the removed obj size to the start index
				// 	data[write_pointer] = data[read_pointer];
				// 	//p("Writing " + data[read_pointer] + " at " + read_pointer + " to " + data[write_pointer] + " at " + write_pointer);
				// 	data[read_pointer] = Byte.parseByte("-99");
				// 	data[write_pointer] = Byte.parseByte("-69");
				// 	write_pointer--;
				// 	read_pointer--;
				// }

				
				// First thing lets do is calculate the read pointer. This is going to be the end of the previous object's bytes
				// Note that the previous items' data is in the NEXT index
				// Edge case: When there is no next entry (ie is last item, ie )
				/*
				int previous_data_addr = get_next_valid_entry_addr(current_index);
				// Now that we have its start, we want to see how large it used to be
				int sizeof_previous_data = 0;
				try {
					sizeof_previous_data = get_obj_byte_length(get(get_next_valid_entry_addr(current_index)));
				} catch (Exception e) {
					// TODO: handle exception
				}
				int previous_data_end_addr = previous_data_addr + sizeof_previous_data;
				int read_pointer = previous_data_end_addr -1; // -1 so we start reading the end of that block
				p("Read pointer at previous data end addr: " + read_pointer);
				*/

				// Okay, so for the read pointer.
				// If it's at the end of data (start of indexes), up until the first one, the read address will be the 
				// last address of the last data object (next index)
				// If it's the first one, (last index) just change the startofdata poitner to the next data object (previous index)
				int prev_data_obj_start_addr = get_next_valid_entry_addr(current_index);
				int prev_data_obj_size = 0;
				try {
					prev_data_obj_size = get_obj_byte_length(
						get( // get the object at the prev data index
							get_next_valid_entry_addr(current_index)
							) 
						); // and get its length
				} catch (Exception e) {e.printStackTrace();}
				p("Calculated previous object length " + prev_data_obj_size);
				int prev_data_obj_end_addr = prev_data_obj_start_addr + prev_data_obj_size;
				int read_pointer = prev_data_obj_end_addr;
				p("Read pointer set to: " + read_pointer);

				// Now we want to calculate the write pointer. This will be the address after the empty space which will be
				// the address of the entry BEFORE it.
				int next_addr_start = get_previous_valid_entry_addr(current_index);
				int write_pointer = next_addr_start - 1; // Don't want to overwrite the first one
				p("Write pointer at next_addr_start: " + write_pointer);
				p("pointer diff: " + (write_pointer - read_pointer));
				// Now finally, we need to know how far to go up. We want this to be the distance from the startofdata
				// to the read_pointer
				int length = read_pointer - startOfDataStorage();
				p("Length to copy:: " + length);

			

				// Let's let it rip and see what happens
				shift_array_bytes_right(data, read_pointer, write_pointer, length+1);
				
				int removed_obj_size = write_pointer - read_pointer;
				// Update all the header entries. Since we are changing bytes towards the begnning of the data segment,
				// those are going to be the HIGHER indexes, so update everything after us.
				for(int entry = current_index+1; entry < this.entryCount(); entry++) {
					if(getLocation(entry) != -1) // Skip any previously deleted entries
						// Update the location to: what it currently has, + the removed object size. 
						saveLocation(entry, getLocation(entry) + removed_obj_size);
				}
// FIXME
				// Update the free storate start loc to where it was + the size of the rmoved obj
				setStartOfDataStorage(startOfDataStorage() + removed_obj_size);
				// add the index to our removed_indexes so we don't mess with that one again. 
				removed_indexes.add(current_index);

				/*
				int start_loc = getLocation(null_index-1);  // Check if the first is deleted
				int end_loc = getLocation(null_index+1); // check if the next one is too
				int obj_size = end_loc - start_loc;
	
				// Now that we know how large it is, we want to move all the data before it to the right that much,
				// and then update the indexes by the obj_size too
	
	
				byte buffer;
				for(int start = start_loc; start < end_loc; start++) {
					// Shift all the data before us, into this. 
					// So we're moving Some into where ring from String is
					// Let's get the bytes
					buffer = data[start];
					data[]
	
				}
				*/
			}
		}
		// TODO complete this method (5 points)
		//throw new UnsupportedOperationException();
	}

	/**
	 * Saves the specified object in the free space of this {@code SlottedPage}.
	 * 
	 * @param o
	 *            an object
	 * @return the location at which the object is saved within this {@code SlottedPage}
	 * @throws OverflowException
	 *             if this {@code SlottedPage} cannot accommodate the specified object
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected int save(Object o) throws OverflowException, IOException {
		return save(toByteArray(o));
	}

	/**
	 * Saves the specified byte array in the free space of this {@code SlottedPage}.
	 * 
	 * @param b
	 *            a byte array
	 * @return the location at which the object is saved within this {@code SlottedPage}
	 * @throws OverflowException
	 *             if this {@code SlottedPage} cannot accommodate the specified byte array
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected int save(byte[] b) throws OverflowException, IOException{
		if (freeSpaceSize() < b.length + Integer.BYTES) {
			compact();
			if (freeSpaceSize() < b.length + Integer.BYTES)
				throw new OverflowException();
		}
		int location = startOfDataStorage() - b.length;
		System.arraycopy(b, 0, data, location, b.length);
		setStartOfDataStorage(location);
		return location;
	}

	/**
	 * Sets the number of entries in this {@code SlottedPage}.
	 * 
	 * @param count
	 *            the number of entries in this {@code SlottedPage}
	 */
	protected void setEntryCount(int count) {
		writeInt(0, count);
	}

	/**
	 * Returns the start location of the specified object within this {@code SlottedPage}.
	 * 
	 * @param index
	 *            an index that specifies an object
	 * @return the start location of the specified object within this {@code SlottedPage}
	 */
	protected int getLocation(int index) {
		return readInt((index + 1) * Integer.BYTES);
	}

	/**
	 * Saves the start location of an object within the header of this {@code SlottedPage}.
	 * 
	 * @param index
	 *            the index of the object
	 * @param location
	 *            the start location of an object within this {@code SlottedPage}
	 */
	protected void saveLocation(int index, int location) {
		writeInt((index + 1) * Integer.BYTES, location);
	}

	/**
	 * Returns the size of free space in this {@code SlottedPage}.
	 * 
	 * @return the size of free space in this {@code SlottedPage}
	 */
	public int freeSpaceSize() {
		return startOfDataStorage() - headerSize();
	}

	/**
	 * Returns the size of the header in this {@code SlottedPage}.
	 * 
	 * @return the size of the header in this {@code SlottedPage}
	 */
	protected int headerSize() {
		return Integer.BYTES * (entryCount() + 1);
	}

	/**
	 * Sets the start location of data storage.
	 * 
	 * @param startOfDataStorage
	 *            the start location of data storage
	 */
	protected void setStartOfDataStorage(int startOfDataStorage) {
		writeInt(data.length - Integer.BYTES, startOfDataStorage);
	}

	/**
	 * Returns the start location of data storage in this {@code SlottedPage}.
	 * 
	 * @return the start location of data storage in this {@code SlottedPage}
	 */
	protected int startOfDataStorage() {
		return readInt(data.length - Integer.BYTES);
	}

	/**
	 * Writes an integer value at the specified location in the byte array of this {@code SlottedPage}.
	 * 
	 * @param location
	 *            a location in the byte array of this {@code SlottedPage}
	 * @param value
	 *            the value to write
	 */
	protected void writeInt(int location, int value) {
		data[location] = (byte) (value >>> 24);
		data[location + 1] = (byte) (value >>> 16);
		data[location + 2] = (byte) (value >>> 8);
		data[location + 3] = (byte) value;
	}

	/**
	 * Reads an integer at the specified location in the byte array of this {@code SlottedPage}.
	 * 
	 * @param location
	 *            a location in the byte array of this {@code SlottedPage}
	 * @return an integer read at the specified location in the byte array of this {@code SlottedPage}
	 */
	protected int readInt(int location) {
		return ((data[location]) << 24) + ((data[location + 1] & 0xFF) << 16) + ((data[location + 2] & 0xFF) << 8)
				+ (data[location + 3] & 0xFF);
	}

	/**
	 * Returns a byte array representing the specified object.
	 * 
	 * @param o
	 *            an object.
	 * @return a byte array representing the specified object
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected static byte[] toByteArray(Object o) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(b);
		out.writeObject(o);
		out.flush();
		return b.toByteArray();
	}

	/**
	 * Returns an object created from the specified byte array.
	 * 
	 * @param b
	 *            a byte array
	 * @param offset
	 *            the offset in the byte array of the first byte to read
	 * @return an object created from the specified byte array
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected Object toObject(byte[] b, int offset) throws IOException {
		try {
			if (b == null)
				return null;
			return new ObjectInputStream(new ByteArrayInputStream(b, offset, b.length - offset)).readObject();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * A {@code OverflowException} is thrown if a {@code SlottedPage} cannot accommodate an additional object.
	 * 
	 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
	 */
	public class OverflowException extends Exception {

		/**
		 * Automatically generated serial version UID.
		 */
		private static final long serialVersionUID = -3007432568764672956L;

	}

	/**
	 * An {@code IndexOutofBoundsException} is thrown if an invalid index is used.
	 * 
	 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
	 */
	public class IndexOutOfBoundsException extends Exception {

		/**
		 * Automatically generated serial version UID.
		 */
		private static final long serialVersionUID = 7167791498344223410L;

	}

}
