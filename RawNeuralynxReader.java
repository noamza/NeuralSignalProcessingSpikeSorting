/*
 *Based on specs found at http://neuralynx.com/software/NeuralynxDataFileFormats.pdf 
 */
package rawneuralynxreader;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
//import java.util.HashMap;
import java.util.*;
import java.text.*;

/**
 *
 * @author Noam Almog
 */
public class RawNeuralynxReader {
    static String homeDir = "C:\\Noam\\Data\\doriPlot\\Ohad\\160515\\";         // 160515 160519
    static String[] inputDirs = { //list of folders
                                 "1"+"\\" //, "2"+"\\", "3"+"\\","4"+"\\"
                                };
    static String inputFile = "DigitalLynxSXRawDataFile_01.nrd";
    static boolean segment = true; //read file all at once or in chunks
    static int numChannels = 16;
    static boolean save = true;
    
    
    public static void main(String[] args) {
        for(String inputDir: inputDirs) {
            
            String outputDir = homeDir + inputDir;
            System.out.println("Hello " + inputDir);
            InputStream inStream = null;
            BufferedInputStream bis = null;
            String fin = homeDir + inputDir + inputFile;
            FileWriter fwo = null;
            String cols[] = {"STX", "ID", "Size", "TimestampHigh", "TimestampLow", "Status",
                    "ParallelPort", "Extras", "Data", "CRC"};
            int fakeTime = 1;
            BufferedWriter[] outa = new BufferedWriter[numChannels];
            int maxLinesPerFile = 1000000; //Size of CSV chunk
            int chunk = 1;

            if(segment) outputDir += "chunks" + "\\"; //doing raw file in chunks or all
            
            //tracking maxs and mins of channels
            int[] minCh = new int[numChannels], maxCh = new int[numChannels];
            for(int i = 0; i < numChannels; i++){
                minCh[i] = Integer.MAX_VALUE;
                maxCh[i] = Integer.MIN_VALUE;
            }

            try {
                inStream = new FileInputStream(fin);
                bis = new BufferedInputStream(inStream);
                int stop = 0;
                int lines = 0;

                // read until a single byte is available
                //NOTE all data is little endian and needs to be reversed 
                while (bis.available() > 0 && stop < 10000 ) {
                    //stop++; //STOP
                    byte[] arr = new byte[4];
                    bis.read(arr, 0, 4);
                    ByteBuffer bb = ByteBuffer.wrap(arr);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    int temp = bb.getInt();
                    // print the characters
                    //System.out.println("Int32: "+ temp);
                    //Start of packet
                    //*
                    if (temp == 2048) {
                        HashMap<String, Object> packet = new HashMap<>();
                        packet.put("STX", temp);//;new Integer(temp));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("ID", bb.getInt()); //new Integer()
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        Integer dataSize = bb.getInt(); //n 
                        packet.put("Size", dataSize);                        
                        //this code takes in two uint32 integers that are meant to combined
                        //into a uint64 timestamp which is in microseconds
                        //the first uint32 is the high bits and the second the low bits
                        //however these need to be reversed before they can be combined (endian issue)
                        byte[] a1 = new byte[4];
                        bis.read(a1, 0, 4);
                        byte[] a2 = new byte[4];
                        bis.read(a2, 0, 4);
                        byte[] at = new byte[8];
                        //reversing top half of long
                        at[0] = a1[3]; at[1] = a1[2]; at[2] = a1[1]; at[3] = a1[0];
                        //reverseing bottom half of long
                        at[4] = a2[3]; at[5] = a2[2]; at[6] = a2[1]; at[7] = a2[0];
                        bb = ByteBuffer.wrap(at);
                        Long timestamp = bb.getLong();
                        packet.put("Timestamp", timestamp);
                        //print("goal      11461423740");
                        //print("timeStamp " + timeStamp);
                        //print(timestamp);
                        
                        /* USEFUL TIMESTAMP DEBUGGING CODE
                        arr = new byte[4]; //also do for th
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        int tl = bb.getInt();
                        long x = th; 
                        x <<= 32;
                        long y = tl & 0x00000000ffffffffL;
                        //or maybe Integer.toUnsignedLong(tl);
                        long timestamp = x + y;
                        print("bit " + String.format("%8s", Long.toBinaryString(x & 0xFFFFFFFF)).replace(' ', '0'));
                        Date time = new Date(timeStamp / 1000000); //In Microseconds
                        DateFormat formatter = new SimpleDateFormat("YYYY:MM:DD:HH:mm:ss:SSS");
                        String dateFormatted = formatter.format(time);
                        print(dateFormatted); 
                        */

                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("Status", bb.getInt());
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("ParallelPort", bb.getInt());

                        Integer extras[] = new Integer[10];
                        for (int i = 0; i < 10; i++) {
                            arr = new byte[4];
                            bis.read(arr, 0, 4);
                            bb = ByteBuffer.wrap(arr);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            extras[i] = bb.getInt();
                        }
                        packet.put("Extras", extras);

                        //ASSUMES DATA HAS 64 length!!
                        Integer data[] = new Integer[dataSize - 10]; //
                        for (int i = 0; i < dataSize - 10; i++) {
                            arr = new byte[4];
                            bis.read(arr, 0, 4);
                            bb = ByteBuffer.wrap(arr);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            data[i] = bb.getInt();
                        }
                        packet.put("Data", data);

                        //arr = new byte[4];bis.read(arr, 0, 4); bb = ByteBuffer.wrap(arr);bb.order(ByteOrder.LITTLE_ENDIAN);
                        //packet.put("Data", new Integer(bb.getInt()));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("CRC", bb.getInt());

                        for (String k : cols) {
                            //print(k+": "+packet.get(k));
                        }
                        for (int k : (Integer[]) packet.get("Extras")) {
                            //print("extra: "+k);
                        }
                        for (int k : (Integer[]) packet.get("Data")) {
                            //print(i++ + " data: "+k);
                        }

                        if (lines == maxLinesPerFile && segment) {
                            //out.close();
                            for (int i = 0; i < numChannels; i++) {
                                outa[i].close();
                            }
                            chunk += 1;
                            lines = 0;
                        }

                        if (fakeTime % maxLinesPerFile == 0) {
                            print("processing: " + " chunk " + chunk + "! " + (new Date()).toString());
                        }

                        if (lines == 0) {
                            //fwo = new FileWriter(outputDir+name);
                            //channel files.
                            for (int i = 0; i < numChannels; i++) {
                                String filename = "";
                                if(segment){
                                    filename = outputDir + "ch" + (i + 1) + "_" + chunk + ".csv";
                                } else {
                                    filename = outputDir + "ch" + (i + 1) + ".csv";
                                }
                                outa[i] = new BufferedWriter(new FileWriter(filename));
                            }
                            //out = new BufferedWriter(fwo);
                            String header = "time";
                            for (int i = 0; i < dataSize - 10; i++) {
                                header += ",ch" + (i + 1);
                            }
                            header += "\n";
                            //print(header);
                            //write(out,header); //WRITE
                        }
                        
                        /*
                        String output = timestamp + ""; //USED TO BE WRONG VALUE
                        for (int k : (Integer[]) packet.get("Data")) {
                            output += "," + k;
                        }
                        output += "\n"; 
                        //print(output); //write(out, output); WRITE */
                         
                        //write out each channel seperately
                        String output;
                        if(save){
                            for (int i = 0; i < numChannels; i++) {
                                int chVal = ((Integer[]) packet.get("Data"))[i];
                                if(minCh[i]>chVal) minCh[i] = chVal;
                                if(maxCh[i]<chVal) maxCh[i] = chVal;
                                //output = "";
                                if (segment) {
                                    output = timestamp + "," + chVal + "\n";
                                } else {
                                    output = chVal + "\n";
                                }
                                //print(output);
                                write(outa[i], output);
                                
                            }
                        }
                         
                        
                        lines++;
                        fakeTime++;

                    } /**/
                 //*********************read kiio
                }
            } catch (Exception e) {
                // if any I/O error occurs
                e.printStackTrace();
            } finally {
                // releases any system resources associated with the streams
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (Exception e) {
                        print("error closing");
                    }
                }
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (Exception e) {
                        print("error closing");
                    }
                }
                if (fwo != null) {
                    try {
                        fwo.close();
                    } catch (Exception e) {
                        print("error closing");
                    }
                }
            /*
            if(out!=null) {
                try {
                    out.close();
                } catch (Exception e ) {
                    print("error closing");
                }
            }
            */
                for (int i = 0; i < numChannels; i++) {
                    if (outa[i] != null) {
                        try {
                            outa[i].close();
                        } catch (Exception e) {
                            print("error closing");
                        }
                    }
                }
            }
            print(inputDir+":");
            for(int i = 0; i < numChannels; i++){
                System.out.printf("ch%d min %d max %d\n", i, minCh[i], maxCh[i]);
                //System.out.printf("ch%d min %d max %d\n", i, i, i);
            }
            print("************");

        } // END INPUT DIRS
        print("done!");
    }

    public static void print(Object o) {
        System.out.println(o.toString());
    }

    public static void write(BufferedWriter out, String s){
        try{
            out.write(s);
            //out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

    }

}
/*

1\
processing: 1000000 chunk 1! Mon May 30 18:49:56 IDT 2016
processing: 2000000 chunk 2! Mon May 30 18:50:21 IDT 2016
processing: 3000000 chunk 3! Mon May 30 18:51:00 IDT 2016
processing: 4000000 chunk 4! Mon May 30 18:51:29 IDT 2016
processing: 5000000 chunk 5! Mon May 30 18:51:53 IDT 2016
processing: 6000000 chunk 6! Mon May 30 18:52:20 IDT 2016
processing: 7000000 chunk 7! Mon May 30 18:52:44 IDT 2016
processing: 8000000 chunk 8! Mon May 30 18:53:06 IDT 2016
processing: 9000000 chunk 9! Mon May 30 18:53:29 IDT 2016
processing: 10000000 chunk 10! Mon May 30 18:53:55 IDT 2016
processing: 11000000 chunk 11! Mon May 30 18:54:22 IDT 2016
processing: 12000000 chunk 12! Mon May 30 18:54:52 IDT 2016
processing: 13000000 chunk 13! Mon May 30 18:55:12 IDT 2016
processing: 14000000 chunk 14! Mon May 30 18:55:36 IDT 2016
processing: 15000000 chunk 15! Mon May 30 18:56:02 IDT 2016
processing: 16000000 chunk 16! Mon May 30 18:56:22 IDT 2016
processing: 17000000 chunk 17! Mon May 30 18:56:46 IDT 2016
processing: 18000000 chunk 18! Mon May 30 18:57:08 IDT 2016
processing: 19000000 chunk 19! Mon May 30 18:57:24 IDT 2016
processing: 20000000 chunk 20! Mon May 30 18:57:41 IDT 2016
processing: 21000000 chunk 21! Mon May 30 18:58:02 IDT 2016
processing: 22000000 chunk 22! Mon May 30 18:58:21 IDT 2016
processing: 23000000 chunk 23! Mon May 30 18:58:42 IDT 2016
processing: 24000000 chunk 24! Mon May 30 18:59:03 IDT 2016
processing: 25000000 chunk 25! Mon May 30 18:59:32 IDT 2016
processing: 26000000 chunk 26! Mon May 30 18:59:48 IDT 2016
processing: 27000000 chunk 27! Mon May 30 19:00:10 IDT 2016
processing: 28000000 chunk 28! Mon May 30 19:00:30 IDT 2016
1\:
ch0 min 1817694 max 2248301
ch1 min 1813464 max 2272858
ch2 min 1793322 max 2201764
ch3 min 1811031 max 2276906
ch4 min 1963772 max 2313268
ch5 min 1999340 max 2290264
ch6 min 1852381 max 2168460
ch7 min 1862387 max 2166669
ch8 min 1867412 max 2311753
ch9 min 1833966 max 2283821
ch10 min -8388608 max 5212898
ch11 min 1841096 max 2282118
ch12 min 1871522 max 2328152
ch13 min 1863795 max 2335535
ch14 min -1647817 max 4143673
ch15 min 1846720 max 2305478
************
Hello 2\
processing: 1000000 chunk 1! Mon May 30 19:00:50 IDT 2016
processing: 2000000 chunk 2! Mon May 30 19:01:14 IDT 2016
processing: 3000000 chunk 3! Mon May 30 19:01:41 IDT 2016
processing: 4000000 chunk 4! Mon May 30 19:02:14 IDT 2016
processing: 5000000 chunk 5! Mon May 30 19:02:33 IDT 2016
processing: 6000000 chunk 6! Mon May 30 19:02:56 IDT 2016
processing: 7000000 chunk 7! Mon May 30 19:03:17 IDT 2016
processing: 8000000 chunk 8! Mon May 30 19:03:35 IDT 2016
processing: 9000000 chunk 9! Mon May 30 19:03:52 IDT 2016
processing: 10000000 chunk 10! Mon May 30 19:04:12 IDT 2016
processing: 11000000 chunk 11! Mon May 30 19:04:45 IDT 2016
processing: 12000000 chunk 12! Mon May 30 19:05:04 IDT 2016
processing: 13000000 chunk 13! Mon May 30 19:05:27 IDT 2016
processing: 14000000 chunk 14! Mon May 30 19:05:48 IDT 2016
processing: 15000000 chunk 15! Mon May 30 19:06:10 IDT 2016
processing: 16000000 chunk 16! Mon May 30 19:06:33 IDT 2016
processing: 17000000 chunk 17! Mon May 30 19:06:58 IDT 2016
processing: 18000000 chunk 18! Mon May 30 19:07:24 IDT 2016
processing: 19000000 chunk 19! Mon May 30 19:07:48 IDT 2016
processing: 20000000 chunk 20! Mon May 30 19:08:06 IDT 2016
processing: 21000000 chunk 21! Mon May 30 19:08:33 IDT 2016
processing: 22000000 chunk 22! Mon May 30 19:08:56 IDT 2016
processing: 23000000 chunk 23! Mon May 30 19:09:24 IDT 2016
processing: 24000000 chunk 24! Mon May 30 19:09:52 IDT 2016
processing: 25000000 chunk 25! Mon May 30 19:10:11 IDT 2016
processing: 26000000 chunk 26! Mon May 30 19:10:30 IDT 2016
processing: 27000000 chunk 27! Mon May 30 19:10:53 IDT 2016
processing: 28000000 chunk 28! Mon May 30 19:11:32 IDT 2016
processing: 29000000 chunk 29! Mon May 30 19:11:52 IDT 2016
2\:
ch0 min 2045778 max 2492974
ch1 min 2033148 max 2495240
ch2 min 2007335 max 2438928
ch3 min 2043981 max 2513857
ch4 min 2107911 max 2462635
ch5 min 2155226 max 2487058
ch6 min 1994109 max 2313017
ch7 min 1998142 max 2330478
ch8 min 2061803 max 2532550
ch9 min 2053554 max 2525257
ch10 min -8388608 max 8388607
ch11 min 2044142 max 2501040
ch12 min 2066186 max 2538002
ch13 min 2069211 max 2545972
ch14 min -1254707 max 5381513
ch15 min 1978760 max 2484458
************
Hello 3\
processing: 1000000 chunk 1! Mon May 30 19:12:24 IDT 2016
processing: 2000000 chunk 2! Mon May 30 19:12:53 IDT 2016
processing: 3000000 chunk 3! Mon May 30 19:13:12 IDT 2016
processing: 4000000 chunk 4! Mon May 30 19:13:34 IDT 2016
processing: 5000000 chunk 5! Mon May 30 19:13:57 IDT 2016
processing: 6000000 chunk 6! Mon May 30 19:14:27 IDT 2016
processing: 7000000 chunk 7! Mon May 30 19:14:50 IDT 2016
processing: 8000000 chunk 8! Mon May 30 19:15:05 IDT 2016
processing: 9000000 chunk 9! Mon May 30 19:15:25 IDT 2016
processing: 10000000 chunk 10! Mon May 30 19:15:47 IDT 2016
processing: 11000000 chunk 11! Mon May 30 19:16:06 IDT 2016
processing: 12000000 chunk 12! Mon May 30 19:16:25 IDT 2016
processing: 13000000 chunk 13! Mon May 30 19:16:42 IDT 2016
processing: 14000000 chunk 14! Mon May 30 19:17:03 IDT 2016
processing: 15000000 chunk 15! Mon May 30 19:17:23 IDT 2016
processing: 16000000 chunk 16! Mon May 30 19:17:48 IDT 2016
processing: 17000000 chunk 17! Mon May 30 19:18:09 IDT 2016
processing: 18000000 chunk 18! Mon May 30 19:18:26 IDT 2016
processing: 19000000 chunk 19! Mon May 30 19:18:45 IDT 2016
processing: 20000000 chunk 20! Mon May 30 19:19:07 IDT 2016
processing: 21000000 chunk 21! Mon May 30 19:19:25 IDT 2016
processing: 22000000 chunk 22! Mon May 30 19:19:41 IDT 2016
processing: 23000000 chunk 23! Mon May 30 19:20:03 IDT 2016
processing: 24000000 chunk 24! Mon May 30 19:20:20 IDT 2016
processing: 25000000 chunk 25! Mon May 30 19:20:42 IDT 2016
processing: 26000000 chunk 26! Mon May 30 19:21:02 IDT 2016
processing: 27000000 chunk 27! Mon May 30 19:21:23 IDT 2016
processing: 28000000 chunk 28! Mon May 30 19:21:47 IDT 2016
3\:
ch0 min 1834452 max 2290285
ch1 min 1828816 max 2278153
ch2 min 1810583 max 2338925
ch3 min 1833275 max 2349718
ch4 min 1961067 max 2282365
ch5 min 2006112 max 2290823
ch6 min 1839456 max 2156854
ch7 min 1860673 max 2220375
ch8 min 1880131 max 2370608
ch9 min 1850509 max 2346854
ch10 min -6204637 max 8388607
ch11 min 1858783 max 2302027
ch12 min 1881221 max 2327126
ch13 min 1878774 max 2465538
ch14 min -774033 max 5650384
ch15 min 1857054 max 2308935
************
Hello 4\
processing: 1000000 chunk 1! Mon May 30 19:22:27 IDT 2016
processing: 2000000 chunk 2! Mon May 30 19:22:46 IDT 2016
processing: 3000000 chunk 3! Mon May 30 19:23:03 IDT 2016
processing: 4000000 chunk 4! Mon May 30 19:23:25 IDT 2016
processing: 5000000 chunk 5! Mon May 30 19:23:45 IDT 2016
processing: 6000000 chunk 6! Mon May 30 19:24:02 IDT 2016
processing: 7000000 chunk 7! Mon May 30 19:24:22 IDT 2016
processing: 8000000 chunk 8! Mon May 30 19:24:41 IDT 2016
processing: 9000000 chunk 9! Mon May 30 19:25:03 IDT 2016
processing: 10000000 chunk 10! Mon May 30 19:25:21 IDT 2016
processing: 11000000 chunk 11! Mon May 30 19:25:38 IDT 2016
processing: 12000000 chunk 12! Mon May 30 19:26:09 IDT 2016
processing: 13000000 chunk 13! Mon May 30 19:26:25 IDT 2016
processing: 14000000 chunk 14! Mon May 30 19:26:47 IDT 2016
processing: 15000000 chunk 15! Mon May 30 19:27:11 IDT 2016
processing: 16000000 chunk 16! Mon May 30 19:27:30 IDT 2016
processing: 17000000 chunk 17! Mon May 30 19:27:48 IDT 2016
processing: 18000000 chunk 18! Mon May 30 19:28:12 IDT 2016
processing: 19000000 chunk 19! Mon May 30 19:28:35 IDT 2016
processing: 20000000 chunk 20! Mon May 30 19:28:56 IDT 2016
processing: 21000000 chunk 21! Mon May 30 19:29:17 IDT 2016
processing: 22000000 chunk 22! Mon May 30 19:29:38 IDT 2016
processing: 23000000 chunk 23! Mon May 30 19:29:59 IDT 2016
processing: 24000000 chunk 24! Mon May 30 19:30:15 IDT 2016
processing: 25000000 chunk 25! Mon May 30 19:30:31 IDT 2016
processing: 26000000 chunk 26! Mon May 30 19:30:51 IDT 2016
processing: 27000000 chunk 27! Mon May 30 19:31:13 IDT 2016
processing: 28000000 chunk 28! Mon May 30 19:31:28 IDT 2016
4\:
ch0 min 1819713 max 2295572
ch1 min 1816398 max 2342690
ch2 min 1794792 max 2194295
ch3 min 1817531 max 2267095
ch4 min 1957979 max 2254602
ch5 min 1975741 max 2249046
ch6 min 1846868 max 2081196
ch7 min 1843988 max 2085949
ch8 min 1856626 max 2322492
ch9 min 1830792 max 2300156
ch10 min -4259083 max 4094355
ch11 min 1842838 max 2294658
ch12 min 1867039 max 2322005
ch13 min 1864397 max 2328661
ch14 min -10453 max 3882448
ch15 min 1819406 max 2275314
************
done!
BUILD SUCCESSFUL (total time: 42 minutes 10 seconds)


run:
Hello 1\
processing:  chunk 1! Mon May 30 19:33:44 IDT 2016
processing:  chunk 39! Mon May 30 19:46:47 IDT 2016
1\:
ch0 min 1828971 max 2420874
ch1 min 1827966 max 2416430
ch2 min 1803971 max 2488447
ch3 min 1836142 max 2498731
ch4 min 1729511 max 2490785
ch5 min 1938644 max 2449037
ch6 min 1709992 max 2472229
ch7 min 1675063 max 2485672
ch8 min 1863212 max 2515264
ch9 min 1850557 max 2547961
ch10 min -8388608 max 8388607
ch11 min 1881209 max 2468128
ch12 min 1888124 max 2593444
ch13 min 1889961 max 2576308
ch14 min -7757093 max 8388607
ch15 min 1880587 max 2581342

 */

