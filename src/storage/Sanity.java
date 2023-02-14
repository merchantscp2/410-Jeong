package storage;

public class Sanity {
    private static boolean debug = true;
    private static void p(String s) { if(debug) System.out.println(s);}

	
    public static void doCompact(SlottedPage sp, int current_index) throws Exception {
            p("Found deleted entry to compact at index " + current_index);

            /* ==== Check General Edge Cases ==== */
            // If it's the last index, its't the first item
            if(is_first_data_entry(sp, current_index)) {
                // All we need to do is move the datastartpointer to the start of the the next data item (previous index)
                if(debug) fill(sp.data, sp.startOfDataStorage(), get_previous_valid_entry_addr(sp, current_index)-1);
                sp.setStartOfDataStorage(
                    get_previous_valid_entry_addr(sp, current_index)
                );
                sp.removed_indexes.add(current_index);
                p("Found that we're removing the first data entry (last index). Setting the new start od DS to " + get_previous_valid_entry_addr(sp, current_index));
                return;
            }
            // If it's the first item, it's the last index, so we have data to go off of.
            // Check if there are no more items :D (ie deleted last one)
            
            /* ==== Get the data read pointer (last byte of the previous data item [next index])==== */
            /* == Check Read Pointer Edge Cases == */
            /*
             * There really shouldn't be any since the only case where the previous one would be an issue is the general one we
             * covered earlier.
             */
            p("\n\n ==== Read Pointer ====");
            int prev_data_object_index = get_next_valid_entry_index(sp, current_index);
            int prev_data_obj_start_addr = sp.getLocation(prev_data_object_index);
            int prev_data_obj_size = 0;
            try {
                prev_data_obj_size = get_obj_byte_length_at_addr(sp,
                        prev_data_object_index
                    ); // and get its length
            } catch (Exception e) {e.printStackTrace();}
            p("\tCalculated previous object index " + prev_data_object_index);
            p("\tCalculated previous object length " + prev_data_obj_size);
            p("\tCalculated previous object start address " + prev_data_obj_start_addr);

            int prev_data_obj_end_addr = prev_data_obj_start_addr + prev_data_obj_size;
            int read_pointer = prev_data_obj_end_addr - 1;
            p("\tCalculated read pointer set to " + read_pointer);

            /* === Get data write pointer == */
            /*
             * Check for any edge cases. The write pointer is the bit right before the first byte of the next data obj (prev index)
             * If it's the last object, we want to start writing at the back of the array
             */
            p("\n\n ==== Write Pointer ====");
            int write_pointer = -1;
            if(current_index == 0) { // Set it to the end of the array (minus the ds field)
                write_pointer = (sp.data.length - Integer.BYTES)-1;
                p("\tFound end of array as write addr since first index");
            } else { // if it's not the end of the data
                int next_obj_index = get_previous_valid_entry_index(sp, current_index);
                int next_obj_address = sp.getLocation(next_obj_index);
                write_pointer = next_obj_address - 1; // Don't want to overwrite the first one

                p("\tNext object index " + next_obj_index);
                p("\tNext object adddress " + next_obj_address);
            }
            p("\tSet write pointer to " + write_pointer);
            p("\tpointer diff (Should be the size of the deleted): " + ((write_pointer - read_pointer)));
            // Now finally, we need to know how far to go up. We want this to be the distance from the startofdata
            // to the read_pointer
            int length = (read_pointer - sp.startOfDataStorage()) + 1;
            p("Length to copy:: " + length);

        

            // Let's let it rip and see what happens
            shift_array_bytes_right(sp.data, read_pointer, write_pointer, length);
            
            int removed_obj_size = (write_pointer - read_pointer);
            // Update all the header entries. Since we are changing bytes towards the begnning of the data segment,
            // those are going to be the HIGHER indexes, so update everything after us.
            for(int entry = current_index+1; entry < sp.entryCount(); entry++) {
                if(sp.getLocation(entry) != -1) // Skip any previously deleted entries
                    // Update the location to: what it currently has, + the removed object size. 
                    sp.saveLocation(entry, sp.getLocation(entry) + removed_obj_size);
            }
// FIXME
            p("Current startOfDatStorage Addr: " +sp.startOfDataStorage());
            p("Adding: " + removed_obj_size);
            p("New storage start:" + (sp.startOfDataStorage() + removed_obj_size));
            // Update the free storate start loc to where it was + the size of the rmoved obj
            sp.setStartOfDataStorage(sp.startOfDataStorage() + removed_obj_size);
            // add the index to our removed_indexes so we don't mess with that one again. 
            sp.removed_indexes.add(current_index);
    }
    protected static void fill(byte[] data, int start, int end) {
        while(start <= end) {
            data[start++] = -69;
        }
    }
    protected static boolean is_first_data_entry(SlottedPage sp, int current_index) {
        if(current_index == (sp.entryCount()-1)) return true;
        else {
            while((++current_index) >= sp.entryCount()-1) {
                if(sp.getLocation(current_index) != -1)
                    return true;
            }
        }
        return false;
    }
    protected static boolean is_last_data_entry(SlottedPage sp, int current_index) {
        if(current_index == 0) return true;
        else {
            while((--current_index) >= sp.entryCount()-1) {
                if(sp.getLocation(current_index) != -1)
                    return true;
            }
        }
        return false;
    }
    protected int readInt(byte[] data) {
    return ((data[0]) << 24) + ((data[1] & 0xFF) << 16) + ((data[2] & 0xFF) << 8)
            + (data[3] & 0xFF);
}
    protected static int get_next_valid_entry_addr(SlottedPage sp, int current_index) {
		// Initialize the next address var
		int next_address = -1;
		do { // If we reach the end of the indexes. We shouldn't need to check this because base case covers it
			// if(current_index == sp.entryCount()) {
			// 	return sp.startOfDataStorage(); 
			// }
			try {
				next_address = sp.getLocation(current_index++);
			} catch(Exception e) { break;}
		} while(next_address == -1);
		return next_address;
	}

    protected static int get_next_valid_entry_index(SlottedPage sp, int current_index) {
        // Initialize the next address var
        int next_index = -1;
        do { // If we reach the end of the indexes. We shouldn't need to check this because base case covers it
            // if(current_index == sp.entryCount()) {
            // 	return sp.startOfDataStorage(); 
            // }
            try {
                next_index = current_index++;
            } catch(Exception e) {}
        } while(sp.getLocation(next_index) == -1);
        return next_index;
    }

    protected static int get_previous_valid_entry_index(SlottedPage sp, int current_index) {
        // Initialize the next address var
        int prev_index = -1;
        do { // If we reach the end of the indexes. We shouldn't need to check this because base case covers it
            // if(current_index == sp.entryCount()) {
            // 	return sp.startOfDataStorage(); 
            // }
            try {
                prev_index = current_index--;
            } catch(Exception e) {}
        } while(sp.getLocation(prev_index) == -1);
        return prev_index;
    }

	protected static int get_previous_valid_entry_addr(SlottedPage sp, int current_index) {
		int previous_address = -1;
		do { // If we reach the end of the indexes
			if(current_index == 0) {
				return sp.data.length - Integer.BYTES; // The last item will
			}
			try {
				previous_address = sp.getLocation(current_index--);
			} catch(Exception e) {}
		} while(previous_address == -1);
		return previous_address;
	}
	/**
     * Shift array bytes to the right to fill in removed data
     * @param arr The array of data
     * @param read_pointer Further most left address of bytes to start reading
     * @param write_pointer Further most righ address of bytes to start filling in
     * @param count How many to do. Must be >= read_pointer
     */
    public static void shift_array_bytes_right(byte[] arr, int read_pointer, int write_pointer, int count) throws Exception{
        if(count > read_pointer)
           throw new Exception("Count must be greater than the read pointer");
        for(int i = 0; i < count; i++) {
            // Since we're lazy programmers, we don't care about what the free space actually contains.
            arr[write_pointer] = arr[read_pointer];
            arr[read_pointer] = -69; // So we can see what's been read/copied down
            write_pointer--;
            read_pointer--;
        }
    }
	public static int get_obj_byte_length_at_addr(SlottedPage sp, int index) {
		int len;
        try {
            Object o = sp.get(index);
            // This is mad ugly, but we want to read the previous object to get its length
            // Then subtract the start of the next object. This is how much we can compact
            // p("Previous item is " + (String)get(current_index-1));
            len = ((sp.toByteArray(o).length));
            
        } catch (Exception e) {
			// TODO Auto-generated catch block
			len = -1;
		}
		return len;
	}
}
