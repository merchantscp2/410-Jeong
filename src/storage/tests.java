package storage;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.cert.LDAPCertStoreParameters;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import java.util.Iterator;


public class tests {
    public static void main(String[] args) throws Exception {
        dtest_compact();
        //utest_compact();
        //test_shift();

        
    }
    public static void test_shift() {
        byte[] b_arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25};
        p(Arrays.toString(b_arr));
        shift_array_bytes_right(b_arr, 5, 10, 5);   
        p(Arrays.toString(b_arr));
    }

    /**
     * Shift array bytes to the right to fill in removed data
     * @param arr The array of data
     * @param read_pointer Further most left address of bytes to start reading
     * @param write_pointer Further most righ address of bytes to start filling in
     * @param count How many to do. Must be >= read_pointer
     */
    public static void shift_array_bytes_right(byte[] arr, int read_pointer, int write_pointer, int count) {
        if(count > read_pointer)
            p("WILL NOT WORK! THIS IS A BROKEN CALL");
        for(int i = 0; i < count; i++) {
            arr[write_pointer] = arr[read_pointer];
            write_pointer--;
            read_pointer--;
        }
    }

    public static void dtest_compact() throws Exception{
        SlottedPage sp = new SlottedPage(0, 500);
        sp.add("UAlbany");
        sp.add("is");
        sp.add("not");
        sp.add("that");
        sp.add("great");
        sp.add("of a");
        sp.add("school");
        //stats(sp);
        
        dump(sp);
        // FIXMEEEEE
		// int removed_obj_size;
        // try {
        //     // This is mad ugly, but we want to read the previous object to get its length
        //     // Then subtract the start of the next object. This is how much we can compact
        //     // p("Previous item is " + (String)get(current_index-1));
        //     removed_obj_size = ((toByteArray(sp.remove(0)).length));
            
        // } catch(Exception e) { // If soemething happens, do this for now
        //     removed_obj_size = 0;
        // }
        // p("SIZE OF REMOVED::: " + removed_obj_size);
        //p("Testing getting 2-1: " + sp.get(2-1));
        p("Removing first...");
        
        p("Compacting...");
        p("SIZEOF REMOVED: " + toByteArray(sp.remove(1)).length);
        sp.compact();
        p((String)sp.get(0));
        p((String)sp.get(1));
        p((String)sp.get(2));
        // for(Object o : sp) {
        //     p((String)o);
        // }
        // Okay, so now we're pretty filled. Let's do some stats
    }
    protected static byte[] toByteArray(Object o) throws Exception {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(b);
		out.writeObject(o);
		out.flush();
		return b.toByteArray();
	}

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static void dump(SlottedPage sp) {
        
        StringBuilder sb = new StringBuilder();
        int addr = 0;
        sb.append(String.format(ANSI_RED + "|%3d|", 4 * addr));
        for(int i = 0; i < sp.data.length; i++) {
            if(i == ((sp.entryCount()+1) * Integer.BYTES))
                sb.append(ANSI_RESET);
            sb.append(String.format("%4d,", sp.data[i]));
            if(((i+1) % 16) == 0 && i > 0)
                sb.append("|\n");
            if(((i+1) % 4) == 0 && i > 0)
                sb.append(String.format("|%3d|", 4 * ++addr));
        }
        p(sb.toString());
    }

    public static void stats(SlottedPage sp) {
        p("Stats::: ");
        p("Page Size: " + sp.data.length + " bytes");
        p("Entry Count: " + sp.entryCount());
        p("Free Space: " + sp.freeSpaceSize());
        p("Header Size: " + sp.headerSize());
        p("Start of DS: " + sp.startOfDataStorage());
        p("Data: "); dump(sp);
    }
    
    public static void p(String s) {
        System.out.println(s);
    }

    public static void utest_compact() throws Exception{
        
        int slottedPageSize = 2048;
        SlottedPage p = new SlottedPage(0, slottedPageSize*2);

        try {
			for (int i = 0; i < Integer.MAX_VALUE; i++)
				p.add(i);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		ArrayList<Object> l = list(p.iterator());
		int index = 3;
		Object o = l.set(index, null);
		try {
			p.remove(index);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		try {
			l.add(o);
			p.add(o);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Size of list: " + l.size() + " --- Size of Page: " + p.entryCount());
		for (int i = 0; i < l.size(); i++)
			System.out.println("Values at ["+i+"]: List: " + l.get(i) + " --- Page: " + p.get(i));
    }

    public static ArrayList<Object> list(Iterator<Object> i) {
		ArrayList<Object> l = new ArrayList<Object>();
		while (i.hasNext())
			l.add(i.next());
		return l;
	}
}
